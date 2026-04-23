package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.test.TestBase;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ScriptRunController}.
 */
class ScriptRunControllerTest extends TestBase {
	static ScriptRunController controller;

	@BeforeAll
	static void setup() {
		controller = new ScriptRunController(recaf.get(ScriptEngine.class));
	}

	@Test
	void testStopAfterRestart() throws Exception {
		Path key = Path.of("test-script");
		String counterProperty = "recaf.script-run-controller.counter";
		String script = loopPrintingScript(counterProperty);

		// Start the script that writes to the given property.
		System.clearProperty(counterProperty);
		CompletableFuture<ScriptResult> firstRun = controller.start(key, script);

		// Wait for the script to start and write to the property a few times.
		awaitCounter(counterProperty, 2);

		// Verify the script is running and then request it to stop.
		assertTrue(controller.isRunning(key));
		controller.requestStop();

		// Wait for the script to acknowledge the stop request and verify it was cancelled.
		assertTrue(firstRun.get(5, TimeUnit.SECONDS).wasCancelled());
		assertFalse(controller.isRunning(key));

		// Start the script again and verify it can run after being stopped.
		System.clearProperty(counterProperty);
		CompletableFuture<ScriptResult> secondRun = controller.start(key, script);
		awaitCounter(counterProperty, 2);
		assertTrue(controller.isRunning(key));
		controller.requestStop();

		// Wait for the script to acknowledge the stop request and verify it was cancelled.
		ScriptResult secondResult = secondRun.get(5, TimeUnit.SECONDS);
		assertTrue(secondResult.wasCancelled());
		assertFalse(controller.isRunning(key));
		assertTrue(Integer.parseInt(System.getProperty(counterProperty, "100")) < 100);
	}

	@Test
	void testOnlyOneScriptRunsAtATime() throws Exception {
		Path firstKey = Path.of("first-script");
		Path secondKey = Path.of("second-script");
		String counterProperty = "recaf.script-run-controller.concurrent";
		String script = loopPrintingScript(counterProperty);

		// Start the first script that writes to the given property.
		System.clearProperty(counterProperty);
		CompletableFuture<ScriptResult> firstRun = controller.start(firstKey, script);
		awaitCounter(counterProperty, 2);

		// Attempt to start a second script while the first is still running. This should fail.
		CompletableFuture<ScriptResult> secondRun = controller.start(secondKey,
				"System.setProperty(\"recaf.script-run-controller.second\", \"ran\");");
		assertTrue(secondRun.isCompletedExceptionally());
		assertFalse(controller.isRunning(secondKey));

		// Request the first script to stop and verify it was cancelled.
		controller.requestStop();
		assertTrue(firstRun.get(5, TimeUnit.SECONDS).wasCancelled());
		assertFalse(controller.isRunning());
	}

	@Nonnull
	private static String loopPrintingScript(String counterProperty) {
		return """
				for (int i = 0; i < 100; i++) {
					System.setProperty("%s", String.valueOf(i));
					try {
						Thread.sleep(10);
					} catch (Throwable t) {}
				}
				""".formatted(counterProperty);
	}

	@SuppressWarnings("all")
	private static void awaitCounter(String property, int min) throws InterruptedException {
		long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);
		while (Integer.parseInt(System.getProperty(property, "-1")) < min && System.currentTimeMillis() < end)
			Thread.sleep(10);
		assertTrue(Integer.parseInt(System.getProperty(property, "-1")) >= min);
	}
}
