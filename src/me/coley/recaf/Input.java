package me.coley.recaf;

import java.io.*;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.ProtectionDomain;
import java.time.Instant;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.Asm;
import me.coley.recaf.bytecode.analysis.Hierarchy;
import me.coley.recaf.bytecode.analysis.Verify;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.event.*;
import me.coley.recaf.util.Streams;
import me.coley.recaf.util.Threads;

/**
 * 
 * @author Matt
 */
public class Input {
	private static final int HISTORY_LENGTH = 10;
	private static final String HIST_POSTFIX = ".hst";
	private static final String FILE_EXT = ".rcf";
	/**
	 * The file loaded from.
	 */
	public final File input;
	/**
	 * Instrumentation instance loaded from.
	 */
	private final Instrumentation instrumentation;
	/**
	 * Map of class names to ClassNode representations of the classes.
	 */
	public final Set<String> classes = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * Set of classes to be updated in the next save-state.
	 */
	private final Set<String> dirtyClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * Map of resource names to their raw bytes.
	 */
	public final Set<String> resources = new HashSet<>();;
	/**
	 * File system representation of contents of input.
	 */
	private final FileSystem system;
	/**
	 * Map of class names to ClassNodes.
	 */
	private final FileMap<String, ClassNode> proxyClasses;
	/**
	 * Map of resource names to their value.
	 */
	private final FileMap<String, byte[]> proxyResources;
	/**
	 * History manager of changes.
	 */
	private final Map<String, History> history = new ConcurrentHashMap<>();
	
	public Input(Instrumentation instrumentation) throws IOException {
		this.input = null;
		this.instrumentation = instrumentation;
		system = createSystem(instrumentation);
		Bus.subscribe(this);
		proxyClasses = createClassMap();
		proxyResources = createResourceMap();
		current = this;
	}

	public Input(File input) throws IOException {
		this.input = input;
		this.instrumentation = null;
		if (input.getName().endsWith(".class")) {
			system = createSystem(readClass());
		} else {
			system = createSystem(readArchive());
		}
		Bus.subscribe(this);
		proxyClasses = createClassMap();
		proxyResources = createResourceMap();
		current = this;
	}

	@Listener(priority = -1)
	private void onNewInput(NewInputEvent input) {
		// New input is loaded.
		// Don't want events still coming around here.
		if (input.get() != this) {
			// Run-later so event system doesn't
			// concurrent-modification-exception
			Threads.runFx(() -> {
				Bus.unsubscribe(this);
				Bus.unsubscribe(proxyClasses);
				Bus.unsubscribe(proxyResources);
			});
		}
	}

	/**
	 * Fires back a SaveStateEvent.
	 */
	@Listener(priority = -1)
	private void onSaveRequested(RequestSaveStateEvent event) {
		Bus.post(new SaveStateEvent(dirtyClasses));
		dirtyClasses.clear();
	}

	/**
	 * Marks a class as dirty <i>(To be saved in next save-state)</i>.
	 */
	@Listener(priority = -1)
	private void onClassMarkedDirty(ClassDirtyEvent event) {
		String name = event.getNode().name;
		dirtyClasses.add(name);
	}

	/**
	 * Marks a class as dirty <i>(To be saved in next save-state)</i>.
	 */
	@Listener(priority = -1)
	private void onClassRename(ClassRenameEvent event) {
		String nameOriginal = event.getOriginalName();
		String nameRenamed = event.getNewName();
		//
		Set<String> referenced = new HashSet<>();
		Map<String, ClassNode> updatedMap = new HashMap<>();
		referenced.add(nameRenamed);
		// replace references in all classes
		ExecutorService pool = Threads.pool(Threads.PoolKind.LOGIC);
		for (ClassNode cn : proxyClasses.values()) {
			pool.execute(() -> {
				ClassNode updated = new ClassNode();
				cn.accept(new ClassRemapper(updated, new Remapper() {
					@Override
					public String map(String internalName) {
						if (internalName.equals(nameOriginal)) {
							// mark classes that have referenced the renamed
							// class.
							referenced.add(cn.name);
							return nameRenamed;
						}
						return super.map(internalName);
					}
				}));
				if (referenced.contains(cn.name)) {
					updatedMap.put(cn.name, updated);
				}
			});
		}
		Threads.waitForCompletion(pool);
		pool = Threads.pool(Threads.PoolKind.LOGIC);
		// Update all classes with references to the renamed class.
		for (Entry<String, ClassNode> e : updatedMap.entrySet()) {
			pool.execute(() -> {
				ClassNode updated = e.getValue();
				if (updated.name.equals(nameRenamed) || updated.name.equals(nameOriginal)) {
					// Update the renamed class (itself)
					proxyClasses.put(nameRenamed, updated);
				} else {
					// Update the class that contains references to the renamed
					// class
					proxyClasses.put(updated.name, updated);
					proxyClasses.removeCache(updated.name);
				}
			});
		}
		Threads.waitForCompletion(pool);
		// Get updated node
		ClassNode node = proxyClasses.get(nameRenamed);
		if (node == null) {
			Logging.fatal(new RuntimeException("Failed to fetch updated ClassNode for remapped class: " + nameOriginal + " -> "
					+ nameRenamed));
		}
		// update inner classes
		for (InnerClassNode innerNode : node.innerClasses) {
			String inner = innerNode.name;
			// ASM gives inner-classes a constant of themselves, copied from
			// their parent.
			// So skip self-referencing values.
			if (inner.equals(nameOriginal)) {
				continue;
			}
			// And skip self-referneces for renamed parents.
			if (inner.equals(nameRenamed)) {
				continue;
			}

			// Ensure the class exists. Can contain inner classes of other nodes
			// that may not exist (such as Map$Entry)
			if (!classes.contains(inner)) {
				continue;
			}
			// New name of inner, to be defined next.
			String innerNew = null;
			// Check if inner/outer names exist. This will work for
			// non-anonymous classes.
			if (innerNode.outerName != null && innerNode.innerName != null) {
				// Verify that this should be renamed. Don't want to mess with
				// it if it is defined by another class.
				if (!innerNode.outerName.equals(nameRenamed)) {
					continue;
				}
				innerNew = nameRenamed + "$" + innerNode.innerName;
			} else {
				// Deal with anonymous class.
				//
				// Inner ----------- Host ----------- Should rename? -- Case
				// Orig$1 ---------- Renmaed$Sub ---- NO -------------- 1
				// Orig$Sub$1 ------ Renmaed$Sub ---- YES ------------- 2
				// Renmaed$Sub ----- Renmaed$Sub$1 -- NO -------------- 3
				// Renmaed$1$1 ----- Renmaed$Sub$1 -- NO -------------- 4
				//
				int splitIndex = inner.lastIndexOf("$");
				// Verify the name of the inner class does not denote that it
				// has been defined in another class. This is case 4.
				String innerHost = inner.substring(0, splitIndex);
				if (!innerHost.equals(nameOriginal) && !innerHost.equals(nameRenamed)) {
					continue;
				}
				String innerName = inner.substring(splitIndex + 1);
				// Account for case 3, where the inner is sorta an outer class.
				if (nameRenamed.startsWith(inner) && inner.length() < nameRenamed.length()) {
					continue;
				}
				innerNew = nameRenamed + "$" + innerName;
			}
			// Send rename event if innerName is updated.
			if (innerNew != null && !inner.equals(innerNew)) {
				Bus.post(new ClassRenameEvent(getClass(inner), inner, innerNew));
			}
		}
		// add new name
		classes.add(nameRenamed);
		// remove original name from input sets
		proxyClasses.remove(nameOriginal);
		dirtyClasses.remove(nameOriginal);
		history.remove(nameOriginal);
		classes.remove(nameOriginal);
		Logging.info("Rename " + nameOriginal + " -> " + nameRenamed);
	}

	@Listener(priority = -1)
	private void onFieldRename(FieldRenameEvent event) {
		String fOwner = event.getOwner().name;
		String fName = event.getOriginalName();
		String fNameNew = event.getNewName();
		Set<String> classesWithUpdates = new HashSet<>();
		for (ClassNode cn : proxyClasses.values()) {
			ClassNode updated = new ClassNode();
			cn.accept(new ClassRemapper(updated, new Remapper() {
				@Override
				public String mapFieldName(final String owner, final String name, final String descriptor) {
					if (owner.equals(fOwner) && name.equals(fName) && descriptor.equals(event.getField().desc)) {
						Bus.post(new ClassDirtyEvent(cn));
						classesWithUpdates.add(cn.name);
						return fNameNew;
					}
					return name;
				}
			}));
			if (classesWithUpdates.contains(cn.name)) {
				proxyClasses.put(updated.name, updated);
			}
		}
		Logging.info("Rename " + fOwner + "." + fName + " -> " + fOwner + "." + fNameNew);
	}

	@Listener(priority = -1)
	private void onMethodRename(MethodRenameEvent event) {
		try {
			String mOwner = event.getOwner().name;
			String mName = event.getOriginalName();
			String mNameNew = event.getNewName();
			String mDesc = event.getMethod().desc;
			Set<String> classesWithUpdates = new HashSet<>();
			for (ClassNode cn : proxyClasses.values()) {
				ClassNode updated = new ClassNode();
				cn.accept(new ClassRemapper(updated, new Remapper() {
					@Override
					public String mapMethodName(final String owner, final String name, final String descriptor) {
						if (ConfASM.instance().useLinkedMethodRenaming()) {
							// Not combined into one statement since this would
							// allow the other block to be run even if linked
							// renaming were to be active.
							if (Hierarchy.INSTANCE.linked(mOwner, mName, mDesc, owner, name, descriptor)) {
								return rename(owner, name, descriptor);
							}
						} else if (owner.equals(mOwner) && name.equals(mName) && descriptor.equals(mDesc)) {
							return rename(owner, name, descriptor);
						}
						return name;
					}

					/**
					 * Rename the method, post dirty even for the class.
					 * 
					 * @param owner
					 * @param name
					 * @param descriptor
					 * @return
					 */
					private String rename(String owner, String name, String descriptor) {
						Logging.trace("Rename " + owner + "." + name + descriptor + " -> " + mNameNew);
						Bus.post(new ClassDirtyEvent(cn));
						classesWithUpdates.add(cn.name);
						return mNameNew;
					}
				}));
				// only update neccesary classes
				if (classesWithUpdates.contains(cn.name)) {
					proxyClasses.put(updated.name, updated);
				}
			}
		} catch (Exception e) {
			Logging.error(e);
		}
	}

	/**
	 * Creates a backup of classes in the SaveStateEvent.
	 */
	@Listener(priority = -1)
	private void onSave(SaveStateEvent event) {
		for (String name : event.getClasses()) {
			// ensure history for item exists
			if (!history.containsKey(name)) {
				history.put(name, new History(name));
			}
			// Update history.
			// This will in turn update the current stored class instance.
			try {
				byte[] modified = Asm.getBytes(proxyClasses.get(name));
				History classHistory = history.get(name);
				classHistory.push(modified);
				Logging.info("Save state created for: " + name + " [" + classHistory.length + " total states]");
			} catch (Exception e) {
				Logging.error(e);
			}
		}
	}

	/**
	 * Export contents to file given by the event.
	 * 
	 * @throws IOException
	 *             Thrown if the file could not be written to, or if a file
	 *             could not be exported from the virtual file system.
	 */
	@Listener(priority = -1)
	private void onExportRequested(RequestExportEvent event) throws IOException {
		if (ConfASM.instance().doVerify() && !Verify.isValid()) {
			// isValid() will show a window detailing the first instance of bad
			// bytecode being exported.
			Logging.error("Denied export, please fix invalid bytecode");
			return;
		}
		Map<String, byte[]> contents = new TreeMap<>(new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				// Packages are higher valued than default-package entries
				if (o1.contains("/") && !o2.contains("/")) return -1;
				else if (!o1.contains("/") && o2.contains("/")) return 1;
				// Standard name comparison
				return o1.compareTo(o2);
			}
			
		});
		Logging.info("Exporting to " + event.getFile().getAbsolutePath());
		// write classes
		Set<String> modified = getModifiedClasses();
		Logging.info("Writing " + classes.size() + " classes...");
		Logging.info("\t" + modified.size() + " modified classes");
		for (String name : classes) {
			// Export if file has been modified.
			// We know if it is modified if it has a history or is marked as
			// dirty.
			if (modified.contains(name)) {
				try {
					byte[] data = Asm.getBytes(getClass(name));
					contents.put(name + ".class", data);
					continue;
				} catch (Exception e) {
					Logging.warn("Failed to export: '" + name + "' due to the following error: ");
					Logging.error(e);
				}
			}
			// Otherwise we don't even have to have ASM regenerate the
			// bytecode. We can just plop the exact original file back
			// into the output.
			// This is great for editing one file in a large system.
			//
			// Or we can be here because the export failed, which sucks, but is
			// better than not exporting anything at all. At least the user will
			// be notified in the console.
			byte[] data = getFile(getPath(name));
			contents.put(name + ".class", data);
		}
		Logging.info("Writing " + resources.size() + " resources...");
		// Write resources. Can't modify these yet so just take them directly
		// from the system.
		for (String name : resources) {
			byte[] data = getFile(getPath(name));
			contents.put(name, data);
		}
		// Post to event bus. Allow plugins to inject their own files to the
		// output.
		Bus.post(new ExportEvent(event.getFile(), contents));
		// Save contents to jar.
		try (JarOutputStream output = new JarOutputStream(new FileOutputStream(event.getFile()))) {
			Set<String> dirsVisited = new HashSet<>();
			// Contents is iterated in sorted order (because type is TreeMap).
			// This allows us to insert directory entries before file entries of that directory occur.
			for (Entry<String, byte[]> entry : contents.entrySet()) {
				String key = entry.getKey();
				// Write directories for upcoming entries if necessary
				// - Ugly, but does the job.
				if (key.contains("/")) {
					// Record directories
					String parent = key;
					List<String> toAdd = new ArrayList<>();
					do {
						parent = parent.substring(0, parent.lastIndexOf("/"));
						if (!dirsVisited.contains(parent)) {
							dirsVisited.add(parent);
							toAdd.add(0, parent + "/");
						} else break;
					} while (parent.contains("/"));
					// Put directories in order of depth
					for (String dir : toAdd) {
						output.putNextEntry(new JarEntry(dir));
						output.closeEntry();
					}
				}
				// Write entry content
				output.putNextEntry(new JarEntry(key));
				output.write(entry.getValue());
				output.closeEntry();
			}
		}
		Logging.info("Exported to: " + event.getFile());

	}

	/**
	 * Populates class and resource maps.
	 * 
	 * @throws IOException
	 *             Thrown if the archive could not be read, or an internal file
	 *             could not be read.
	 */
	private Map<String, byte[]> readArchive() throws IOException {
		Map<String, byte[]> contents = new HashMap<>();
		try (ZipFile file = new ZipFile(input)) {
			// iterate zip entries
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// skip directories
				if (entry.isDirectory()) continue;
				try (InputStream is = file.getInputStream(entry)) {
					// add as class, or resource if not a class file.
					String name = entry.getName();
					if (name.endsWith(".class")) {
						addClass(contents, name, is);
					} else {
						addResource(contents, name, is);
					}
				}
			}
		}
		return contents;
	}

	/**
	 * Populates class map.
	 * 
	 * @throws IOException
	 *             Thrown if the file could not be read.
	 */
	private Map<String, byte[]> readClass() throws IOException {
		Map<String, byte[]> contents = new HashMap<>();
		try (InputStream is = new FileInputStream(input)) {
			addClass(contents, input.getName(), is);
		}
		return contents;
	}

	/**
	 * Try to add the class contained in the given stream to the classes map.
	 * 
	 * @param name
	 *            Entry name.
	 * @param is
	 *            Stream of entry.
	 * @throws IOException
	 *             Thrown if stream could not be read or if ClassNode could not
	 *             be derived from the streamed content.
	 */
	private void addClass(Map<String, byte[]> contents, String name, InputStream is) throws IOException {
		byte[] value = Streams.from(is);
		try {
			ClassReader cr = new ClassReader(value);
			String className = cr.getClassName();
			// run some basic verification
			if (className.endsWith("/")) {
				Logging.warn(String.format("Invalid file-name, '%s', skipping this entry", name));
				return;
			}
			if (!verify(cr)) {
				Logging.warn(String.format("Invalid code, '%s', skipping this entry", name));
				return;
			}
			classes.add(className);
			contents.put(className, value);
		} catch (Exception e) {
			Logging.warn(String.format("Could not read archive entry: '%s' as a class. Added as resource instead.", name));
			contents.put(name, value);
		}
	}

	/**
	 * Try to add the resource contained in the given stream to the resource
	 * map.
	 * 
	 * @param name
	 *            Entry name.
	 * @param is
	 *            Stream of entry.
	 * @throws IOException
	 *             Thrown if stream could not be read.
	 */
	private void addResource(Map<String, byte[]> contents, String name, InputStream is) throws IOException {
		resources.add(name);
		contents.put(name, Streams.from(is));
	}

	/**
	 * @return Map of ClassNodes.
	 */
	public Map<String, ClassNode> getClasses() {
		return proxyClasses;
	}

	/**
	 * Get a ClassNode by its name.
	 * 
	 * @param name
	 *            Name of class.
	 * @return ClassNode if found, {@code null} otherwise.
	 */
	public ClassNode getClass(String name) {
		return proxyClasses.get(name);
	}

	/**
	 * @return Map of resources.
	 */
	public Map<String, byte[]> getResources() {
		return proxyResources;
	}

	/**
	 * Get a resource by its name.
	 * 
	 * @param name
	 *            Name of resource.
	 * @return {@code byte[]} if found, {@code null} otherwise.
	 */
	public byte[] getResource(String name) {
		return proxyResources.get(name);
	}

	/**
	 * @return Map of save states for classes.
	 */
	public Map<String, History> getHistory() {
		return history;
	}

	/**
	 * Undo the last action for the ClassNode with the given name. To receive
	 * changes re-use {@link #getClass(String)}.
	 * 
	 * @param name
	 * @throws IOException
	 */
	public void undo(String name) throws IOException {
		History hist = history.get(name);
		byte[] last = hist.pop();
		if (last != null) {
			proxyClasses.removeCache(name);
			write(getPath(name), last);
			Bus.post(new HistoryRevertEvent(name));
			Logging.info("Reverted '" + name + "'");
		} else {
			Logging.info("No history for '" + name + "' to revert back to");
		}
	}

	/**
	 * Generate FileSystem to represent contained data:
	 * <ul>
	 * <li>{@link #classes}</li>
	 * <li>{@link #resources}</li>
	 * </ul>
	 * 
	 * @return FileSystem representation of input.
	 * @throws IOException
	 */
	private FileSystem createSystem(Map<String, byte[]> contents) throws IOException {
		Logging.fine("Creating internal file-system for archive...");
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		for (Entry<String, byte[]> entry : contents.entrySet()) {
			Path path = getPath(fs, entry.getKey());
			write(path, entry.getValue());
		}
		return fs;
	}

	/**
	 * Generate FileSystem to represent classes loaded in the given
	 * Instrumentation instance.
	 * 
	 * @param instrumentation
	 * @return FileSystem representation of instrumentation.
	 * @throws IOException
	 */
	private FileSystem createSystem(Instrumentation instrumentation) throws IOException {
		Logging.fine("Creating internal file-system for instrumented runtime...");
		FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
		// Wrapped return of "readContents" in another map to prevent
		// ConcurrentModificationException for when a new class is loaded
		// while this loop is executing.
		Map<String, byte[]> contents = new HashMap<>(readContents(instrumentation));
		for (Entry<String, byte[]> entry : contents.entrySet()) {
			Path path = getPath(fs, entry.getKey());
			write(path, entry.getValue());
		}
		return fs;
	}

	/**
	 * Read bytecode of classes in the instrumented environment.
	 * 
	 * @param instrumentation
	 * @return Map of runtime classes's bytecode.
	 * @throws IOException
	 */
	private Map<String, byte[]> readContents(Instrumentation instrumentation) throws IOException {
		Map<String, byte[]> map = new HashMap<>();
		// add all existing classes
		for (Class<?> c : instrumentation.getAllLoadedClasses()) {
			String name = c.getName().replace(".", "/");
			String path = name + ".class";
			ClassLoader loader = c.getClassLoader();
			if (loader == null) {
				loader = ClassLoader.getSystemClassLoader();
			}
			InputStream is = loader.getResourceAsStream(path);
			if (is != null) {
				classes.add(name);
				map.put(name, Streams.from(is));
			}
		}
		return map;
	}

	/**
	 * Called after the window is loaded. This allows the UI to register an
	 * instance of "Input" so that it can use it when fetching values posted by
	 * the transformer in this method.
	 */
	public void registerLoadListener() {
		if (instrumentation == null) return;
		// register transformer so new classes can be added on the fly
		instrumentation.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				// skip invalid entries
				if (className == null || classfileBuffer == null) {
					return classfileBuffer;
				}
				// add classes as they're loaded
				try {
					instLoaded(className, classfileBuffer);
				} catch (IOException e) {
					Logging.warn("Failed to load inst. class: " + className);
				}
				return classfileBuffer;
			}
		}, true);
	}

	@Listener
	private void onAgentSave(RequestAgentSaveEvent event) {
		if (ConfASM.instance().doVerify() && !Verify.isValid()) {
			Logging.error("Denied export, please fix invalid bytecode");
			return;
		}
		Map<String, byte[]> targets = new HashMap<>();
		Set<String> modified = getModifiedClasses();
		Logging.info("Redefining classes...");
		Logging.info("\t" + modified.size() + " modified classes");
		// write classes
		for (String name : classes) {
			// Export if file has been modified.
			// We know if it is modified if it has a history or is marked as
			// dirty.
			if (modified.contains(name)) {
				try {
					byte[] data = Asm.getBytes(getClass(name));
					targets.put(name, data);
				} catch (Exception e) {
					Logging.warn("Failed to export: '" + name + "' due to the following error: ");
					Logging.error(e);
				}
			}
		}
		// Post to event bus. Allow plugins to inject their own files to the
		// output.
		Bus.post(new AgentSaveEvent(instrumentation, targets));
		// Create new definitions and apply
		try {
			int i = 0;
			ClassDefinition[] defs = new ClassDefinition[targets.size()];
			for (Entry<String, byte[]> entry : targets.entrySet()) {
				String name = entry.getKey().replace("/", ".");
				ClassLoader loader = ClassLoader.getSystemClassLoader();
				defs[i] = new ClassDefinition(Class.forName(name, false, loader), entry.getValue());
				i++;
			}
			instrumentation.redefineClasses(defs);
			Logging.info("Redefinition complete.");
		} catch (Exception e) {
			Logging.error(e);
		}
		// clear dirty list
		dirtyClasses.clear();
	}

	/**
	 * @return Set of class names of modified files.
	 */
	public Set<String> getModifiedClasses() {
		Set<String> set = new HashSet<>();
		set.addAll(dirtyClasses);
		set.addAll(history.keySet());
		return set;
	}

	/**
	 * When a class is loaded via the transformer, add it to the file-system and
	 * class list.
	 * 
	 * @param className
	 * @param classfileBuffer
	 * @throws IOException
	 */
	private void instLoaded(String className, byte[] classfileBuffer) throws IOException {
		// add to file system
		Path path = getPath(className);
		Files.createDirectories(path.getParent());
		write(path, classfileBuffer);
		// add to class list
		classes.add(className);
		// send notification
		Bus.post(new ClassLoadInstrumentedEvent(className));
	}

	/**
	 * Verify that constant-pool values are not malformed.
	 * 
	 * @param cr
	 *            ClassReader.
	 * @return Validity of code, {@code true} for valid.
	 */
	private static boolean verify(ClassReader cr) {
		try {
			// The class reader will attempt to parse the raw bytecode stored in
			// the array "b" in ClassReader and save it back to a class file via
			// ClassWriter. If any errors occur in this process, the code is
			// invalid.
			//
			// This kinda sucks for speed/performance, but hey it means not
			// dealing with invalid code (which IMO are not within the scope of
			// this tool)
			cr.accept(new ClassWriter(0), 0);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Wrapper for system.getPath().
	 * 
	 * @param name
	 *            File name.
	 * @return File path.
	 */
	private Path getPath(String name) {
		return getPath(getFileSystem(), name);
	}

	/**
	 * Wrapper for system.getPath().
	 * 
	 * @param system
	 *            File system to fetch path from.
	 * @param name
	 *            File name.
	 * 
	 * @return File path.
	 */
	private Path getPath(FileSystem system, String name) {
		Path path = system.getPath("/" + name);
		path = extensionify(path);
		return path;
	}

	/**
	 * @return Files contained within input as navigable file system.
	 */
	public FileSystem getFileSystem() {
		return system;
	}

	/**
	 * Retrieve bytes of file in the {@link #getFileSystem() virtual system}.
	 * 
	 * @param name
	 *            File name. For classes, use internal names.
	 * @return Bytes of requested files.
	 * @throws IOException
	 *             File could not be read from.
	 */
	public byte[] getFile(String name) throws IOException {
		return Files.readAllBytes(getPath(name));
	}

	/**
	 * Retrieve bytes of file in the {@link #getFileSystem() virtual system}.
	 * 
	 * @param path
	 *            File path.
	 * @return Bytes of requested files.
	 * @throws IOException
	 *             File could not be read from.
	 */
	private static byte[] getFile(Path path) throws IOException {
		return Files.readAllBytes(path);
	}

	/**
	 * Removes file by name from local system.
	 * 
	 * @param name
	 *            File name.
	 * @throws IOException
	 *             Thrown if the file could not be removed.
	 */
	private void removeFile(String name) throws IOException {
		Files.delete(getPath(name));
	}

	/**
	 * Add file to local system.
	 * 
	 * @param path
	 *            Path to write to.
	 * @param value
	 *            Bytes to write.
	 * @throws IOException
	 *             Thrown if could not write to path.
	 */
	private static void write(Path path, byte[] value) throws IOException {
		ensureParentExists(path);
		Files.write(path, value, StandardOpenOption.CREATE);
	}

	/**
	 * Ensures the parent path exists. If the parent of the given path does not
	 * exist, all directories leading up to it will be created.
	 * 
	 * @param path
	 *            Path to some file.
	 * @throws IOException
	 *             Thrown if the parent of the given path could not be created
	 *             as a directory.
	 */
	private static void ensureParentExists(Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null && !Files.isDirectory(parent)) {
			Files.createDirectories(parent);
		}
	}

	/**
	 * Append extension to given path.
	 * 
	 * @param path
	 *            Path to a file.
	 * @return Path to file, with added extension.
	 */
	private static Path extensionify(Path path) {
		if (path.endsWith(FILE_EXT)) {
			return path;
		}
		return path.resolveSibling(path.getFileName() + FILE_EXT);
	}

	/**
	 * @return FileMap of classes.
	 */
	private FileMap<String, ClassNode> createClassMap() {
		return new FileMap<String, ClassNode>(classes) {
			@Override
			ClassNode castValue(byte[] file) {
				try {
					return Asm.getNode(file);
				} catch (IOException e) {
					Logging.fatal(e);
				}
				return null;
			}

			@Override
			byte[] castBytes(ClassNode value) {
				try {
					return Asm.getBytes(value);
				} catch (Exception e) {
					Logging.warn("Failed to convert to raw byte[]: '" + value.name + "' due to the following error: ");
					Logging.error(e);
				}
				try {
					return getFile(getPath(value.name));
				} catch (IOException e) {
					Logging.warn("Failed to fetch fallback value of byte[] for: '" + value.name
							+ "' due to the following error: ");
					Logging.error(e);
				}
				return new byte[0];
			}

			@Override
			String castKey(Object in) {
				return in.toString();
			}
		};
	}

	/**
	 * @return FileMap of resources.
	 */
	private FileMap<String, byte[]> createResourceMap() {
		return new FileMap<String, byte[]>(resources) {
			@Listener
			private void onUpdate(ResourceUpdateEvent event) {
				if (event.getResource() == null) {
					remove(event.getResourceName());
				} else {
					cache.remove(event.getResourceName());
				}
			}

			@Override
			byte[] castValue(byte[] file) {
				return file;
			}

			@Override
			byte[] castBytes(byte[] value) {
				return value;
			}

			@Override
			String castKey(Object in) {
				return in.toString();
			}
		};
	}

	/**
	 * History manager for files.
	 * 
	 * @author Matt
	 */
	public class History {
		/**
		 * File being tracked.
		 */
		public final String name;
		/**
		 * Number of elements.
		 */
		public int length;

		public History(String name) {
			this.name = name;
		}

		/**
		 * Wipe all items from the history.
		 * 
		 * @throws IOException
		 *             Thrown if the history file could not be deleted.
		 */
		public void clear() throws IOException {
			for (int i = 0; i < length; i++) {
				Path cur = getPath(name + HIST_POSTFIX + i);
				Files.delete(cur);
			}
			length = 0;
		}

		/**
		 * Fetch the creation times of all files in the history.
		 * 
		 * @return Creation times. Lower index = older files.
		 * @throws IOException
		 *             Thrown if the history file could not have their
		 *             attributes read.
		 */
		public Instant[] getFileTimes() throws IOException {
			Instant[] instants = new Instant[length];
			for (int i = 0; i < length; i++) {
				Path file = getPath(name + HIST_POSTFIX + i);
				BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
				instants[i] = attr.creationTime().toInstant();
			}
			return instants;
		}

		/**
		 * Gets most recent change, shifts all history up.
		 * 
		 * @return Last value before change.
		 * @throws IOException
		 *             Thrown if the history file could not be read, or if
		 *             history files could not be re-arranged.
		 */
		public byte[] pop() throws IOException {
			// Path of most recent element
			Path lastPath = getPath(name + HIST_POSTFIX + "0");
			if (Files.exists(lastPath)) {
				byte[] last = getFile(lastPath);
				// Move histories up.
				// Acts like a history stack.
				for (int i = -1; i < length; i++) {
					Path cur = i == -1 ? getPath(name) : getPath(name + HIST_POSTFIX + i);
					Path newer = getPath(name + HIST_POSTFIX + (i + 1));
					if (Files.exists(newer)) {
						Files.move(newer, cur, StandardCopyOption.REPLACE_EXISTING);
					}
				}
				// Decrease history count, return last
				length--;
				return last;
			}
			// No history to be found
			return null;
		}

		/**
		 * Updates current value, pushing the latest value into the history
		 * stack.
		 * 
		 * @param modified
		 *            Changed value.
		 * @throws IOException
		 *             thrown if the value failed to push onto the stack.
		 */
		public void push(byte[] modified) throws IOException {
			Path path = getPath(name);
			for (int i = Math.min(length, HISTORY_LENGTH) + 1; i > 0; i--) {
				Path cur = getPath(name + HIST_POSTFIX + i);
				Path newer = getPath(name + HIST_POSTFIX + (i - 1));
				// start of history
				if (Files.exists(newer)) {
					Files.move(newer, cur, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			// Update lastest history element.
			Path pathNewHistory = getPath(name + HIST_POSTFIX + "0");
			write(pathNewHistory, getFile(name));
			// Update current value.
			write(path, modified);
			length++;
			if (length > HISTORY_LENGTH) {
				length = HISTORY_LENGTH;
			}
		}
	}

	/**
	 * Map wrapper for this structure. Allows users to access classes in a
	 * familiar fashion.
	 * 
	 * @author Matt
	 *
	 * @param <K>
	 * @param <V>
	 */
	public abstract class FileMap<K, V> implements Map<K, V> {
		protected Map<K, V> cache = new HashMap<>();
		protected final Set<K> keys;

		public FileMap(Set<K> keys) {
			this.keys = keys;
		}

		/**
		 * Exists because casting generics alone is a bad idea without
		 * implementation information.
		 * 
		 * @param in
		 *            Key input.
		 * @return Key output.
		 */
		abstract K castKey(Object in);

		/**
		 * Exists because casting generics alone is a bad idea without
		 * implementation information.
		 * 
		 * @param file
		 *            Value input.
		 * @return Value output.
		 */
		abstract V castValue(byte[] file);

		/**
		 * Exists because casting generics alone is a bad idea without
		 * implementation information.
		 * 
		 * @param value
		 *            Value input.
		 * @return {@code byte[]} of input.
		 */
		abstract byte[] castBytes(V value);

		@Override
		public int size() {
			return keys.size();
		}

		@Override
		public boolean isEmpty() {
			return keys.isEmpty();
		}

		@Override
		public boolean containsKey(Object key) {
			return keys.contains(key);
		}

		public boolean containsCache(Object key) {
			return cache.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			throw new UnsupportedOperationException();
		}

		public void removeCache(Object key) {
			cache.remove(key);
		}

		@Override
		public V get(Object key) {
			// check if cached copy exists.
			V v = cache.get(key);
			if (v != null) {
				return v;
			}
			// no cache, fetch from file system, add to cache.
			try {
				v = castValue(getFile(key.toString()));
				cache.put(castKey(key), v);
				return v;
			} catch (ClosedByInterruptException e) {
				// happens when closing the editor window when runnon on an
				// instrumented input.
				// Ignore.
			} catch (IOException e) {
				Logging.warn(e);
			}
			return null;
		}

		@Override
		public V put(K key, V value) {
			keys.add(key);
			cache.remove(key);
			try {
				write(getPath(key.toString()), castBytes(value));
			} catch (IOException e) {
				Logging.fatal(e);
			}
			return value;
		}

		@Override
		public V remove(Object key) {
			String ks = key.toString();
			V v = get(key);
			try {
				removeFile(ks);
				keys.remove(ks);
				history.remove(ks);
			} catch (IOException e) {
				return v;
			}
			return v;
		}

		@Override
		public void putAll(Map<? extends K, ? extends V> m) {
			for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public void clear() {
			cache.clear();
			for (K key : keys) {
				remove(key);
				history.remove(key);
			}
		}

		@Override
		public Set<K> keySet() {
			return keys;
		}

		@Override
		public Collection<V> values() {
			Set<V> s = new HashSet<>();
			for (K key : keys) {
				s.add(get(key));
			}
			return s;
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			Set<Entry<K, V>> s = new HashSet<>();
			for (final K key : keys) {
				final V value = get(key);
				s.add(new Entry<K, V>() {
					@Override
					public K getKey() {
						return key;
					}

					@Override
					public V getValue() {
						return value;
					}

					@Override
					public V setValue(V value) {
						throw new UnsupportedOperationException();
					}
				});
			}
			return s;
		}
	}

	/**
	 * Current instance.
	 */
	private static Input current;

	public static Input get() {
		return current;
	}
}