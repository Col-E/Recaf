package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.util.ReflectUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * An implementation of {@link PhantomGenerator} using {@link JPhantom}.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class JPhantomGenerator implements PhantomGenerator {
	public static final String SERVICE_ID = "jphantom-generator";
	private static final Logger logger = Logging.get(JPhantomGenerator.class);
	private final JPhantomGeneratorConfig config;

	@Inject
	public JPhantomGenerator(@Nonnull JPhantomGeneratorConfig config) {
		this.config = config;
	}

	@Nonnull
	@Override
	public WorkspaceResource createPhantomsForWorkspace(@Nonnull Workspace workspace) throws PhantomGenerationException {
		// Extract all JVM classes from workspace
		Map<String, JvmClassInfo> classMap = workspace.getPrimaryResource().jvmClassBundleStream()
				.flatMap(Bundle::stream)
				.collect(Collectors.toMap(Info::getName, Function.identity()));

		// Generate phantoms for them and wrap into resource
		try {
			Map<String, byte[]> generated = generate(workspace, classMap);
			return wrap(generated);
		} catch (IOException ex) {
			throw new PhantomGenerationException(ex, "JPhantom encountered a problem");
		}
	}

	@Nonnull
	@Override
	public WorkspaceResource createPhantomsForClasses(@Nonnull Workspace workspace, @Nonnull Collection<JvmClassInfo> classes)
			throws PhantomGenerationException {
		// Convert collection to map
		Map<String, JvmClassInfo> classMap = classes.stream()
				.collect(Collectors.toMap(Info::getName, Function.identity()));

		// Generate phantoms for them and wrap into resource
		try {
			Map<String, byte[]> generated = generate(workspace, classMap);
			return wrap(generated);
		} catch (IOException ex) {
			throw new PhantomGenerationException(ex, "JPhantom encountered a problem");
		}
	}

	/**
	 * @param generated
	 * 		Map of generated classes.
	 *
	 * @return Wrapping resource.
	 */
	@Nonnull
	public static WorkspaceResource wrap(@Nonnull Map<String, byte[]> generated) {
		// Wrap into resource
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		generated.forEach((name, phantom) -> {
			JvmClassInfo phantomClassInfo = new JvmClassInfoBuilder(new ClassReader(phantom)).build();
			bundle.initialPut(phantomClassInfo);
		});
		return new WorkspaceResourceBuilder()
				.withJvmClassBundle(bundle)
				.build();
	}

	/**
	 * @param workspace
	 * 		Workspace to check for class existence within.
	 * @param inputMap
	 * 		Input map of classes to create phantoms for.
	 *
	 * @return Map of phantom classes.
	 *
	 * @throws IOException
	 * 		When {@link JPhantom#run()} fails.
	 */
	@Nonnull
	public static Map<String, byte[]> generate(@Nonnull Workspace workspace,
											   @Nonnull Map<String, JvmClassInfo> inputMap) throws IOException {
		Map<String, byte[]> out = new HashMap<>();

		// Write the parameter passed classes to a temp jar
		Map<String, byte[]> classMap = new HashMap<>();
		Map<Type, ClassNode> nodes = new HashMap<>();
		inputMap.forEach((name, info) -> {
			ClassReader cr = info.getClassReader();
			ClassNode node = new ClassNode();
			cr.accept(node, ClassReader.SKIP_FRAMES);
			classMap.put(name + ".class", info.getBytecode());
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
		ClassAccessStateMachine.v().getConstraints().removeIf(c -> !existingConstraints.add(c.toString()));

		// Execute and populate the current resource with generated classes
		try {
			JPhantom phantom = new JPhantom(nodes, hierarchy, members);
			phantom.run();
			phantom.getGenerated().forEach((k, v) -> {
				// Only put items not found in the workspace.
				// We may call the generator on a small scope, and thus create phantoms of classes that
				// exist in the workspace, but were not in the provided scope.
				String name = k.getInternalName();
				if (workspace.findJvmClass(name) == null)
					out.put(name, decorate(v));
			});
			logger.debug("Phantom analysis complete, generated {} classes", out.size());
		} finally {
			// Cleanup
			Phantoms.refresh();
			Phantoms.V().getLookupTable().clear();
			ClassAccessStateMachine.refresh();
			FieldAccessStateMachine.refresh();
			MethodAccessStateMachine.refresh();
		}
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
	@Nonnull
	public static ClassMembers createMembers(@Nonnull Map<String, byte[]> classMap, @Nonnull ClassHierarchy hierarchy) {
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
	@Nonnull
	public static ClassHierarchy createHierarchy(@Nonnull Map<String, byte[]> classMap) {
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
	 * @return Modified class that clearly indicates it is generated.
	 */
	@Nonnull
	private static byte[] decorate(@Nonnull byte[] generated) {
		ClassReader classReader = new ClassReader(generated);
		ClassWriter cw = new ClassWriter(classReader, 0);
		ClassVisitor cv = new ClassVisitor(RecafConstants.getAsmVersion(), cw) {
			@Override
			public void visitEnd() {
				visitAnnotation("LAutoGenerated;", true)
						.visit("msg", "Recaf/JPhantom automatically generated this class");
				super.visitEnd();
			}
		};
		classReader.accept(cv, ClassReader.SKIP_FRAMES);
		return cw.toByteArray();
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public JPhantomGeneratorConfig getServiceConfig() {
		return config;
	}
}
