package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
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
import software.coley.recaf.util.threading.ThreadPoolFactory;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
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
	private final TransformationApplierConfig transformApplyConfig;
	private final InheritanceGraph inheritanceGraph;
	private final MappingApplier mappingApplier;
	private final Workspace workspace;
	private int maxPasses = 1;
	private boolean dropFaultyClasses;

	/**
	 * @param transformationManager
	 * 		Manager to pull transformer instances from.
	 * @param transformApplyConfig
	 * 		Transformation applier config.
	 * @param inheritanceGraph
	 * 		Inheritance graph to use for frame computation <i>(Some transformers will trigger this)</i>.
	 * @param mappingApplier
	 * 		Mapping applier to update workspace with mappings registered by transformers.
	 * @param workspace
	 * 		Workspace with classes to transform.
	 */
	public TransformationApplier(@Nonnull TransformationManager transformationManager,
	                             @Nonnull TransformationApplierConfig transformApplyConfig,
	                             @Nonnull InheritanceGraph inheritanceGraph,
	                             @Nonnull MappingApplier mappingApplier,
	                             @Nonnull Workspace workspace) {
		this.transformationManager = transformationManager;
		this.transformApplyConfig = transformApplyConfig;
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
	 * @param dropFaultyClasses
	 * 		When {@code true}, classes that fail to be written back to bytecode are dropped from the results
	 * 		instead of aborting the run. Forwarded to the {@link JvmTransformerContext} of each run.
	 */
	public void setDropFaultyClasses(boolean dropFaultyClasses) {
		this.dropFaultyClasses = dropFaultyClasses;
	}

	/**
	 * @return {@code true} when faulty classes are dropped instead of aborting the run.
	 */
	public boolean isDropFaultyClasses() {
		return dropFaultyClasses;
	}

	/**
	 * Runs a set of JVM class transformers on the workspace.
	 *
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
		return transformJvm(transformerClasses, TransformationParameters.empty());
	}

	/**
	 * Runs a set of JVM class transformers on the workspace.
	 *
	 * @param transformerClasses
	 * 		JVM class transformers to run.
	 * @param parameters
	 * 		Per-run parameters transformers can pull values from.
	 *
	 * @return Result container with details about the transformation, including any failures, the transformed classes,
	 * and the option to apply the transformations to the workspace.
	 *
	 * @throws TransformationException
	 * 		When transformation cannot be run for any reason.
	 */
	@Nonnull
	public JvmTransformResult transformJvm(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                                       @Nonnull TransformationParameters parameters) throws TransformationException {
		return transformJvm(transformerClasses, parameters, TransformationFeedback.DEFAULT);
	}

	/**
	 * Runs a set of JVM class transformers on the workspace.
	 *
	 * @param transformerClasses
	 * 		JVM class transformers to run.
	 * @param feedback
	 * 		Feedback to report transformation progress to, and control which JVM classes are transformed.
	 *
	 * @return Result container with details about the transformation, including any failures, the transformed classes,
	 * and the option to apply the transformations to the workspace.
	 *
	 * @throws TransformationException
	 * 		When transformation cannot be run for any reason.
	 */
	@Nonnull
	public JvmTransformResult transformJvm(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                                       @Nonnull TransformationFeedback feedback) throws TransformationException {
		return transformJvm(transformerClasses, TransformationParameters.empty(), feedback);
	}

	/**
	 * Runs a set of JVM class transformers on the workspace.
	 *
	 * @param transformerClasses
	 * 		JVM class transformers to run.
	 * @param parameters
	 * 		Per-run parameters transformers can pull values from.
	 * @param feedback
	 * 		Feedback to report transformation progress to, and control which JVM classes are transformed.
	 *
	 * @return Result container with details about the transformation, including any failures, the transformed classes,
	 * and the option to apply the transformations to the workspace.
	 *
	 * @throws TransformationException
	 * 		When transformation cannot be run for any reason.
	 */
	@Nonnull
	public JvmTransformResult transformJvm(@Nonnull List<Class<? extends JvmClassTransformer>> transformerClasses,
	                                       @Nonnull TransformationParameters parameters,
	                                       @Nonnull TransformationFeedback feedback) throws TransformationException {
		// Build transformer visitation order.
		TransformerQueue queue = buildQueue(cast(transformerClasses));
		JvmPhaseRun run = runJvmPhase(queue, parameters, feedback, getMaxPasses());
		feedback.onCompletion();
		return run.result();
	}

	/**
	 * Runs a validated graph of JVM transformation phases.
	 *
	 * @param plan
	 * 		Transformation phase graph to execute.
	 *
	 * @return Aggregate result containing per-phase transformed classes.
	 *
	 * @throws TransformationException
	 * 		When the graph is invalid or a phase cannot be run.
	 */
	@Nonnull
	public TransformationPlanResult transformJvm(@Nonnull TransformationPlan plan) throws TransformationException {
		return transformJvm(plan, TransformationFeedback.DEFAULT);
	}

	/**
	 * Runs a validated graph of JVM transformation phases with feedback.
	 *
	 * @param plan
	 * 		Transformation phase graph to execute.
	 * @param feedback
	 * 		Feedback to report phase and class progress to, and control cancellation.
	 *
	 * @return Aggregate result containing per-phase transformed classes.
	 *
	 * @throws TransformationException
	 * 		When the graph is invalid or a phase cannot be run.
	 */
	@Nonnull
	public TransformationPlanResult transformJvm(@Nonnull TransformationPlan plan,
	                                             @Nonnull TransformationFeedback feedback) throws TransformationException {
		return executePlan(plan, feedback);
	}

	@Nonnull
	private JvmPhaseRun runJvmPhase(@Nonnull TransformerQueue queue,
	                                @Nonnull TransformationParameters parameters,
	                                @Nonnull TransformationFeedback feedback,
	                                int maxPasses) throws TransformationException {
		// Map to hold transformation errors for each class:transformer.
		Map<ClassPathNode, Map<Class<? extends JvmClassTransformer>, Throwable>> transformJvmFailures = Collections.synchronizedMap(new IdentityHashMap<>());

		// Map to hold transformers to the paths of classes they have modified.
		Map<Class<? extends JvmClassTransformer>, Collection<ClassPathNode>> transformerToModifiedClasses = Collections.synchronizedMap(new IdentityHashMap<>());

		// Build the transformer context before setup so every transformer shares the same phase state.
		List<JvmClassTransformer> transformers = queue.getTransformers();
		int initialTransformerCount = transformers.size();
		WorkspaceResource resource = workspace.getPrimaryResource();
		ResourcePathNode resourcePath = PathNodes.resourcePath(workspace, resource);
		JvmTransformerContext context = new JvmTransformerContext(workspace, resource, transformers, parameters);
		context.setDropFaultyClasses(dropFaultyClasses);
		for (JvmClassTransformer transformer : transformers) {
			try {
				transformer.setup(context, workspace);
			} catch (Throwable t) {
				// If setup fails, abort the transformation.
				String message = "Transformer '" + transformer.identifier() + "' failed on setup";
				logger.error(message, t);
				throw new TransformationException(message, t);
			}
		}

		// Collect every class after setup so setup-time workspace changes join the phase snapshot.
		List<JvmClassTarget> targets = new ArrayList<>();
		resource.jvmAllClassBundleStreamRecursive().forEach(bundle -> {
			BundlePathNode bundlePathNode = resourcePath.child(bundle);
			for (JvmClassInfo cls : bundle)
				targets.add(new JvmClassTarget(bundle, cls, bundlePathNode));
		});

		// Run the phase, repeating passes until no work is done or the max pass count is reached.
		int finalPass = 0;
		TransformationPhaseResult.Status status = null;
		try (ExecutorService service = transformApplyConfig.doParallelize().getValue() ?
				ThreadPoolFactory.newFixedThreadPool("transform-apply") :
				ThreadPoolFactory.newSingleThreadExecutor("transform-apply")) {
			for (int pass = 1; pass <= Math.max(1, maxPasses); pass++) {
				finalPass = pass;
				AtomicBoolean anyWorkDone = new AtomicBoolean(false);
				List<JvmClassTransformer> prunedTransformers = new ArrayList<>();
				for (JvmClassTransformer transformer : transformers) {
					AtomicBoolean transformerWorkDone = new AtomicBoolean(false);
					final int currentPass = pass;

					// Transformers run in parallel across every class in the selected resource.
					List<Callable<Void>> tasks = new ArrayList<>(targets.size());
					for (JvmClassTarget target : targets)
						tasks.add(() -> {
							JvmClassBundle bundle = target.bundle();
							JvmClassInfo cls = target.classInfo();
							BundlePathNode bundlePathNode = target.bundlePath();

							// Skip if transformation has been cancelled.
							if (feedback.hasRequestedCancellation())
								return null;

							// Skip if the class does not pass the predicate.
							if (!feedback.shouldTransform(workspace, resource, bundle, cls, transformer, currentPass))
								return null;

							try {
								context.resetTransformerTracking();
								transformer.transform(context, workspace, resource, bundle, cls);
								boolean didWork = context.didTransformerDoWork();
								if (didWork) {
									// Transformer modified this class, record the interaction.
									anyWorkDone.set(true);
									transformerWorkDone.set(true);
									Collection<ClassPathNode> paths = transformerToModifiedClasses.computeIfAbsent(transformer.getClass(),
											t -> Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>())));

									// Only keep one path (since we may have repeated passes)
									synchronized (paths) {
										if (paths.stream().noneMatch(p -> p.getValue().getName().equals(cls.getName()))) {
											ClassPathNode path = bundlePathNode.child(cls.getPackageName()).child(cls);
											paths.add(path);
										}
									}
									feedback.onTransformed(workspace, resource, bundle, cls, transformer, currentPass);
								} else {
									feedback.onTransformedWithoutWork(workspace, resource, bundle, cls, transformer, currentPass);
								}
								logger.debugging(l -> l.debug("Pass {}: Transformer {} didWork={}",
										currentPass, transformer.getClass().getSimpleName(), didWork));
							} catch (Throwable t) {
								logger.error("Transformer '{}' failed on class '{}'", transformer.identifier(), cls.getName(), t);
								feedback.onTransformFailure(workspace, resource, bundle, cls, transformer, currentPass, t);
								ClassPathNode path = bundlePathNode.child(cls.getPackageName()).child(cls);
								var transformerToThrowable = transformJvmFailures.computeIfAbsent(path, p -> Collections.synchronizedMap(new IdentityHashMap<>()));
								transformerToThrowable.put(transformer.getClass(), t);
							}
							return null;
						});

					// Invoke and wait for all classes in the resource to be visited/transformed.
					try {
						service.invokeAll(tasks);
					} catch (InterruptedException ex) {
						throw new RuntimeException("Interrupt", ex);
					}

					// If a transformer is prunable (they no longer execute after a full pass without any work completed)
					// schedule it for removal so that it will not be executed in following passes.
					if (!transformerWorkDone.get() && transformer.pruneAfterNoWork()) {
						logger.debug("Pruning transformer '{}' after pass {} completed with no work done", transformer.identifier(), pass);
						prunedTransformers.add(transformer);
					}
				}

				// Remove transformers pruned after this complete resource pass.
				transformers.removeAll(prunedTransformers);
				if (feedback.hasRequestedCancellation()) {
					status = TransformationPhaseResult.Status.CANCELLED;
					break;
				}

				// Break if this transformer has done no work has been done this pass.
				if (!anyWorkDone.get()) {
					status = TransformationPhaseResult.Status.STABLE;
					break;
				}
			}

			// Update status if it was not set by the loop above.
			// This can happen when the max pass count is reached with work still remaining.
			if (status == null)
				status = feedback.hasRequestedCancellation() ?
						TransformationPhaseResult.Status.CANCELLED :
						TransformationPhaseResult.Status.MAX_PASSES;
		} catch (RuntimeException ex) {
			// Handle the interrupt runtime exception seen a few lines up.
			throw new TransformationException("Unexpected runtime exception", ex);
		}

		// A class failure makes the phase unsuccessful even when other classes completed.
		if (!transformJvmFailures.isEmpty())
			status = TransformationPhaseResult.Status.FAILED;

		// Update the workspace contents with the transformation results.
		Map<ClassPathNode, JvmClassInfo> transformedJvmClasses = context.buildChangeMap(inheritanceGraph);
		logger.debug("Computed transformations with {} transformers, affecting {} classes after {} passes",
				initialTransformerCount, transformedJvmClasses.size(), finalPass);
		Set<ClassPathNode> classesToRemove = context.getClassesToRemove().stream()
				.map(workspace::findJvmClass)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		JvmTransformResult result = new JvmTransformResult() {
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
				return classesToRemove;
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
				// Dump transformed classes into the workspace.
				checkedForEach(transformedJvmClasses, (path, cls) -> {
					JvmClassBundle bundle = path.getValueOfType(JvmClassBundle.class);
					if (bundle != null)
						bundle.put(cls);
				}, (path, cls, t) -> logger.error("Exception thrown handling transform application", t));

				// Delete classes that are marked for removal.
				for (ClassPathNode path : getClassesToRemove()) {
					JvmClassBundle bundle = path.getValueOfType(JvmClassBundle.class);
					if (bundle != null)
						bundle.remove(path.getValue().getName());
				}

				// Apply mappings if they exist.
				IntermediateMappings mappings = context.getMappings();
				if (!mappings.isEmpty()) {
					MappingResults results = mappingApplier.applyToPrimaryResource(mappings);
					results.apply();
				}
			}
		};
		return new JvmPhaseRun(result, finalPass, status);
	}

	@Nonnull
	private TransformationPlanResult executePlan(@Nonnull TransformationPlan plan,
	                                             @Nonnull TransformationFeedback feedback) throws TransformationException {
		// Validate all phases and reserve fresh transformer instances before visiting class nodes.
		List<PreparedPhase> preparedPhases = preparePlan(plan);
		List<TransformationPhaseResult> phaseResults = new ArrayList<>(preparedPhases.size());
		try {
			for (int index = 0; index < preparedPhases.size(); index++) {
				// Notify feedback of phase start.
				PreparedPhase prepared = preparedPhases.get(index);
				TransformationPhase phase = prepared.phase();
				feedback.onPhaseStart(phase, index + 1, preparedPhases.size());

				// Complete the phase with cancelled status if requested.
				if (feedback.hasRequestedCancellation()) {
					phaseResults.add(new TransformationPhaseResult(phase, null, TransformationPhaseResult.Status.CANCELLED, 0));
					feedback.onPhaseComplete(phaseResults.getLast());
					break;
				}

				// Run the phase.
				JvmPhaseRun phaseRun = runJvmPhase(prepared.queue(), phase.parameters(), feedback, phase.maxPasses());

				// Log the result of the phase run.
				TransformationPhaseResult phaseResult = new TransformationPhaseResult(phase, phaseRun.result(), phaseRun.status(), phaseRun.passes());
				phaseResults.add(phaseResult);
				feedback.onPhaseComplete(phaseResult);

				// If a phase fails, we bail out.
				if (!phaseRun.status().isSuccess())
					break;

				// For multiphase plans, we apply the result of each phase to the workspace so that the next phase sees the changes.
				// The last phase is not applied here, as the caller of this method is responsible for applying the final phase.
				if (index + 1 < preparedPhases.size())
					// TODO: It would be ideal to not apply results, but still have following phases able to see the changes.
					//  - Do we want to create a sort of 'layered view' of a workspace that adds the transformations on top of the original workspace?
					//  - Problem is PathNode usage... It could get weird if working off of the layer Workspace implementation.
					//    - There is also reference equality checks for workspace content in a few places that would break if we did that.
					phaseRun.result().apply();
			}

			// Phases after the one that stopped execution never run.
			while (phaseResults.size() < preparedPhases.size()) {
				TransformationPhase skippedPhase = preparedPhases.get(phaseResults.size()).phase();
				TransformationPhaseResult skipped = new TransformationPhaseResult(skippedPhase, null, TransformationPhaseResult.Status.SKIPPED, 0);
				phaseResults.add(skipped);
				feedback.onPhaseComplete(skipped);
			}
		} finally {
			feedback.onCompletion();
		}
		return new TransformationPlanResult(phaseResults);
	}

	@Nonnull
	private List<PreparedPhase> preparePlan(@Nonnull TransformationPlan plan) throws TransformationException {
		List<TransformationPhase> phases = plan.phases();

		// Validate identifiers and transformer registration before instantiating anything.
		Map<String, TransformationPhase> byId = new HashMap<>();
		for (TransformationPhase phase : phases) {
			if (byId.putIfAbsent(phase.id(), phase) != null)
				throw new TransformationException("Duplicate transformation phase id: " + phase.id());
			for (Class<? extends JvmClassTransformer> transformerClass : phase.jvmTransformers()) {
				if (!transformationManager.getJvmClassTransformers().contains(transformerClass))
					throw new TransformationException("Transformer is not registered: " + transformerClass.getName());
				if (Collections.frequency(phase.jvmTransformers(), transformerClass) > 1)
					throw new TransformationException("Phase '" + phase.id() + "' contains duplicate transformers");
			}
		}

		// A dependency must not be explicitly owned by a later phase, or it would run twice
		// (once auto-included in this phase's queue, once in the later phase's queue).
		Map<Class<? extends ClassTransformer>, Integer> ownerIndex = new HashMap<>();
		for (int i = 0; i < phases.size(); i++)
			for (Class<? extends JvmClassTransformer> transformerClass : phases.get(i).jvmTransformers())
				ownerIndex.put(transformerClass, i);

		List<PreparedPhase> prepared = new ArrayList<>(phases.size());
		for (int i = 0; i < phases.size(); i++) {
			TransformationPhase phase = phases.get(i);

			// Build dependency instances now so failures occur before class nodes are visited.
			TransformerQueue queue = buildQueue(cast(phase.jvmTransformers()));
			for (JvmClassTransformer transformer : queue.<JvmClassTransformer>getTransformers())
				for (Class<? extends ClassTransformer> dependency : transformer.dependencies()) {
					Integer owner = ownerIndex.get(dependency);
					if (owner != null && owner > i)
						throw new TransformationException("Transformer '" + transformer.identifier() + "' in phase '"
								+ phase.id() + "' depends on transformer owned by a later phase: " + dependency.getName());
				}
			prepared.add(new PreparedPhase(phase, queue));
		}
		return List.copyOf(prepared);
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
	 * Sorts transformers by recommended order.
	 *
	 * @param transformers
	 * 		Transformers to order.
	 * @param <T>
	 * 		Transformer type.
	 *
	 * @return Reordered transformers.
	 */
	@Nonnull
	public static <T extends ClassTransformer> List<T> sortRecommended(@Nonnull List<T> transformers) {
		int n = transformers.size();
		if (n <= 1)
			return transformers;

		// Key by type for quickj lookups.
		Map<Class<? extends ClassTransformer>, Integer> indexByClass = new HashMap<>();
		for (int i = 0; i < n; i++)
			indexByClass.put(transformers.get(i).getClass(), i);

		// Build graph of recommended predecessors and successors.
		Map<Class<? extends ClassTransformer>, List<Class<? extends ClassTransformer>>> edges = new HashMap<>();
		Map<Class<? extends ClassTransformer>, Integer> inDegree = new HashMap<>();
		for (T transformer : transformers) {
			// Initialize the graph with all nodes, even if they have no edges.
			Class<? extends ClassTransformer> type = transformer.getClass();
			edges.put(type, new ArrayList<>());
			inDegree.put(type, 0);
		}
		for (T transformer : transformers) {
			// Add edges for recommended predecessors and successors, but only if the other transformer is in the list.
			Class<? extends ClassTransformer> type = transformer.getClass();
			for (Class<? extends ClassTransformer> pre : transformer.recommendedPredecessors()) {
				if (!indexByClass.containsKey(pre))
					continue;
				edges.get(pre).add(type);
				inDegree.merge(type, 1, Integer::sum);
			}
			for (Class<? extends ClassTransformer> suc : transformer.recommendedSuccessors()) {
				if (!indexByClass.containsKey(suc))
					continue;
				edges.get(type).add(suc);
				inDegree.merge(suc, 1, Integer::sum);
			}
		}

		// Setup queue of ready nodes, sorted by their original index in the list.
		PriorityQueue<Class<? extends ClassTransformer>> ready =
				new PriorityQueue<>(Comparator.comparingInt(indexByClass::get));
		for (T transformer : transformers)
			if (inDegree.get(transformer.getClass()) == 0)
				ready.add(transformer.getClass());

		// Perform a topological sort of the graph, using the original list order as a tie-breaker.
		List<Class<? extends ClassTransformer>> sorted = new ArrayList<>(n);
		while (!ready.isEmpty()) {
			Class<? extends ClassTransformer> type = ready.poll();
			sorted.add(type);
			for (Class<? extends ClassTransformer> next : edges.getOrDefault(type, List.of())) {
				int degree = inDegree.merge(next, -1, Integer::sum);
				if (degree == 0)
					ready.add(next);
			}
		}

		// If we have a cycle that cannot be resolved, we will just append the remaining nodes in their current relative order.
		if (sorted.size() < n) {
			Set<Class<? extends ClassTransformer>> sortedSet = new HashSet<>(sorted);
			for (T transformer : transformers)
				if (!sortedSet.contains(transformer.getClass()))
					sorted.add(transformer.getClass());
		}

		// Map the sorted types back to the original transformer instances.
		Map<Class<? extends ClassTransformer>, T> byClass = new HashMap<>();
		for (T transformer : transformers)
			byClass.put(transformer.getClass(), transformer);
		return sorted.stream().map(byClass::get).toList();
	}

	/**
	 * Result metadata produced by one low-level phase run.
	 *
	 * @param result
	 * 		Low-level transformation result.
	 * @param passes
	 * 		Number of passes executed.
	 * @param status
	 * 		Outcome of the run.
	 */
	private record JvmPhaseRun(@Nonnull JvmTransformResult result,
	                           int passes,
	                           @Nonnull TransformationPhaseResult.Status status) {}

	/**
	 * Prepared phase and the fresh transformer instances reserved for it.
	 *
	 * @param phase
	 * 		Phase specification.
	 * @param queue
	 * 		Transformer queue for the phase.
	 */
	private record PreparedPhase(@Nonnull TransformationPhase phase,
	                             @Nonnull TransformerQueue queue) {}

	/**
	 * Class target visited by a resource-wide transformer pass.
	 *
	 * @param bundle
	 * 		Bundle containing the class.
	 * @param classInfo
	 * 		Class state at the start of the phase.
	 * @param bundlePath
	 * 		Path to the containing bundle.
	 */
	private record JvmClassTarget(@Nonnull JvmClassBundle bundle,
	                              @Nonnull JvmClassInfo classInfo,
	                              @Nonnull BundlePathNode bundlePath) {}

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
