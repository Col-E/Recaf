package me.coley.recaf.workspace;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.ReflectUtil;
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
import java.util.*;

/**
 * Resource for holding phantom references.
 *
 * @author Matt
 */
public class PhantomResource extends JavaResource {
	private static final ResourceLocation LOCATION = LiteralResourceLocation.ofKind(ResourceKind.JAR, "Phantoms");
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
		getClasses().clear();
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
	public void populatePhantoms(Map<String, byte[]> classes) throws IOException {
		Log.debug("Begin generating phantom classes, given {} input classes", classes.size());
		// Clear old classes
		clear();
		// Write the parameter passed classes to a temp jar
		Map<Type, ClassNode> nodes = new HashMap<>();
		classes.forEach((name, c) -> {
			ClassReader cr = new ClassReader(c);
			ClassNode node = ClassUtil.getNode(cr, 0);
			nodes.put(Type.getObjectType(node.name), node);
		});
		// Read into JPhantom
		Options.V().setSoftFail(true);
		Options.V().setJavaVersion(8);
		ClassHierarchy hierarchy = createHierarchy(classes);
		ClassMembers members = createMembers(classes, hierarchy);
		classes.forEach((name, c) -> {
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
		Log.debug("Phantom analysis complete, generated {} classes", classes.size());
		// Cleanup
		Phantoms.refresh();
		ClassAccessStateMachine.refresh();
		FieldAccessStateMachine.refresh();
		MethodAccessStateMachine.refresh();
	}

	/**
	 * @param classMap
	 * 		Map to pull classes from.
	 * @param hierarchy
	 * 		Hierarchy to pass to {@link ClassMembers} constructor.
	 *
	 * @return Members instance.
	 */
	public static ClassMembers createMembers(Map<String, byte[]> classMap, ClassHierarchy hierarchy) {
		Class<?>[] argTypes = new Class[]{ClassHierarchy.class};
		Object[] argVals = new Object[]{hierarchy};
		ClassMembers repo = ReflectUtil.quietNew(ClassMembers.class, argTypes, argVals);
		try {
			new ClassReader("java/lang/Object").accept(repo.new Feeder(), 0);
		} catch (IOException ex) {
			Log.error("Failed to get initial reader ClassMembers, could not lookup 'java/lang/Object'");
			throw new IllegalStateException();
		}
		for (Map.Entry<String, byte[]> e : classMap.entrySet()) {
			try {
				new ClassReader(e.getValue()).accept(repo.new Feeder(), 0);
			} catch (Throwable t) {
				Log.debug("Could not supply {} to ClassMembers feeder", e.getKey(), t);
			}
		}
		return repo;
	}

	/**
	 * @param classMap
	 * 		Map to pull classes from.
	 *
	 * @return Class hierarchy.
	 */
	public static ClassHierarchy createHierarchy(Map<String, byte[]> classMap) {
		ClassHierarchy hierarchy = new IncrementalClassHierarchy();
		for (Map.Entry<String, byte[]> e : classMap.entrySet()) {
			try {
				ClassReader reader = new ClassReader(e.getValue());
				String[] ifaceNames = reader.getInterfaces();
				Type clazz = Type.getObjectType(reader.getClassName());
				Type superclass = reader.getSuperName() == null ?
						Type.getObjectType("java/lang/Object") : Type.getObjectType(reader.getSuperName());
				Type[] ifaces = new Type[ifaceNames.length];
				for (int i = 0; i < ifaces.length; i++)
					ifaces[i] = Type.getObjectType(ifaceNames[i]);
				// Add type to hierarchy
				boolean isInterface = (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0;
				if (isInterface) {
					hierarchy.addInterface(clazz, ifaces);
				} else {
					hierarchy.addClass(clazz, superclass, ifaces);
				}
			} catch (Exception ex) {
				Log.error("JPhantom: Hierarchy failure for: {}", e.getKey(), ex);
			}
		}
		return hierarchy;
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
