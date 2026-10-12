package software.coley.recaf.services.phantom;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.phantom.analysis.PhantomClassWriter;
import software.coley.recaf.services.phantom.analysis.PhantomConstraintCollector;
import software.coley.recaf.services.phantom.analysis.PhantomGenerationContext;
import software.coley.recaf.services.phantom.analysis.PhantomHierarchyResolver;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for generating phantom classes from partial program analysis.
 *
 * @author Matt Coley
 */
@ApplicationScoped
@EagerInitialization
public class PhantomGenerator implements Service {
	public static final String SERVICE_ID = "phantom-generator";
	private static final int DEFAULT_TARGET_VERSION = 8;
	private static final Logger logger = Logging.get(PhantomGenerator.class);
	private final PhantomGeneratorConfig config;

	@Inject
	public PhantomGenerator(@Nonnull PhantomGeneratorConfig config, @Nonnull WorkspaceManager workspaceManager) {
		this.config = config;

		workspaceManager.addWorkspaceOpenListener(workspace -> {
			if (!config.getGenerateWorkspacePhantoms().getValue())
				return;
			CompletableFuture.supplyAsync(() -> {
				try {
					return createPhantomsForWorkspace(workspace);
				} catch (Throwable t) {
					logger.warn("Failed to generate phantoms for workspace. Some graphing operations may be slightly less effective.", t);
					return null;
				}
			}).thenAccept(generatedResource -> {
				// Skip adding the generated phantom resource if:
				// - No resource was generated
				// - The generated resource has no classes
				// - The workspace is no longer the current workspace
				if (generatedResource == null
						|| generatedResource.getJvmClassBundle().isEmpty()
						|| !workspaceManager.hasCurrentWorkspace()
						|| workspaceManager.getCurrent() != workspace)
					return;
				installGeneratedResourceIfAbsent(workspace, generatedResource);
			});
		});
	}

	/**
	 * Installs the given generated phantom resource into the workspace if it is not already present.
	 *
	 * @param workspace
	 * 		Workspace to install the resource into.
	 * @param generatedResource
	 * 		Resource to install.
	 */
	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	public static void installGeneratedResourceIfAbsent(@Nonnull Workspace workspace,
	                                                    @Nonnull GeneratedPhantomWorkspaceResource generatedResource) {
		// Workspace-open completions can possibly race on more complex workspaces.
		// Ensure we only add the generated resource once.
		synchronized (workspace) {
			boolean alreadyInstalled = workspace.getSupportingResources().stream()
					.anyMatch(resource -> resource instanceof GeneratedPhantomWorkspaceResource);
			if (!alreadyInstalled)
				workspace.addSupportingResource(generatedResource);
		}
	}

	/**
	 * Generates a resource containing the phantom classes necessary to compile the JVM classes in the
	 * primary resource of the given workspace.
	 * <p>
	 * Do note that this is a largely more computationally complex task than creating phantoms for
	 * {@link #createPhantomsForClasses(Workspace, Collection, int) one or a few classes at a time}.
	 *
	 * @param workspace
	 * 		Workspace to scan for classes with missing references.
	 *
	 * @return Resource containing generated phantoms, targeting missing references across all classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	public GeneratedPhantomWorkspaceResource createPhantomsForWorkspace(@Nonnull Workspace workspace) throws PhantomGenerationException {
		Collection<JvmClassInfo> inputClasses = workspace.getPrimaryResource().jvmAllClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.toList();
		Map<String, JvmClassInfo> classMap = workspace.getPrimaryResource().jvmClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> a, TreeMap::new));
		Map<String, JvmClassInfo> versionedClassMap = workspace.getPrimaryResource().versionedJvmClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> b, TreeMap::new));
		versionedClassMap.forEach(classMap::putIfAbsent);
		return generate(workspace, inputClasses, classMap, DEFAULT_TARGET_VERSION);
	}

	/**
	 * Generates a resource containing phantoms for the given classes at a compiler-compatible class-file version.
	 *
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * 		If a class that is required for compilation of a given class is in the workspace, then no phantom
	 * 		for it will be generated in the resulting created resource.
	 * @param classes
	 * 		Classes to scan for missing references.
	 * @param targetVersion
	 * 		Java version that will consume the generated class files.
	 *
	 * @return Resource containing generated phantoms.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	public GeneratedPhantomWorkspaceResource createPhantomsForClasses(@Nonnull Workspace workspace,
	                                                                  @Nonnull Collection<JvmClassInfo> classes,
	                                                                  int targetVersion)
			throws PhantomGenerationException {
		Collection<JvmClassInfo> inputClasses = classes.stream().toList();
		Map<String, JvmClassInfo> classMap = inputClasses.stream()
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> a, TreeMap::new));
		return generate(workspace, inputClasses, classMap, targetVersion);
	}

	@Nonnull
	private GeneratedPhantomWorkspaceResource generate(@Nonnull Workspace workspace,
	                                                   @Nonnull Collection<JvmClassInfo> inputClasses,
	                                                   @Nonnull Map<String, JvmClassInfo> inputMap,
	                                                   int targetVersion) throws PhantomGenerationException {
		try {
			PhantomGenerationContext context = new PhantomGenerationContext(workspace, inputMap,
					config.getLenientConflictingHierarchies().getValue(), targetVersion);
			Map<String, PhantomClassConstraint> constraints = context.getConstraints();

			PhantomConstraintCollector collector = new PhantomConstraintCollector(context);
			inputClasses.forEach(collector::collect);
			PhantomClassWriter.prepareAnnotationDefaults(constraints, context.getLookup());
			new PhantomHierarchyResolver(context).resolve();

			Map<String, byte[]> generated = new TreeMap<>();
			for (PhantomClassConstraint constraint : constraints.values()) {
				if (!context.isKnown(constraint.getName()))
					generated.put(constraint.getName(), PhantomClassWriter.write(constraint, constraints, targetVersion, context.getLookup()));
			}
			logger.debug("Phantom analysis complete, generated {} classes", generated.size());
			return wrap(generated);
		} catch (Throwable t) {
			throw new PhantomGenerationException(t, "Phantom generation failed");
		}
	}

	/**
	 * @param generated
	 * 		Map of generated classes.
	 *
	 * @return Wrapping resource.
	 */
	@Nonnull
	public static GeneratedPhantomWorkspaceResource wrap(@Nonnull Map<String, byte[]> generated) {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		generated.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.forEach(entry -> bundle.initialPut(new JvmClassInfoBuilder(entry.getValue()).build()));
		bundle.markInitialState();
		return new GeneratedPhantomWorkspaceResource(new WorkspaceResourceBuilder()
				.withJvmClassBundle(bundle));
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public PhantomGeneratorConfig getServiceConfig() {
		return config;
	}
}
