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
import software.coley.recaf.services.phantom.analysis.PhantomConstraintCollector;
import software.coley.recaf.services.phantom.analysis.PhantomClassWriter;
import software.coley.recaf.services.phantom.analysis.PhantomGenerationContext;
import software.coley.recaf.services.phantom.analysis.PhantomHierarchyResolver;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
				if (generatedResource != null)
					workspace.addSupportingResource(generatedResource);
			});
		});
	}

	/**
	 * Generates a resource containing the phantom classes necessary to compile the JVM classes in the
	 * primary resource of the given workspace.
	 * <p>
	 * Do note that this is a largely more computationally complex task than creating phantoms for
	 * {@link #createPhantomsForClasses(Workspace, Collection) one or a few classes at a time}.
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
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> a, HashMap::new));
		Map<String, JvmClassInfo> versionedClassMap = workspace.getPrimaryResource().versionedJvmClassBundleStreamRecursive()
				.flatMap(Bundle::stream)
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> b, HashMap::new));
		versionedClassMap.forEach(classMap::putIfAbsent);
		return generate(workspace, inputClasses, classMap);
	}

	/**
	 * Generates a resource containing the phantom classes necessary to compile the given classes.
	 *
	 * @param workspace
	 * 		Workspace to pull class information from.
	 * 		If a class that is required for compilation of a given class is in the workspace, then no phantom
	 * 		for it will be generated in the resulting created resource.
	 * @param classes
	 * 		Classes to scan for missing references.
	 *
	 * @return Resource containing generated phantoms, targeting only the specified classes.
	 *
	 * @throws PhantomGenerationException
	 * 		When generating phantoms failed.
	 */
	@Nonnull
	public GeneratedPhantomWorkspaceResource createPhantomsForClasses(@Nonnull Workspace workspace,
	                                                                  @Nonnull Collection<JvmClassInfo> classes)
			throws PhantomGenerationException {
		Collection<JvmClassInfo> inputClasses = classes.stream().toList();
		Map<String, JvmClassInfo> classMap = inputClasses.stream()
				.collect(Collectors.toMap(Info::getName, Function.identity(), (a, b) -> a, HashMap::new));
		return generate(workspace, inputClasses, classMap);
	}

	@Nonnull
	private GeneratedPhantomWorkspaceResource generate(@Nonnull Workspace workspace,
	                                                   @Nonnull Collection<JvmClassInfo> inputClasses,
	                                                   @Nonnull Map<String, JvmClassInfo> inputMap) throws PhantomGenerationException {
		try {
			PhantomGenerationContext context = new PhantomGenerationContext(workspace, inputMap,
					config.getLenientConflictingHierarchies().getValue());
			Map<String, PhantomClassConstraint> constraints = context.getConstraints();

			PhantomConstraintCollector collector = new PhantomConstraintCollector(context);
			inputClasses.forEach(collector::collect);
			new PhantomHierarchyResolver(context).resolve();

			Map<String, byte[]> generated = new HashMap<>();
			for (PhantomClassConstraint constraint : constraints.values()) {
				if (!context.isKnown(constraint.getName()))
					generated.put(constraint.getName(), PhantomClassWriter.write(constraint, constraints));
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
		generated.forEach((name, phantom) -> bundle.initialPut(new JvmClassInfoBuilder(phantom).build()));
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
