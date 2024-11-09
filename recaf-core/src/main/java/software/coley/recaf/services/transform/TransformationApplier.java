package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Sets;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies transformations to workspaces.
 *
 * @author Matt Coley
 * @see TransformationManager
 */
@WorkspaceScoped
public class TransformationApplier implements Service {
	public static final String SERVICE_ID = "transformation-applier";
	private static final Logger logger = Logging.get(TransformationApplier.class);
	private final TransformationManager manager;
	private final InheritanceGraph graph;
	private final TransformationApplierConfig config;

	@Inject
	public TransformationApplier(@Nonnull TransformationManager manager, @Nonnull InheritanceGraph graph,
	                             @Nonnull TransformationApplierConfig config) {
		this.manager = manager;
		this.graph = graph;
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Workspace to transform.
	 * @param transformerClasses
	 * 		JVM class transformers to run.
	 *
	 * @return Result container with details about the transformation, including any failures, the transformed classes,
	 * and the option to apply the transformations to the workspace.
	 *
	 * @throws TransformationException
	 * 		When transformation cannot be run for any reason.
	 */
	@Nonnull
	public TransformResult transformJvm(@Nonnull Workspace workspace, @Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses) throws TransformationException {
		return transformJvm(workspace, transformerClasses, null);
	}

	/**
	 * @param workspace
	 * 		Workspace to transform.
	 * @param transformerClasses
	 * 		JVM class transformers to run.
	 * @param predicate
	 * 		Filter to control which JVM classes are transformed.
	 * 		Can be {@code null} to transform all JVM classes.
	 *
	 * @return Result container with details about the transformation, including any failures, the transformed classes,
	 * and the option to apply the transformations to the workspace.
	 *
	 * @throws TransformationException
	 * 		When transformation cannot be run for any reason.
	 */
	@Nonnull
	public TransformResult transformJvm(@Nonnull Workspace workspace, @Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                                    @Nullable JvmClassTransformerPredicate predicate) throws TransformationException {
		// Build transformer visitation order
		TransformerQueue queue = buildQueue(transformerClasses);

		// Map to hold transformation errors for each class:transformer
		Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> transformJvmFailures = new HashMap<>();

		// Build the transformer context and apply all transformations in order
		WorkspaceResource resource = workspace.getPrimaryResource();
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		JvmTransformerContext context = new JvmTransformerContext(workspace, resource, queue.transformers);
		resource.jvmClassBundleStreamRecursive().forEach(bundle -> {
			BundlePathNode bundlePathNode = resourcePath.child(bundle);
			for (JvmClassTransformer transformer : queue.transformers) {
				bundle.forEach(cls -> {
					// Skip if the class does not pass the predicate
					if (predicate != null && !predicate.shouldTransform(workspace, resource, bundle, cls))
						return;

					try {
						transformer.transform(context, workspace, resource, bundle, cls);
					} catch (Throwable t) {
						logger.error("Transformer '{}' failed on class '{}'", transformer.name(), cls.getName(), t);
						ClassPathNode path = bundlePathNode.child(cls.getPackageName()).child(cls);
						var transformerToThrowable = transformJvmFailures.computeIfAbsent(path, p -> new HashMap<>());
						transformerToThrowable.put(transformer.getClass(), t);
					}
				});
			}
		});

		// Update the workspace contents with the transformation results
		Map<ClassPathNode, JvmClassInfo> transformedJvmClasses = context.buildChangeMap(graph);
		return new TransformResult() {
			@Nonnull
			@Override
			public Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> getJvmTransformerFailures() {
				return transformJvmFailures;
			}

			@Nonnull
			@Override
			public Map<ClassPathNode, JvmClassInfo> getJvmTransformedClasses() {
				return transformedJvmClasses;
			}

			@Override
			public void apply() {
				Unchecked.checkedForEach(transformedJvmClasses, (path, cls) -> {
					JvmClassBundle bundle = path.getValueOfType(JvmClassBundle.class);
					if (bundle != null)
						bundle.put(cls);
				}, (path, cls, t) -> logger.error("Exception thrown handling transform application", t));
			}
		};

		// TODO: If we want a transformer which generates and applies mappings, we need a way to facilitate that
		//  - Letting a transformer control mapping applier is bad because that can break the tracking state of class models
		//  - We should probably just track a "mappings to apply" in the transformer context and apply it afterwards
		//    - We can add that 'MappingAppplier' as an argument to the 'apply' method above
	}

	@Nonnull
	private TransformerQueue buildQueue(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses) throws TransformationException {
		TransformerQueue queue = new TransformerQueue();
		for (Class<? extends JvmClassTransformer> transformerClass : transformerClasses)
			insert(queue, transformerClass, Collections.emptySet());
		return queue;
	}

	private void insert(@Nonnull TransformerQueue queue, @Nonnull Class<? extends JvmClassTransformer> transformerClass,
	                    @Nonnull Set<Class<? extends JvmClassTransformer>> dependants) throws TransformationException {
		// Abort if a cycle is detected
		if (dependants.contains(transformerClass))
			throw new TransformationException("Transformer dependency cycle detected with '" + transformerClass.getSimpleName() + "'");

		// Create the transformer and its dependencies
		//  - Dependencies first
		//  - Then the transformer
		JvmClassTransformer transformer = manager.newJvmTransformer(transformerClass);
		for (Class<? extends JvmClassTransformer> dependency : transformer.dependencies())
			if (!queue.containsType(dependency))
				insert(queue, dependency, Sets.add(dependants, transformerClass));
		queue.add(transformer);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public TransformationApplierConfig getServiceConfig() {
		return config;
	}

	/**
	 * Wrapper holding which transformers to run.
	 */
	private static class TransformerQueue {
		private final List<JvmClassTransformer> transformers = new ArrayList<>();
		private final List<Class<? extends JvmClassTransformer>> transformerTypes = new ArrayList<>();

		/**
		 * @param transformer
		 * 		Transformer to add to the queue.
		 */
		private void add(@Nonnull JvmClassTransformer transformer) {
			transformers.add(transformer);
			transformerTypes.add(transformer.getClass());
		}

		/**
		 * @param transformerClass
		 * 		Transformer type to check for,
		 *
		 * @return {@code true} when the queue already has a transformer of that type registered.
		 */
		private boolean containsType(@Nonnull Class<? extends JvmClassTransformer> transformerClass) {
			return transformerTypes.contains(transformerClass);
		}
	}
}
