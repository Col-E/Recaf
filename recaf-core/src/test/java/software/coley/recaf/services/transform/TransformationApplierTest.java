package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceGraphService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link TransformationApplier}
 */
class TransformationApplierTest extends TestBase {
	private static final TransformationApplierConfig config = new TransformationApplierConfig();
	private static final InheritanceGraph inheritanceGraph;
	private static final Workspace workspace;

	static {
		// Make a dummy workspace. We just need a single class (and any class will work)
		try {
			workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(HelloWorld.class));
			inheritanceGraph = recaf.get(InheritanceGraphService.class).newInheritanceGraph(workspace);
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
		TransformationApplier applier = new TransformationApplier(manager, inheritanceGraph, workspace);
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
		TransformationApplier applier = new TransformationApplier(manager, inheritanceGraph, workspace);
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
		TransformationApplier applier = new TransformationApplier(manager, inheritanceGraph, workspace);
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
		TransformationApplier applier = new TransformationApplier(manager, inheritanceGraph, workspace);
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleSingle.class)));
		verify(transformer, never()).transform(any(), same(workspace), any(), any(), any());
	}

	@Test
	void missingRegistration() {
		// If we transform with a transformer that is not registered in the manager, the transform should fail
		TransformationManager manager = new TransformationManager(Collections.emptyMap());
		TransformationApplier applier = new TransformationApplier(manager, inheritanceGraph, workspace);
		assertThrows(TransformationException.class, () -> applier.transformJvm(Collections.singletonList(JvmCycleSingle.class)));
	}

	static class JvmTransformerA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-a";
		}
	}

	static class JvmTransformerB implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-b";
		}
	}

	static class JvmTransformerDependingOnA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends JvmClassTransformer>> dependencies() {
			return Collections.singleton(JvmTransformerA.class);
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-depending-on-a";
		}
	}

	static class JvmCycleSingle implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends JvmClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleSingle.class);
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-cycle";
		}
	}

	static class JvmCycleA implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends JvmClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleB.class);
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-cycle-a";
		}
	}

	static class JvmCycleB implements JvmClassTransformer {

		@Override
		public void transform(@Nonnull JvmTransformerContext context, @Nonnull Workspace workspace,
		                      @Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle,
		                      @Nonnull JvmClassInfo classInfo) {
			// no-op
		}

		@Nonnull
		@Override
		public Set<Class<? extends JvmClassTransformer>> dependencies() {
			return Collections.singleton(JvmCycleA.class);
		}

		@Nonnull
		@Override
		public String name() {
			return "jvm-cycle-b";
		}
	}
}