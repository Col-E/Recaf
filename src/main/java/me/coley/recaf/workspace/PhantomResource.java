package me.coley.recaf.workspace;

import me.coley.recaf.Recaf;
import me.coley.recaf.command.impl.Export;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.TypeUtil;
import org.clyze.jphantom.ClassMembers;
import org.clyze.jphantom.JPhantom;
import org.clyze.jphantom.Options;
import org.clyze.jphantom.Phantoms;
import org.clyze.jphantom.access.ClassAccessStateMachine;
import org.clyze.jphantom.access.FieldAccessStateMachine;
import org.clyze.jphantom.access.MethodAccessStateMachine;
import org.clyze.jphantom.adapters.ClassPhantomExtractor;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.clyze.jphantom.hier.IncrementalClassHierarchy;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Resource for holding phantom references.
 *
 * @author Matt
 */
public class PhantomResource extends JavaResource {
	private static final ResourceLocation LOCATION = LiteralResourceLocation.ofKind(ResourceKind.JAR, "Phantoms");
	private static final Path PHANTOM_DIR = Recaf.getDirectory("classpath").resolve("generated");
	// TODO: Update phantom refs when:
	//  - using the recompilers
	//  - assembling methods (just at startup?)

	// TODO: Add a visual indicator when this passes / fails

	/**
	 * Constructs the phantom resource.
	 */
	public PhantomResource() {
		super(ResourceKind.JAR);
	}

	/**
	 * Clear the Phantom class cache internally and in file cache.
	 *
	 * @throws IOException
	 * 		When the files cannot be deleted.
	 */
	public void clear() throws IOException {
		// Clear internal
		getClasses().clear();
		// Clear file cache
		Path input = PHANTOM_DIR.resolve("input.jar");
		Path output = PHANTOM_DIR.resolve("output.jar");
		if (!Files.isDirectory(PHANTOM_DIR))
			Files.createDirectories(PHANTOM_DIR);
		Files.deleteIfExists(input);
		Files.deleteIfExists(output);
	}

	/**
	 * Populates the current resource with phantom classes
	 * and dumps the classes into {@code [RECAF]/classpath/generated/output.jar}
	 *
	 * @param classes
	 * 		Collection of classes to generate phantoms for.
	 *
	 * @throws IOException
	 * 		Thrown when JPhantom cannot read from the temporary file where these classes are written to.
	 */
	public void populatePhantoms(Collection<byte[]> classes) throws IOException {
		Log.debug("Begin generating phantom classes, given {} input classes", classes.size());
		// Clear old classes
		clear();
		Path input = PHANTOM_DIR.resolve("input.jar");
		Path output = PHANTOM_DIR.resolve("output.jar");
		// Write the parameter passed classes to a temp jar
		Map<String, byte[]> classMap = new HashMap<>();
		Map<Type, ClassNode> nodes = new HashMap<>();
		classes.forEach(c -> {
			ClassReader cr = new ClassReader(c);
			ClassNode node = ClassUtil.getNode(cr, 0);
			classMap.put(node.name + ".class", c);
			nodes.put(Type.getObjectType(node.name), node);
		});
		Export.writeArchive(true, input.toFile(), classMap);
		Log.debug("Wrote classes to temp file, starting phantom analysis...", classes.size());
		// Read into JPhantom
		Options.V().setSoftFail(true);
		Options.V().setJavaVersion(8);
		ClassHierarchy hierarchy = clsHierarchyFromArchive(new JarFile(input.toFile()));
		ClassMembers members = ClassMembers.fromJar(new JarFile(input.toFile()), hierarchy);
		classes.forEach(c -> {
			ClassReader cr = new ClassReader(c);
			if (cr.getClassName().contains("$"))
				return;
			try {
				cr.accept(new ClassPhantomExtractor(hierarchy, members), 0);
			} catch (Throwable t) {
				Log.debug("Phantom extraction failed: {}", t);
			}
		});
		// Remove duplicate constraints for faster analysis
		Set<String> existingConstraints = new HashSet<>();
		ClassAccessStateMachine.v().getConstraints().removeIf(c -> {
			boolean isDuplicate = existingConstraints.contains(c.toString());
			existingConstraints.add(c.toString());
			return isDuplicate;
		});
		// Execute and populate the current resource with generated classes
		JPhantom phantom = new JPhantom(nodes, hierarchy, members);
		phantom.run();
		phantom.getGenerated().forEach((k, v) -> getClasses().put(k.getInternalName(), decorate(v)));
		classMap.clear();
		getClasses().forEach((k, v) -> classMap.put(k + ".class", v));
		Export.writeArchive(true, output.toFile(), classMap);
		Log.debug("Phantom analysis complete, cleaning temp file", classes.size());
		// Cleanup
		Phantoms.refresh();
		ClassAccessStateMachine.refresh();
		FieldAccessStateMachine.refresh();
		MethodAccessStateMachine.refresh();
		Files.deleteIfExists(input);
	}

	/**
	 * This is copy pasted from JPhantom, modified to be more lenient towards obfuscated inputs.
	 *
	 * @param file
	 * 		Some jar file.
	 *
	 * @return Class hierarchy.
	 *
	 * @throws IOException
	 * 		When the archive cannot be read.
	 */
	private static ClassHierarchy clsHierarchyFromArchive(JarFile file) throws IOException {
		try {
			ClassHierarchy hierarchy = new IncrementalClassHierarchy();
			for (Enumeration<JarEntry> e = file.entries(); e.hasMoreElements(); ) {
				JarEntry entry = e.nextElement();
				if (entry.isDirectory())
					continue;
				if (!entry.getName().endsWith(".class"))
					continue;
				try (InputStream stream = file.getInputStream(entry)) {
					ClassReader reader = new ClassReader(stream);
					String[] ifaceNames = reader.getInterfaces();
					Type clazz = Type.getObjectType(reader.getClassName());
					Type superclass = reader.getSuperName() == null ?
							TypeUtil.OBJECT_TYPE : Type.getObjectType(reader.getSuperName());
					Type[] ifaces = new Type[ifaceNames.length];
					for (int i = 0; i < ifaces.length; i++)
						ifaces[i] = Type.getObjectType(ifaceNames[i]);
					// Add type to hierarchy
					boolean isInterface = (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
					try {
						if (isInterface) {
							hierarchy.addInterface(clazz, ifaces);
						} else {
							hierarchy.addClass(clazz, superclass, ifaces);
						}
					} catch (Exception ex) {
						Log.error(ex, "JPhantom: Hierarchy failure for: {}", clazz);
					}
				} catch (IOException ex) {
					Log.error(ex, "JPhantom: IO Error reading from archive: {}", file.getName());
				}
			}
			return hierarchy;
		} finally {
			file.close();
		}
	}

	/**
	 * Adds a note to the given class that it has been auto-generated.
	 *
	 * @param generated
	 * 		Input generated JPhantom class.
	 *
	 * @return modified class that clearly indicates it is generated.
	 */
	private byte[] decorate(byte[] generated) {
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new ClassVisitor(Recaf.ASM_VERSION, cw) {
			@Override
			public void visitEnd() {
				visitAnnotation("LAutoGenerated;", true)
						.visit("msg", "Recaf/JPhantom automatically generated this class");
				super.visitEnd();
			}
		};
		new ClassReader(generated).accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return cw.toByteArray();
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	protected Map<String, byte[]> loadFiles() throws IOException {
		return Collections.emptyMap();
	}

	@Override
	public ResourceLocation getShortName() {
		return LOCATION;
	}

	@Override
	public ResourceLocation getName() {
		return LOCATION;
	}
}
