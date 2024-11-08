package software.coley.recaf.services.transform;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link TransformationManager}
 */
class TransformationManagerTest {
	@Test
	void single() {
		// Build transformer map with just one item
		Map<Class<? extends JvmClassTransformer>, Supplier<JvmClassTransformer>> map = new IdentityHashMap<>();
		map.put(TransformerExample.class, TransformerExample::new);

		// If we ask for that single transformer, we should get it without issue
		TransformationManager manager = new TransformationManager(map);
		TransformerExample transformer = assertDoesNotThrow(() -> manager.newJvmTransformer(TransformerExample.class));
		assertNotNull(transformer);
	}

	static class TransformerExample implements JvmClassTransformer {

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
}