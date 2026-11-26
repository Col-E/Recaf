package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.services.mapping.MappingApplier;
import software.coley.recaf.services.mapping.MappingApplierService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TransformationApplier}
 */
class TransformationApplierTest extends TestBase {
	private static final String HELLO_WORLD = "software/coley/recaf/test/dummy/HelloWorld";
	private static final TransformationApplierConfig config = new TransformationApplierConfig();
	private static final InheritanceGraph inheritanceGraph;
	private static final MappingApplier mappingApplier;
	private static final Workspace workspace;

	static {
		// Make a dummy workspace. We just need a single class (and any class will work)
		try {
			workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(HelloWorld.class));
			inheritanceGraph = recaf.get(InheritanceGraphService.class).newInheritanceGraph(workspace);
			mappingApplier = recaf.get(MappingApplierService.class).inWorkspace(workspace);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read input class for transformer test", e);
		}
	}

	@Test
	void independentAB() {
		JvmTransformerA transformerA = spy(new JvmTransformerA());
		JvmTransformerB transformerB = spy(new JvmTransformerB());

		// Build transformer map with two items
		//  - A
		//  - B
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(JvmTransformerA.class, () -> transformerA);
		map.put(JvmTransformerB.class, () -> transformerB);

		// If we transform with "B" we should observe that only "B" is called on sine the two hold no relation
		TransformationManager manager = new TransformationManager(map);
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);
		assertDoesNotThrow(() -> applier.transformJvm(Collections.singletonList(JvmTransformerB.class)));

		// "A" not used
		verify(transformerA, never()).transform(any(), any(), any(), any(), any());

		// "B" used once
		verify(transformerB, times(1)).transform(any(), same(workspace), any(), any(), any());
	}

	@Test
	void dependentAB() {
		JvmTransformerA transformerA = spy(new JvmTransformerA());
		JvmTransformerDependingOnA transformerB = spy(new JvmTransformerDependingOnA());

		// Build transformer map with two items
		//  - A
		//  - B --> A
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(JvmTransformerA.class, () -> transformerA);
		map.put(JvmTransformerDependingOnA.class, () -> transformerB);

		// If we transform with "B" we should observe that both "B" and "A" were called on.
		TransformationManager manager = new TransformationManager(map);
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);
		assertDoesNotThrow(() -> applier.transformJvm(Collections.singletonList(JvmTransformerDependingOnA.class)));
		verify(transformerA, times(1)).transform(any(), same(workspace), any(), any(), any());
		verify(transformerB, times(1)).transform(any(), same(workspace), any(), any(), any());
	}

	@Test
	void cycleAB() {
		JvmCycleA transformerA = spy(new JvmCycleA());
		JvmCycleB transformerB = spy(new JvmCycleB());

		// Build transformer map with two items
		//  - A --> B
		//  - B --> A
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(JvmCycleA.class, () -> transformerA);
		map.put(JvmCycleB.class, () -> transformerB);

		// If we transform with "A" or "B" we should observe an exception due to the detected cycle
		TransformationManager manager = new TransformationManager(map);
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleA.class)));
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleB.class)));
		verify(transformerA, never()).transform(any(), same(workspace), any(), any(), any());
		verify(transformerB, never()).transform(any(), same(workspace), any(), any(), any());
	}

	@Test
	void cycleSingle() {
		JvmCycleSingle transformer = spy(new JvmCycleSingle());

		// Build transformer map with one item
		//  - A --> A
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(JvmCycleSingle.class, () -> transformer);

		// If we transform with the single transformer we should observe an exception due to the detected cycle
		TransformationManager manager = new TransformationManager(map);
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleSingle.class)));
		verify(transformer, never()).transform(any(), same(workspace), any(), any(), any());
	}

	@Test
	void missingRegistration() {
		// If we transform with a transformer that is not registered in the manager, the transform should fail
		TransformationManager manager = new TransformationManager(Collections.emptyMap());
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleSingle.class)));
	}

	@Test
	void dropBrokenClassWrites() {
		Class<FrameBreakingTransformer> transformerClass = FrameBreakingTransformer.class;
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(transformerClass, () -> {
			try {
				return transformerClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Failed to instantiate transformer", e);
			}
		});
		TransformationManager manager = new TransformationManager(map);
		TransformationApplier applier = new TransformationApplier(manager, config, inheritanceGraph, mappingApplier, workspace);

		// If a class is malformed when we try to write back the transformed class, the default behavior is to abort the run.
		assertFalse(applier.isDropFaultyClasses());
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(transformerClass)));

		// We can configure the applier to drop broken classes instead of aborting the run.
		applier.setDropFaultyClasses(true);

		// The run will pass, but the broken class will be dropped from the result.
		// Since it's the only class in the workspace, the result will be empty.
		JvmTransformResult result = assertDoesNotThrow(() -> applier.transformJvm(Collections.singletonList(transformerClass)));
		assertTrue(result.getTransformedClasses().isEmpty());
	}

	@Test
	void phasedExecution() throws Exception {
		Workspace phaseWorkspace = freshWorkspace();
		AtomicBoolean phaseTwoSawPhaseOne = new AtomicBoolean();

		// Build transformer map with two items
		//  - PhaseOneMarkerTransformer: adds a field to the class
		//  - PhaseTwoMarkerTransformer: records whether the phase-one field is visible, then adds its own
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(PhaseOneMarkerTransformer.class, PhaseOneMarkerTransformer::new);
		map.put(PhaseTwoMarkerTransformer.class, () -> new PhaseTwoMarkerTransformer(phaseTwoSawPhaseOne));
		TransformationApplier applier = newApplier(phaseWorkspace, new TransformationManager(map));

		// Collect phase lifecycle events so the execution order can be asserted below.
		List<String> events = Collections.synchronizedList(new ArrayList<>());
		TransformationFeedback feedback = new TransformationFeedback() {
			@Override
			public void onPhaseStart(@Nonnull TransformationPhase phase, int phaseIndex, int phaseCount) {
				events.add("start:" + phase.id());
			}

			@Override
			public void onPhaseComplete(@Nonnull TransformationPhaseResult result) {
				events.add("complete:" + result.phase().id());
			}

			@Override
			public void onCompletion() {
				events.add("completion");
			}
		};

		// If we run a plan with "phase-two" after "phase-one"...
		TransformationPhase first = new TransformationPhase("phase-one", 1, PhaseOneMarkerTransformer.class);
		TransformationPhase second = new TransformationPhase("phase-two", 1, PhaseTwoMarkerTransformer.class);
		TransformationPlan plan = new TransformationPlan(List.of(first, second));
		TransformationPlanResult result = applier.transformJvm(plan, feedback);

		// ...the plan completes, phases run in declaration order, and phase two observed the applied phase one.
		assertTrue(result.isComplete());
		assertTrue(phaseTwoSawPhaseOne.get(), "The dependent phase must observe the applied prerequisite");
		assertEquals(List.of(
				"start:phase-one",
				"complete:phase-one",
				"start:phase-two",
				"complete:phase-two",
				"completion"), events);

		// The final phase is not applied yet, so the workspace has the phase-one field only.
		JvmClassInfo afterFirst = currentHelloWorld(phaseWorkspace);
		assertNotNull(afterFirst.getDeclaredField(PhaseOneMarkerTransformer.FIELD_NAME, "I"));
		assertNull(afterFirst.getDeclaredField(PhaseTwoMarkerTransformer.FIELD_NAME, "I"));

		// Applying the plan commits the final phase.
		result.apply();
		JvmClassInfo finalClass = currentHelloWorld(phaseWorkspace);
		assertNotNull(finalClass.getDeclaredField(PhaseTwoMarkerTransformer.FIELD_NAME, "I"));
	}

	@Test
	void phasedExecutionWithFreshInstances() throws Exception {
		Workspace phaseWorkspace = freshWorkspace();
		ConvergingTransformer.INSTANCES.clear();

		// Build transformer map with one item
		//  - ConvergingTransformer: does work on its first transform call only
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(ConvergingTransformer.class, ConvergingTransformer::new);
		TransformationApplier applier = newApplier(phaseWorkspace, new TransformationManager(map));

		// If we run the same transformer type in two phases...
		TransformationPhase first = new TransformationPhase("once", 1, ConvergingTransformer.class);
		TransformationPhase second = new TransformationPhase("stable", 3, ConvergingTransformer.class);
		TransformationPlanResult result = applier.transformJvm(new TransformationPlan(List.of(first, second)));

		// ...each phase gets a fresh instance that is set up once.
		assertEquals(2, ConvergingTransformer.INSTANCES.size());
		assertNotSame(ConvergingTransformer.INSTANCES.get(0), ConvergingTransformer.INSTANCES.get(1));
		assertEquals(1, ConvergingTransformer.INSTANCES.get(0).setupCalls);
		assertEquals(1, ConvergingTransformer.INSTANCES.get(1).setupCalls);

		// The one-pass phase still has pending work when it hits its bound, so it ends MAX_PASSES.
		assertEquals(1, result.phaseResults().get(0).passes());
		assertEquals(TransformationPhaseResult.Status.MAX_PASSES, result.phaseResults().get(0).status());

		// The three-pass phase reports no work on its second pass, so it converges and ends STABLE.
		assertEquals(2, result.phaseResults().get(1).passes());
		assertEquals(TransformationPhaseResult.Status.STABLE, result.phaseResults().get(1).status());

		// The terminal phase's output commits on apply().
		result.apply();
	}

	@Test
	void phasedExecutionDeniesBogus() throws Exception {
		Workspace phaseWorkspace = freshWorkspace();
		AtomicInteger invocationCount = new AtomicInteger();

		// Build transformer map with items used by the invalid plans below.
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(CountingTransformer.class, () -> new CountingTransformer(invocationCount));
		map.put(JvmTransformerA.class, JvmTransformerA::new);
		map.put(UnrelatedDependentTransformer.class, UnrelatedDependentTransformer::new);
		TransformationApplier applier = newApplier(phaseWorkspace, new TransformationManager(map));

		// Duplicate phase identifiers are rejected.
		TransformationPlan duplicateIds = new TransformationPlan(List.of(
				new TransformationPhase("same", 1),
				new TransformationPhase("same", 1)));
		assertThrows(TransformationException.class, () -> applier.transformJvm(duplicateIds));

		// Duplicate transformer classes within a phase are rejected.
		TransformationPlan duplicateTransformers = new TransformationPlan(List.of(
				new TransformationPhase("phase", 1, CountingTransformer.class, CountingTransformer.class)));
		assertThrows(TransformationException.class, () -> applier.transformJvm(duplicateTransformers));

		// A transformer may not depend on a type explicitly owned by a later phase.
		TransformationPlan unrelatedDependency = new TransformationPlan(List.of(
				new TransformationPhase("dependent", 1, UnrelatedDependentTransformer.class),
				new TransformationPhase("owner", 1, JvmTransformerA.class)));
		assertThrows(TransformationException.class, () -> applier.transformJvm(unrelatedDependency));

		// No invalid plan visited a single class.
		assertEquals(0, invocationCount.get(), "Invalid plans must not visit class transformers");

		// Unregistered transformer classes are rejected.
		TransformationApplier unregisteredApplier = newApplier(phaseWorkspace, new TransformationManager(Collections.emptyMap()));
		TransformationPlan unregistered = new TransformationPlan(List.of(
				new TransformationPhase("phase", 1, JvmTransformerA.class)));
		assertThrows(TransformationException.class, () -> unregisteredApplier.transformJvm(unregistered));
	}

	@Test
	void failedAndCancelledPhasesDoNotCommitPartialResults() throws Exception {
		Workspace failedWorkspace = freshWorkspace();

		// Build transformer map with three items
		//  - PhaseOneMarkerTransformer: adds a field
		//  - FailingMarkerTransformer: adds a field, then throws
		//  - PhaseTwoMarkerTransformer: adds a field
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(PhaseOneMarkerTransformer.class, PhaseOneMarkerTransformer::new);
		map.put(FailingMarkerTransformer.class, FailingMarkerTransformer::new);
		map.put(PhaseTwoMarkerTransformer.class, () -> new PhaseTwoMarkerTransformer(new AtomicBoolean()));
		TransformationApplier applier = newApplier(failedWorkspace, new TransformationManager(map));

		// If a plan fails in its second phase...
		TransformationPlan plan = new TransformationPlan(List.of(
				new TransformationPhase("seed", 1, PhaseOneMarkerTransformer.class),
				new TransformationPhase("failure", 1, FailingMarkerTransformer.class),
				new TransformationPhase("after", 1, PhaseTwoMarkerTransformer.class)));
		TransformationPlanResult result = applier.transformJvm(plan);

		// ...the plan is incomplete, with the seed phase applied, the failing phase failed, and the rest skipped.
		assertFalse(result.isComplete());
		assertEquals(List.of(TransformationPhaseResult.Status.MAX_PASSES,
						TransformationPhaseResult.Status.FAILED, TransformationPhaseResult.Status.SKIPPED),
				result.phaseResults().stream().map(TransformationPhaseResult::status).toList());
		assertFalse(result.phaseResults().get(1).result().getTransformerFailures().isEmpty());

		// The seed phase is already committed, but the failing phase's partial output is not.
		JvmClassInfo current = currentHelloWorld(failedWorkspace);
		assertNotNull(current.getDeclaredField(PhaseOneMarkerTransformer.FIELD_NAME, "I"));
		assertNull(current.getDeclaredField(FailingMarkerTransformer.FIELD_NAME, "I"));

		// Applying an aborted plan is a no-op, so the failed phase stays uncommitted.
		result.apply();
		assertNull(currentHelloWorld(failedWorkspace).getDeclaredField(FailingMarkerTransformer.FIELD_NAME, "I"));

		Workspace cancelledWorkspace = freshWorkspace();
		AtomicBoolean cancelled = new AtomicBoolean();

		// Build transformer map with three items
		//  - PhaseOneMarkerTransformer: adds a field
		//  - CancellingMarkerTransformer: adds a field, then feedback requests cancellation
		//  - PhaseTwoMarkerTransformer: adds a field
		map = new IdentityHashMap<>();
		map.put(PhaseOneMarkerTransformer.class, PhaseOneMarkerTransformer::new);
		map.put(CancellingMarkerTransformer.class, () -> new CancellingMarkerTransformer());
		map.put(PhaseTwoMarkerTransformer.class, () -> new PhaseTwoMarkerTransformer(new AtomicBoolean()));
		applier = newApplier(cancelledWorkspace, new TransformationManager(map));

		// The feedback requests cancellation once the cancelling transformer has run.
		TransformationFeedback feedback = new TransformationFeedback() {
			@Override
			public void onTransformed(@Nonnull Workspace workspace, @Nonnull WorkspaceResource resource,
			                          @Nonnull software.coley.recaf.workspace.model.bundle.ClassBundle<?> bundle,
			                          @Nonnull software.coley.recaf.info.ClassInfo classInfo,
			                          @Nonnull ClassTransformer transformer, int pass) {
				if (transformer instanceof CancellingMarkerTransformer)
					cancelled.set(true);
			}

			@Override
			public boolean hasRequestedCancellation() {
				return cancelled.get();
			}
		};

		// If a plan is cancelled in its second phase...
		plan = new TransformationPlan(List.of(
				new TransformationPhase("seed", 1, PhaseOneMarkerTransformer.class),
				new TransformationPhase("cancel", 1, CancellingMarkerTransformer.class),
				new TransformationPhase("after", 1, PhaseTwoMarkerTransformer.class)));
		result = applier.transformJvm(plan, feedback);

		// ...the plan is incomplete, with the seed phase applied, the cancelled phase uncommitted, and the rest skipped.
		assertFalse(result.isComplete());
		assertEquals(List.of(TransformationPhaseResult.Status.MAX_PASSES,
						TransformationPhaseResult.Status.CANCELLED, TransformationPhaseResult.Status.SKIPPED),
				result.phaseResults().stream().map(TransformationPhaseResult::status).toList());

		// The seed phase is committed, but the cancelled phase's output is not.
		assertNotNull(currentHelloWorld(cancelledWorkspace).getDeclaredField(PhaseOneMarkerTransformer.FIELD_NAME, "I"));
		assertNull(currentHelloWorld(cancelledWorkspace).getDeclaredField(CancellingMarkerTransformer.FIELD_NAME, "I"));
	}

	@Test
	void removalsAndMappingsArePreviewableAndApplied() throws Exception {
		Workspace removalWorkspace = freshWorkspace();

		// Build transformer map with one item
		//  - RemovingTransformer: marks the class for removal
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(RemovingTransformer.class, RemovingTransformer::new);
		TransformationApplier applier = newApplier(removalWorkspace, new TransformationManager(map));

		// If we run a single-phase plan that marks the class for removal...
		TransformationPlanResult result = applier.transformJvm(new TransformationPlan(List.of(
				new TransformationPhase("remove", 1, RemovingTransformer.class))));

		// ...the removal is reported without being committed yet, so the class is still in the workspace.
		JvmTransformResult removalResult = result.phaseResults().getFirst().result();
		assertNotNull(removalResult);
		assertEquals(1, removalResult.getClassesToRemove().size());
		ClassPathNode removalPath = removalResult.getClassesToRemove().iterator().next();
		assertSame(removalWorkspace.getPrimaryResource().getJvmClassBundle(), removalPath.getValueOfType(JvmClassBundle.class));
		assertNotNull(removalWorkspace.getPrimaryResource().getJvmClassBundle()
				.get(HELLO_WORLD));

		// Applying the plan removes the class, and the removal set stays readable afterwards.
		result.apply();
		assertEquals(0, removalWorkspace.getPrimaryResource().getJvmClassBundle().size());
		assertEquals(1, removalResult.getClassesToRemove().size());

		Workspace mappingWorkspace = freshWorkspace();

		// Build transformer map with one item
		//  - MappingTransformer: registers a rename of the class
		map = new IdentityHashMap<>();
		map.put(MappingTransformer.class, MappingTransformer::new);
		applier = newApplier(mappingWorkspace, new TransformationManager(map));

		// If we run a single-phase plan that registers a rename...
		result = applier.transformJvm(new TransformationPlan(List.of(new TransformationPhase("mapping", 1, MappingTransformer.class))));

		// ...the mapping is reported without being committed yet, so the class keeps its old name.
		JvmTransformResult mappingResult = result.phaseResults().getFirst().result();
		assertNotNull(mappingResult);
		assertNotNull(mappingResult.getMappingsToApply().getClassMapping(HELLO_WORLD));
		assertNotNull(mappingWorkspace.getPrimaryResource().getJvmClassBundle().get(HELLO_WORLD));
		assertNull(mappingWorkspace.getPrimaryResource().getJvmClassBundle().get(MappingTransformer.MAPPED_NAME));

		// Applying the plan commits the rename.
		result.apply();
		assertNotNull(mappingWorkspace.getPrimaryResource().getJvmClassBundle().get(MappingTransformer.MAPPED_NAME));
	}

	/**
	 * @return New workspace with {@link HelloWorld} class.
	 *
	 * @throws IOException
	 * 		When hello world cannot be read.
	 */
	@Nonnull
	private static Workspace freshWorkspace() throws IOException {
		return TestClassUtils.fromBundle(TestClassUtils.fromClasses(HelloWorld.class));
	}

	/**
	 * @param workspace
	 * 		Workspace to apply within.
	 * @param manager
	 * 		Transformation manager holding the transformers to use.
	 *
	 * @return Applier for the given workspace and manager.
	 */
	@Nonnull
	private static TransformationApplier newApplier(@Nonnull Workspace workspace, @Nonnull TransformationManager manager) {
		InheritanceGraph graph = recaf.get(InheritanceGraphService.class).newInheritanceGraph(workspace);
		MappingApplier mappings = recaf.get(MappingApplierService.class).inWorkspace(workspace);
		return new TransformationApplier(manager, config, graph, mappings, workspace);
	}

	/**
	 * @param workspace
	 * 		Workspace to grab class from.
	 *
	 * @return Current {@link HelloWorld} class in the workspace.
	 */
	@Nonnull
	private static JvmClassInfo currentHelloWorld(@Nonnull Workspace workspace) {
		ClassPathNode path = workspace.findJvmClass(HELLO_WORLD);
		assertNotNull(path);
		return path.getValue().asJvmClass();
	}

	/**
	 * Adds a field to the class if it does not already exist. Used by marker transformers to mark that a class was visited.
	 *
	 * @param context
	 * 		Transformer context.
	 * @param bundle
	 * 		Containing class bundle.
	 * @param initialClassState
	 * 		State of class pre-transformation.
	 * @param fieldName
	 * 		Field to add.
	 *
	 * @return {@code true} if the field was added, {@code false} if it already existed.
	 */
	private static boolean addField(@Nonnull JvmTransformerContext context, @Nonnull JvmClassBundle bundle,
	                                @Nonnull JvmClassInfo initialClassState, @Nonnull String fieldName) {
		ClassNode node = context.getNode(bundle, initialClassState);
		if (node.fields.stream().anyMatch(field -> fieldName.equals(field.name)))
			return false;
		node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, fieldName, "I", null, null));
		context.setNode(bundle, initialClassState, node);
		return true;
	}

	/**
	 * Transformer that adds a field to the class to mark that it was visited.
	 */
	static class PhaseOneMarkerTransformer implements JvmClassTransformer {
		static final String FIELD_NAME = "phaseOneMarker";

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			addField(context, bundle, initialClassState, FIELD_NAME);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "phase-one-marker";
		}
	}

	/**
	 * Transformer that adds a field to the class to mark that it was visited, and records whether the phase-one marker is visible.
	 */
	static class PhaseTwoMarkerTransformer implements JvmClassTransformer {
		static final String FIELD_NAME = "phaseTwoMarker";
		private final AtomicBoolean sawPhaseOne;

		PhaseTwoMarkerTransformer(@Nonnull AtomicBoolean sawPhaseOne) {
			this.sawPhaseOne = sawPhaseOne;
		}

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			ClassPathNode path = workspace.findJvmClass(initialClassState.getName());
			sawPhaseOne.set(path != null && path.getValue().asJvmClass()
					.getDeclaredField(PhaseOneMarkerTransformer.FIELD_NAME, "I") != null);
			addField(context, bundle, initialClassState, FIELD_NAME);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "phase-two-marker";
		}
	}

	/**
	 * Transformer that adds a field to the class to mark that it was visited, but only on its first transform call.
	 */
	static class ConvergingTransformer implements JvmClassTransformer {
		private static final List<ConvergingTransformer> INSTANCES = Collections.synchronizedList(new ArrayList<>());
		private static final AtomicInteger NEXT_ID = new AtomicInteger();
		private final int id = NEXT_ID.getAndIncrement();
		private int setupCalls;
		private int transformCalls;

		ConvergingTransformer() {
			INSTANCES.add(this);
		}

		@Override
		public void setup(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace) {
			setupCalls++;
		}

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			transformCalls++;
			if (transformCalls == 1)
				addField(context, bundle, initialClassState, "convergence" + id);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "converging";
		}
	}

	/**
	 * Transformer that counts how many times it was called.
	 */
	static class CountingTransformer implements JvmClassTransformer {
		private final AtomicInteger invocationCount;

		CountingTransformer(@Nonnull AtomicInteger invocationCount) {
			this.invocationCount = invocationCount;
		}

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			invocationCount.incrementAndGet();
		}

		@Nonnull
		@Override
		public String identifier() {
			return "counting";
		}
	}

	/**
	 * Transformer that depends on a transformer that is not in the same phase.
	 */
	static class UnrelatedDependentTransformer implements JvmClassTransformer {
		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends ClassTransformer>> dependencies() {
			return Collections.singleton(JvmTransformerA.class);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "unrelated-dependent";
		}
	}

	/**
	 * Transformer that adds a field to the class to mark that it was visited, but then throws an exception to simulate a failure.
	 */
	static class FailingMarkerTransformer implements JvmClassTransformer {
		static final String FIELD_NAME = "failedMarker";

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			addField(context, bundle, initialClassState, FIELD_NAME);
			throw new IllegalStateException("intentional phase failure");
		}

		@Nonnull
		@Override
		public String identifier() {
			return "failing-marker";
		}
	}

	/**
	 * Transformer that adds a field to the class to mark that it was visited.
	 * The calling context will request cancellation after this transformer has run, so the phase will be aborted.
	 */
	static class CancellingMarkerTransformer implements JvmClassTransformer {
		static final String FIELD_NAME = "cancelledMarker";

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			addField(context, bundle, initialClassState, FIELD_NAME);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "cancelling-marker";
		}
	}

	/**
	 * Transformer that marks the class for removal.
	 */
	static class RemovingTransformer implements JvmClassTransformer {
		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			context.markClassForRemoval(initialClassState);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "removing";
		}
	}

	/**
	 * Transformer that registers a rename of the class.
	 */
	static class MappingTransformer implements JvmClassTransformer {
		static final String MAPPED_NAME = "phase/MappedHelloWorld";

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			context.getMappings().addClass(initialClassState.getName(), MAPPED_NAME);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "mapping";
		}
	}

	/**
	 * Transformer that breaks the class by writing a bogus method descriptor, which will cause a failure during write-back.
	 */
	static class FrameBreakingTransformer implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// Bogus method descriptor will cause a failure during transformation write-back.
			ClassNode broken = new ClassNode();
			broken.visit(initialClassState.getVersion(), Opcodes.ACC_PUBLIC, initialClassState.getName(), null, "java/lang/Object", null);
			MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "broken", "(Q)V", null, null);
			method.instructions.add(new InsnNode(Opcodes.RETURN));
			method.maxStack = 0;
			method.maxLocals = 0;
			broken.methods.add(method);
			broken.visitEnd();

			context.setNode(bundle, initialClassState, broken);
			context.setRecomputeFrames(initialClassState.getName());
		}

		@Nonnull
		@Override
		public String identifier() {
			return "frame-breaking";
		}
	}

	/**
	 * Transformer that does nothing, used for testing dependency resolution.
	 */
	static class JvmTransformerA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-a";
		}
	}

	/**
	 * Transformer that does nothing, used for testing dependency resolution.
	 */
	static class JvmTransformerB implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-b";
		}
	}

	/**
	 * Transformer that depends on {@link JvmTransformerA}, used for testing dependency resolution.
	 */
	static class JvmTransformerDependingOnA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends ClassTransformer>> dependencies() {
			return Collections.singleton(JvmTransformerA.class);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-depending-on-a";
		}
	}

	/**
	 * Transformer that depends on itself, used for testing cycle detection.
	 */
	static class JvmCycleSingle implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends ClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleSingle.class);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-cycle";
		}
	}

	/**
	 * Transformer that depends on {@link JvmCycleB}, used for testing cycle detection.
	 */
	static class JvmCycleA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends ClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleB.class);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-cycle-a";
		}
	}

	/**
	 * Transformer that depends on {@link JvmCycleA}, used for testing cycle detection.
	 */
	static class JvmCycleB implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo initialClassState) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends ClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleA.class);
		}

		@Nonnull
		@Override
		public String identifier() {
			return "jvm-cycle-b";
		}
	}
}