package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.collections.Sets;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.mapping.IntermediateMappings;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static software.coley.collections.Unchecked.cast;
import static software.coley.collections.Unchecked.checkedForEach;

/**
 * Applies transformations to workspaces.
 *
 * @author Matt Coley
 * @see TransformationManager
 */
public class TransformationApplier {
	private static final DebuggingLogger logger = Logging.get(TransformationApplier.class);
	private final TransformationManager transformationManager;
	private final InheritanceGraph inheritanceGraph;
	private final MappingApplier mappingApplier;
	private final Workspace workspace;
	private int maxPasses = 1;

	/**
	 * @param transformationManager
	 * 		Manager to pull transformer instances from.
	 * @param inheritanceGraph
	 * 		Inheritance graph to use for frame computation <i>(Some transformers will trigger this)</i>.
	 * @param mappingApplier
	 * 		Mapping applier to update workspace with mappings registered by transformers.
	 * @param workspace
	 * 		Workspace with classes to transform.
	 */
	public TransformationApplier(@Nonnull TransformationManager transformationManager,
	                             @Nonnull InheritanceGraph inheritanceGraph,
	                             @Nonnull MappingApplier mappingApplier,
	                             @Nonnull Workspace workspace) {
		this.transformationManager = transformationManager;
		this.inheritanceGraph = inheritanceGraph;
		this.mappingApplier = mappingApplier;
		this.workspace = workspace;
	}

	/**
	 * @return Maximum number of times to repeat transformations.
	 */
	public int getMaxPasses() {
		return Math.max(1, maxPasses);
	}

	/**
	 * @param maxPasses
	 * 		Maximum number of times to repeat transformations
	 */
	public void setMaxPasses(int maxPasses) {
		this.maxPasses = maxPasses;
	}

	/**
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
	public JvmTransformResult transformJvm(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses) throws TransformationException {
		return transformJvm(transformerClasses, null);
	}

	/**
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
	public JvmTransformResult transformJvm(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                                       @Nullable JvmClassTransformerPredicate predicate) throws TransformationException {
		// Build transformer visitation order
		TransformerQueue queue = buildQueue(cast(transformerClasses));

		// Map to hold transformation errors for each class:transformer
		Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> transformJvmFailures = new IdentityHashMap<>();

		// Map to hold transformers to the paths of classes they have modified.
		Map<Class<? extends JvmClassTransformer>, Collection<ClassPathNode>> transformerToModifiedClasses = new IdentityHashMap<>();

		// Build the transformer context and apply all transformations in order
		List<JvmClassTransformer> transformers = queue.getTransformers();
		WorkspaceResource resource = workspace.getPrimaryResource();
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		JvmTransformerContext context = new JvmTransformerContext(workspace, resource, transformers);
		for (JvmClassTransformer transformer : transformers) {
			try {
				transformer.setup(context, workspace);
			} catch (Throwable t) {
				// If setup fails, abort the transformation
				String message = "Transformer '" + transformer.name() + "' failed on setup";
				logger.error(message, t);
				throw new TransformationException(message, t);
			}
		}
		resource.jvmClassBundleStreamRecursive().forEach(bundle -> {
			BundlePathNode bundlePathNode = resourcePath.child(bundle);
			for (int pass = 1; pass <= getMaxPasses(); pass++) {
				AtomicBoolean anyWorkDone = new AtomicBoolean(false);
				for (JvmClassTransformer transformer : transformers) {
					final int currentPass = pass;
					bundle.forEach(cls -> {
						// Skip if the class does not pass the predicate
						if (predicate != null && !predicate.shouldTransform(workspace, resource, bundle, cls))
							return;

						try {
							context.resetTransformerTracking();
							transformer.transform(context, workspace, resource, bundle, cls);
							if (context.didTransformerDoWork()) {
								// Transformer modified this class, record the interaction
								anyWorkDone.set(true);
								Collection<ClassPathNode> paths = transformerToModifiedClasses.computeIfAbsent(transformer.getClass(),
										t -> Collections.newSetFromMap(new IdentityHashMap<>()));

								// Only keep one path (since we may have repeated passes)
								if (paths.stream().noneMatch(p -> p.getValue().getName().equals(cls.getName()))) {
									ClassPathNode path = bundlePathNode.child(cls.getPackageName()).child(cls);
									paths.add(path);
								}
							}
							logger.debugging(l -> l.debug("Pass {}: Transformer {} didWork={}",
									currentPass, transformer.getClass().getSimpleName(), context.didTransformerDoWork()));
						} catch (Throwable t) {
							logger.error("Transformer '{}' failed on class '{}'", transformer.name(), cls.getName(), t);
							ClassPathNode path = bundlePathNode.child(cls.getPackageName()).child(cls);
							var transformerToThrowable = transformJvmFailures.computeIfAbsent(path, p -> new IdentityHashMap<>());
							transformerToThrowable.put(transformer.getClass(), t);
						}
					});
				}

				// Break if no work has been done this pass.
				if (!anyWorkDone.get())
					break;
			}
		});

		// Update the workspace contents with the transformation results
		Map<ClassPathNode, JvmClassInfo> transformedJvmClasses = context.buildChangeMap(inheritanceGraph);
		return new JvmTransformResult() {
			@Nonnull
			@Override
			public Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> getTransformerFailures() {
				return transformJvmFailures;
			}

			@Nonnull
			@Override
			public Map<ClassPathNode, JvmClassInfo> getTransformedClasses() {
				return transformedJvmClasses;
			}

			@Nonnull
			@Override
			public Set<ClassPathNode> getClassesToRemove() {
				return context.getClassesToRemove().stream()
						.map(workspace::findJvmClass)
						.collect(Collectors.toSet());
			}

			@Nonnull
			@Override
			public IntermediateMappings getMappingsToApply() {
				return context.getMappings();
			}

			@Nonnull
			@Override
			public Map<Class<? extends JvmClassTransformer>, Collection<ClassPathNode>> getModifiedClassesPerTransformer() {
				return transformerToModifiedClasses;
			}

			@Override
			public void apply() {
				// Dump transformed classes into the workspace
				checkedForEach(transformedJvmClasses, (path, cls) -> {
					JvmClassBundle bundle = path.getValueOfType(JvmClassBundle.class);
					if (bundle != null)
						bundle.put(cls);
				}, (path, cls, t) -> logger.error("Exception thrown handling transform application", t));

				// Delete classes that are marked for removal
				for (ClassPathNode path : getClassesToRemove()) {
					JvmClassBundle bundle = path.getValueOfType(JvmClassBundle.class);
					if (bundle != null)
						bundle.remove(path.getValue().getName());
				}

				// Apply mappings if they exist
				IntermediateMappings mappings = context.getMappings();
				if (!mappings.isEmpty()) {
					MappingResults results = mappingApplier.applyToPrimaryResource(mappings);
					results.apply();
				}
			}
		};
	}

	@Nonnull
	private TransformerQueue buildQueue(@Nonnull List<Class<? extends ClassTransformer>> transformerClasses) throws TransformationException {
		TransformerQueue queue = new TransformerQueue();
		for (Class<? extends ClassTransformer> transformerClass : transformerClasses)
			insert(queue, transformerClass, Collections.emptySet());
		return queue;
	}

	private void insert(@Nonnull TransformerQueue queue, @Nonnull Class<? extends ClassTransformer> transformerClass,
	                    @Nonnull Set<Class<? extends ClassTransformer>> dependants) throws TransformationException {
		// Abort if a cycle is detected
		if (dependants.contains(transformerClass))
			throw new TransformationException("Transformer dependency cycle detected with '" + transformerClass.getSimpleName() + "'");

		// Create the transformer and its dependencies
		//  - Dependencies first
		//  - Then the transformer
		ClassTransformer transformer;
		if (JvmClassTransformer.class.isAssignableFrom(transformerClass)) {
			Class<? extends JvmClassTransformer> jvmTransformerClass = cast(transformerClass);
			transformer = transformationManager.newJvmTransformer(jvmTransformerClass);
		} else {
			throw new TransformationException("Unsupported transformer class type: " + transformerClass);
		}
		for (Class<? extends ClassTransformer> dependency : transformer.dependencies())
			if (!queue.containsType(dependency))
				insert(queue, dependency, Sets.add(dependants, transformerClass));
		queue.add(transformer);
	}

	/**
	 * Wrapper holding which transformers to run.
	 */
	private static class TransformerQueue {
		private final List<ClassTransformer> transformers = new ArrayList<>();
		private final List<Class<? extends ClassTransformer>> transformerTypes = new ArrayList<>();

		/**
		 * @param transformer
		 * 		Transformer to add to the queue.
		 */
		private void add(@Nonnull ClassTransformer transformer) {
			transformers.add(transformer);
			transformerTypes.add(transformer.getClass());
		}

		/**
		 * @param transformerClass
		 * 		Transformer type to check for,
		 *
		 * @return {@code true} when the queue already has a transformer of that type registered.
		 */
		private boolean containsType(@Nonnull Class<? extends ClassTransformer> transformerClass) {
			return transformerTypes.contains(transformerClass);
		}

		/**
		 * @param <T>
		 * 		Inferred transformer type.
		 *
		 * @return List of registered transformers.
		 */
		@Nonnull
		private <T extends ClassTransformer> List<T> getTransformers() {
			return cast(transformers);
		}
	}
}
