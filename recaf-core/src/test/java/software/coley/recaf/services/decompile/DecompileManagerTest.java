package software.coley.recaf.services.decompile;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.coley.observables.ObservableBoolean;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.decompile.fallback.FallbackDecompiler;
import software.coley.recaf.services.decompile.filter.JvmBytecodeFilter;
import software.coley.recaf.services.decompile.filter.OutputTextFilter;
import software.coley.recaf.services.decompile.procyon.ProcyonDecompiler;
import software.coley.recaf.services.decompile.vineflower.VineflowerDecompiler;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.ReflectUtil;
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
	private static final ObservableBoolean OB_TRUE = new ObservableBoolean(true);
	private static final ObservableBoolean OB_FALSE = new ObservableBoolean(false);
	static final TestJvmBytecodeFilter bytecodeFilter = new TestJvmBytecodeFilter();
	static final TestOutputTextFilter textFilter = new TestOutputTextFilter();
	static DecompilerManager decompilerManager;
	static DecompilerManagerConfig decompilerManagerConfig;
	static Workspace workspace;
	static JvmClassInfo classHelloWorld;

	@BeforeAll
	static void setup() throws IOException {
		decompilerManager = recaf.get(DecompilerManager.class);

		// Setup workspace to pull from
		classHelloWorld = TestClassUtils.fromRuntimeClass(HelloWorld.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classHelloWorld));
		workspaceManager.setCurrent(workspace);
	}

	@BeforeEach
	void setupEach() {
		// We don't want to cache decompilations for this test, but we also
		// do not want to edit the shared config in tests.
		// Thus, we will make new config instances each test run so there's no cross-test pollution.
		decompilerManagerConfig = new DecompilerManagerConfig();
		decompilerManagerConfig.getCacheDecompilations().setValue(false);
		assertDoesNotThrow(() -> ReflectUtil.quietSet(unwrapProxy(decompilerManager),
				DecompilerManager.class.getDeclaredField("config"),
				decompilerManagerConfig));
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
	void testFallback() {
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(FallbackDecompiler.NAME);
		assertNotNull(decompiler, "Fallback decompiler was never registered with manager");
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
			decompilerManager.decompile(decompiler, workspace, classHelloWorld).get();

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

	@Test
	void testCaching() {
		decompilerManagerConfig.getCacheDecompilations().setValue(true);
		JvmDecompiler decompiler = decompilerManager.getJvmDecompiler(CfrDecompiler.NAME);
		DecompileResult firstResult = assertDoesNotThrow(() -> decompilerManager.decompile(decompiler, workspace, classHelloWorld).get(1, TimeUnit.DAYS));

		// Assert that repeated decompiles use the same result (caching, should be handled by abstract base)
		// Only the manager will cache results. Using decompilers direcrly will not cache.
		assertTrue(decompilerManagerConfig.getCacheDecompilations().getValue(), "Cache config not 'true'");
		DecompileResult newResult = assertDoesNotThrow(() -> decompilerManager.decompile(decompiler, workspace, classHelloWorld).get(1, TimeUnit.SECONDS));
		assertSame(firstResult, newResult, "Decompiler did not cache results");

		// Change the decompiler hash. The decompiler result should change.
		decompiler.getConfig().setHash(-1);
		newResult = assertDoesNotThrow(() -> decompilerManager.decompile(decompiler, workspace, classHelloWorld).get(1, TimeUnit.SECONDS));
		assertNotSame(firstResult, newResult, "Decompiler used cached result even though config hash changed");

		// Verify direct decompiler usage does not cache
		DecompileResult direct1 = decompiler.decompile(workspace, classHelloWorld);
		DecompileResult direct2 = decompiler.decompile(workspace, classHelloWorld);
		assertNotSame(direct1, direct2, "Direct decompiler use cached results unexpectedly");
	}

	@Test
	void testFilterHollow() {
		String decompilationBefore = assertDoesNotThrow(() -> decompilerManager.decompile(workspace, classHelloWorld).get().getText());
		assertTrue(decompilationBefore.contains("\"Hello world\""));

		decompilerManagerConfig.getFilterHollow().setValue(true);

		// Hollowing will remove method bodies, so the 'println' call should no longer exist in the output
		String decompilationAfter = assertDoesNotThrow(() -> decompilerManager.decompile(workspace, classHelloWorld).get().getText());
		assertFalse(decompilationAfter.contains("\"Hello world\""));
	}

	@Test
	void testDisplay() {
		for (JvmDecompiler decompiler : decompilerManager.getJvmDecompilers()) {
			assertTrue(decompiler.toString().contains(decompiler.getName()));
			assertTrue(decompiler.toString().contains(decompiler.getVersion()));
		}
	}

	@Test
	void testComparison() {
		JvmDecompiler cfr = decompilerManager.getJvmDecompiler(CfrDecompiler.NAME);
		JvmDecompiler pro = decompilerManager.getJvmDecompiler(ProcyonDecompiler.NAME);
		assertNotNull(cfr);
		assertNotNull(pro);
		assertNotEquals(cfr, pro);
		assertNotEquals(cfr.hashCode(), pro.hashCode());
	}

	private static void runJvmDecompilation(@Nonnull JvmDecompiler decompiler) {
		try {
			// Generally, you'd handle results like this, with a when-complete.
			// The blocking 'get' at the end is just so our test works.
			DecompileResult firstResult = decompilerManager.decompile(decompiler, workspace, classHelloWorld)
					.whenComplete((result, throwable) -> {
						assertNull(throwable);

						// Throwable thrown when unhandled exception occurs.
						assertEquals(DecompileResult.ResultType.SUCCESS, result.getType(), "Decompile result was not successful");
						assertNotNull(result.getText(), "Decompile result missing text");
						assertTrue(result.getText().contains("\"Hello world\""), "Decompilation seems to be wrong");
					}) // Block on this thread until we have the value.
					.get(5, TimeUnit.SECONDS);

			// Verify direct decompiler usage does not cache
			DecompileResult result = decompiler.decompile(workspace, classHelloWorld);
			assertNull(result.getException(), "No exceptions should be reported during decompilation");
			assertNotNull(result.getText(), "Missing decompilation output");
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
