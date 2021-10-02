package me.coley.recaf.util;

import me.coley.recaf.RecafConstants;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.logging.Logging;
import org.clyze.jphantom.ClassMembers;
import org.clyze.jphantom.JPhantom;
import org.clyze.jphantom.Options;
import org.clyze.jphantom.access.ClassAccessStateMachine;
import org.clyze.jphantom.adapters.ClassPhantomExtractor;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.clyze.jphantom.hier.IncrementalClassHierarchy;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility for invoking JPhantom for missing class generation.
 *
 * @author Matt Coley
 */
public class JPhantomUtil {
	private static final Logger logger = Logging.get(JPhantomUtil.class);

	/**
	 * @param inputMap
	 * 		Map to pull classes from.
	 *
	 * @return Generated classes.
	 *
	 * @throws Exception
	 * 		When anything in the JPhantom process failed.
	 */
	public static Map<String, byte[]> generate(Map<String, ClassInfo> inputMap) throws Exception {
		Map<String, byte[]> out = new HashMap<>();
		// Write the parameter passed classes to a temp jar
		Map<String, byte[]> classMap = new HashMap<>();
		Map<Type, ClassNode> nodes = new HashMap<>();
		inputMap.forEach((name, info) -> {
			ClassReader cr = new ClassReader(info.getValue());
			ClassNode node = new ClassNode();
			cr.accept(node, ClassReader.SKIP_FRAMES);
			classMap.put(name + ".class", info.getValue());
			nodes.put(Type.getObjectType(node.name), node);
		});
		// Read into JPhantom
		Options.V().setSoftFail(true);
		Options.V().setJavaVersion(8);
		ClassHierarchy hierarchy = createHierarchy(classMap);
		ClassMembers members = createMembers(classMap, hierarchy);
		classMap.forEach((name, raw) -> {
			if (name.contains("$"))
				return;
			try {
				ClassReader cr = new ClassReader(raw);
				cr.accept(new ClassPhantomExtractor(hierarchy, members), 0);
			} catch (Throwable t) {
				logger.debug("Phantom extraction failed: {}", name, t);
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
		phantom.getGenerated().forEach((k, v) -> out.put(k.getInternalName(), decorate(v)));
		return out;
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
			logger.error("Failed to get initial reader ClassMembers, could not lookup 'java/lang/Object'");
			throw new IllegalStateException();
		}
		for (Map.Entry<String, byte[]> e : classMap.entrySet()) {
			try {
				new ClassReader(e.getValue()).accept(repo.new Feeder(), 0);
			} catch (Throwable t) {
				logger.debug("Could not supply {} to ClassMembers feeder", e.getKey(), t);
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
				logger.error("JPhantom: Hierarchy failure for: {}", e.getKey(), ex);
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
	private static byte[] decorate(byte[] generated) {
		ClassWriter cw = new ClassWriter(0);
		ClassVisitor cv = new ClassVisitor(RecafConstants.ASM_VERSION, cw) {
			@Override
			public void visitEnd() {
				visitAnnotation("LAutoGenerated;", true)
						.visit("msg", "Recaf/JPhantom automatically generated this class");
				super.visitEnd();
			}
		};
		new ClassReader(generated).accept(cv, ClassReader.SKIP_FRAMES);
		return cw.toByteArray();
	}
}
