package me.coley.recaf;

import me.coley.event.Bus;
import me.coley.event.Listener;
import me.coley.recaf.bytecode.ClassUtil;
import me.coley.recaf.bytecode.analysis.Hierarchy;
import me.coley.recaf.bytecode.analysis.Verify;
import me.coley.recaf.config.impl.ConfASM;
import me.coley.recaf.event.*;
import me.coley.recaf.util.*;
import me.coley.recaf.workspace.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;

import java.io.*;
import java.lang.instrument.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * 
 * @author Matt
 */
public class Input{
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
	public final Set<String> resources = Collections.newSetFromMap(new ConcurrentHashMap<>());
	/**
	 * Map of class names to ClassNodes.
	 */
	private final ClassesMap classMap = new ClassesMap();
	/**
	 * Map of resource names to their value.
	 */
	private final ResourcesMap resourceMap = new ResourcesMap();
	/**
	 * History manager of changes.
	 */
	private final Map<String, History> history = new ConcurrentHashMap<>();
	/**
	 * Inheritance hierarchy utility.
	 */
	private final Hierarchy hierarchy;
	
	public Input(Instrumentation instrumentation) throws IOException {
		this.input = null;
		this.instrumentation = instrumentation;
		current = Optional.of(this);
		InputBuilder builder = new InputBuilder(instrumentation);
		classes.addAll(builder.getClasses());
		resources.addAll(builder.getResources());
		classMap.putAllRaw(builder.getClassContent());
		resourceMap.putAll(builder.getResourceContent());
		hierarchy = new Hierarchy(this);
		Bus.subscribe(this);
	}

	public Input(File input) throws IOException {
		this.input = input;
		this.instrumentation = null;
		current = Optional.of(this);
		InputBuilder builder = new InputBuilder(input);
		classes.addAll(builder.getClasses());
		resources.addAll(builder.getResources());
		classMap.putAllRaw(builder.getClassContent());
		resourceMap.putAll(builder.getResourceContent());
		hierarchy = new Hierarchy(this);
		Bus.subscribe(this);
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
				Bus.unsubscribe(classMap);
				Bus.unsubscribe(resourceMap);
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
		ExecutorService pool = Threads.pool();
		for (ClassNode cn : classMap.values()) {
			pool.execute(() -> {
				ClassNode updated = new ClassNode();
				cn.accept(new ClassRemapper(updated, new Remapper() {
					@Override
					public String map(String internalName) {
						if (internalName.equals(nameOriginal)) {
							// mark classes that have referenced the renamed class.
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
		pool = Threads.pool();
		// Update all classes with references to the renamed class.
		for (Entry<String, ClassNode> e : updatedMap.entrySet()) {
			pool.execute(() -> {
				// Remove old ClassNode
				classMap.removeRaw(e.getKey());
				// Put & update new ClassNode
				ClassNode updated = e.getValue();
				if (updated.name.equals(nameRenamed) || updated.name.equals(nameOriginal)) {
					// Update the renamed class (itself)
					classMap.put(nameRenamed, updated);
				} else {
					// Update the class that contains references to the renamed class
					classMap.put(updated.name, updated);
					classMap.remove(updated.name);
				}
			});
		}
		Threads.waitForCompletion(pool);
		// Get updated node
		ClassNode node = classMap.get(nameRenamed);
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
				Bus.post(new ClassReloadEvent(inner, innerNew));
			}
		}
		// add new name
		classes.add(nameRenamed);
		// remove original name from input sets
		classMap.remove(nameOriginal);
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
		for (ClassNode cn : classMap.values()) {
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
				classMap.put(updated.name, updated);
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
			for (ClassNode cn : classMap.values()) {
				ClassNode updated = new ClassNode();
				cn.accept(new ClassRemapper(updated, new Remapper() {
					@Override
					public String mapMethodName(final String owner, final String name, final String descriptor) {
						if (ConfASM.instance().useLinkedMethodRenaming()) {
							if (getHierarchy().areLinked(mOwner, mName, mDesc, owner, name, descriptor)) {
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
					classMap.put(updated.name, updated);
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
				history.put(name, new History(classMap, name));
			}
			// Update history.
			// This will in turn update the current stored class instance.
			try {
				byte[] modified = ClassUtil.getBytes(classMap.get(name));
				History classHistory = history.get(name);
				classHistory.push(modified);
				Logging.info("Save state created for: " + name + " [" + classHistory.size() + " total states]");
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
					byte[] data = ClassUtil.getBytes(getClass(name));
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
			byte[] data = classMap.getRaw(name);
			contents.put(name + ".class", data);
		}
		Logging.info("Writing " + resources.size() + " resources...");
		// Write resources. Can't modify these yet so just take them directly
		// from the system.
		for (String name : resources) {
			byte[] data = resourceMap.get(name);
			contents.put(name, data);
		}
		// Post to event bus. Allow plugins to inject their own files to the
		// output.
		Bus.post(new ExportEvent(event.getFile(), contents));
		// Save contents to jar.

		if (event.getFile().getName().endsWith(".class")) {
			if (contents.size() != 1) {
				Logging.error("Cannot export multiple resources into .class file!");
				return;
			}
			Files.write(event.getFile().toPath(), contents.values().iterator().next(), StandardOpenOption.CREATE);
		} else {
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
		}
		Logging.info("Exported to: " + event.getFile());

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
					byte[] data = ClassUtil.getBytes(getClass(name));
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
				String name = entry.getKey().replace('/', '.');
				defs[i++] = new ClassDefinition(Classpath.getSystemClass(name), entry.getValue());
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
	 * Called after the window is loaded. This allows the UI to register an
	 * instance of "Input" so that it can use it when fetching values posted by
	 * the transformer in this method.
	 */
	public void registerLoadListener() {
		if (instrumentation == null) return;
		// register transformer so new classes can be added on the fly
		instrumentation.addTransformer(new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
				// skip invalid entries
				if (className == null || classfileBuffer == null) {
					return classfileBuffer;
				}
				// skip skipped packages
				if (Misc.skipIgnoredPackage(className)) {
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

	/**
	 * When a class is loaded via the transformer, add it to the file-system and
	 * class list.
	 *
	 * @param className
	 * @param classfileBuffer
	 * @throws IOException
	 */
	private void instLoaded(String className, byte[] classfileBuffer) throws IOException {
		// add to the class map
		classMap.putRaw(className, classfileBuffer);
		// add to class list
		classes.add(className);
		// send notification
		Bus.post(new ClassLoadInstrumentedEvent(className));
	}

	/**
	 * @return Inheritance hierarchy utility.
	 */
	public Hierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * @return Map of ClassNodes.
	 */
	public ClassesMap getClasses() {
		return classMap;
	}

	/**
	 * Get a ClassNode by its name.
	 * 
	 * @param name
	 *            Name of class.
	 * @return ClassNode if found, {@code null} otherwise.
	 */
	public ClassNode getClass(String name) {
		return classMap.get(name);
	}

	/**
	 * Get a class's bytecode by its name.
	 *
	 * @param name
	 * 		Name of class.
	 *
	 * @return Bytecode if found, {@code null} otherwise.
	 */
	public byte[] getClassRaw(String name) {
		return classMap.getRaw(name);
	}

	/**
	 * @return Map of resources.
	 */
	public ResourcesMap getResources() {
		return resourceMap;
	}

	/**
	 * Get a resource by its name.
	 * 
	 * @param name
	 *            Name of resource.
	 * @return {@code byte[]} if found, {@code null} otherwise.
	 */
	public byte[] getResource(String name) {
		return resourceMap.get(name);
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
	public void undo(String name) {
		history.get(name).pop();
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
	 * Current instance, wrapped by an {@link Optional}.
	 */
	private static Optional<Input> current = Optional.empty();

	/**
	 * @return Current instance.
	 */
	public static Input get() {
		return current.orElse(null);
	}

	/**
	 * @return the current instance, wrapped by an {@link Optional}.
	 */
	public static Optional<Input> getOptional() {
		return current;
	}

	/**
	 * @return {@code true} if the current instance is present, {@code false} otherwise.
	 */
	public static boolean hasInput() {
		return getOptional().isPresent();
	}
}