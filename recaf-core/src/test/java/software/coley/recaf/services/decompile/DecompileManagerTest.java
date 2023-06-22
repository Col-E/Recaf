package software.coley.recaf.services.decompile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.TestBase;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.decompile.procyon.ProcyonDecompiler;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DecompilerManager}.
 */
public class DecompileManagerTest extends TestBase {
	static DecompilerManager decompilerManager;
	static Workspace workspace;
	static JvmClassInfo classToDecompile;

	@BeforeAll
	static void setup() throws IOException {
		decompilerManager = recaf.get(DecompilerManager.class);
		classToDecompile = TestClassUtils.fromRuntimeClass(HelloWorld.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classToDecompile));
		workspaceManager.setCurrent(workspace);
	}

	@Test
	void testCfr() {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(CfrDecompiler.NAME);
		assertNotNull(decompiler, "CFR decompiler was never registered with manager");
		runJvmDecompilation(decompiler);
	}

	@Test
	void testProcyon() {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(ProcyonDecompiler.NAME);
		assertNotNull(decompiler, "Procyon decompiler was never registered with manager");
		runJvmDecompilation(decompiler);
	}

	private static void runJvmDecompilation(JvmDecompiler decompiler) {
		try {
			// Generally, you'd handle results like this, with a when-complete.
			// The blocking 'get' at the end is just so our test works.
			DecompileResult firstResult = decompilerManager.decompile(decompiler, workspace, classToDecompile)
					.whenComplete((result, throwable) -> {
						assertNull(throwable);

						// Throwable thrown when unhandled exception occurs.
						assertEquals(DecompileResult.ResultType.SUCCESS, result.getType(), "Decompile result was not successful");
						assertNotNull(result.getText(), "Decompile result missing text");
						assertTrue(result.getText().contains("\"Hello world\""), "Decompilation seems to be wrong");
					}) // Block on this thread until we have the value.
					.get(1, TimeUnit.DAYS);

			// Assert that repeated decompiles use the same result (caching, should be handled by abstract base)
			DecompileResult newResult = decompiler.decompile(workspace, classToDecompile);
			assertSame(firstResult, newResult, "Decompiler did not cache results");

			// Change the decompiler hash. The decompiler result should change.
			decompiler.getConfig().setConfigHash(-1);
			newResult = decompilerManager.decompile(decompiler, workspace, classToDecompile)
					.get(1, TimeUnit.SECONDS);
			assertNotSame(firstResult, newResult, "Decompiler used cached result even though config hash changed");
		} catch (InterruptedException e) {
			fail("Decompile was interrupted", e);
		} catch (ExecutionException e) {
			fail("Decompile was encountered exception", e.getCause());
		} catch (TimeoutException e) {
			fail("Decompile timed out", e);
		}
	}
}
