package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;
import software.coley.recaf.services.decompile.procyon.ProcyonDecompiler;
import software.coley.recaf.services.decompile.vineflower.VineflowerDecompiler;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link DecompilerManager}.
 */
public class DecompileManagerTest extends TestBase {
	static final TestJvmBytecodeFilter bytecodeFilter = new TestJvmBytecodeFilter();
	static final TestOutputTextFilter textFilter = new TestOutputTextFilter();
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

	@Test
	void testVineflower() {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(VineflowerDecompiler.NAME);
		assertNotNull(decompiler, "Vineflower decompiler was never registered with manager");
		runJvmDecompilation(decompiler);
	}

	@Test
	void testFiltersUsed() {
		JvmDecompiler decompiler = decompilerManager.getTargetJvmDecompiler();
		TestJvmBytecodeFilter bytecodeFilterSpy = spy(bytecodeFilter);
		TestOutputTextFilter textFilterSpy = spy(textFilter);
		try {
			// Add input/output filters
			decompilerManager.addJvmBytecodeFilter(bytecodeFilterSpy);
			decompilerManager.addOutputTextFilter(textFilterSpy);

			// Decompile
			decompilerManager.decompile(decompiler, workspace, classToDecompile).get();

			// Verify each filter was called once
			verify(bytecodeFilterSpy, times(1)).filter(any(), any(), any());
			verify(textFilterSpy, times(1)).filter(any(), any(), anyString());
		} catch (Exception ex) {
			fail(ex);
		} finally {
			decompilerManager.removeJvmBytecodeFilter(bytecodeFilterSpy);
			decompilerManager.removeOutputTextFilter(textFilterSpy);
		}
	}

	private static void runJvmDecompilation(@Nonnull JvmDecompiler decompiler) {
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
			decompiler.getConfig().setHash(-1);
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

	static class TestJvmBytecodeFilter implements JvmBytecodeFilter {
		@Nonnull
		@Override
		public byte[] filter(@Nonnull Workspace workspace, @Nonnull JvmClassInfo initialClassInfo, @Nonnull byte[] bytecode) {
			return bytecode;
		}
	}

	static class TestOutputTextFilter implements OutputTextFilter {
		@Nonnull
		@Override
		public String filter(@Nonnull Workspace workspace, @Nonnull ClassInfo classInfo, @Nonnull String code) {
			return code;
		}
	}

}
