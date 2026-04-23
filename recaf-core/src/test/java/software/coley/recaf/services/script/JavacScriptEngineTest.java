package software.coley.recaf.services.script;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.test.TestBase;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavacScriptEngine}
 */
@Execution(ExecutionMode.SAME_THREAD)
public class JavacScriptEngineTest extends TestBase {
	static JavacScriptEngine engine;
	public static CountDownLatch concurrentStarted;
	public static CountDownLatch concurrentRelease;
	public static CountDownLatch restartStarted;
	public static AtomicInteger restartCounter = new AtomicInteger();

	@BeforeAll
	static void setup() {
		engine = recaf.get(JavacScriptEngine.class);
	}

	@Nested
	class Snippet {
		@Test
		void testHelloWorld() {
			assertSuccess("System.out.println(\"hello\");");
		}

		@Test
		void testCancellation() throws ExecutionException, InterruptedException, TimeoutException {
			GenerateResult generated = engine.compile("""
					long value = 0;
					while (true) {
						value++;
					}
					""").get(5, TimeUnit.SECONDS);
			assertTrue(generated.wasSuccess());

			CompletableFuture<ScriptResult> run = engine.run(generated);
			generated.requestStop();

			ScriptResult result = run.get(5, TimeUnit.SECONDS);
			assertTrue(result.wasCancelled());
			assertFalse(result.wasSuccess());
			assertFalse(result.wasCompileFailure());
			assertFalse(result.wasRuntimeError());
			assertNull(result.getRuntimeThrowable());
		}

		@Test
		void testRunAfterCancellation() throws ExecutionException, InterruptedException, TimeoutException {
			restartStarted = new CountDownLatch(1);
			restartCounter.set(0);
			GenerateResult generated = engine.compile("""
					software.coley.recaf.services.script.JavacScriptEngineTest.restartStarted.countDown();
					for (int i = 0; i < 10; i++) {
						software.coley.recaf.services.script.JavacScriptEngineTest.restartCounter.incrementAndGet();
						try {
							Thread.sleep(10);
						} catch (Throwable t) {}
					}
					""").get(5, TimeUnit.SECONDS);
			assertTrue(generated.wasSuccess());

			CompletableFuture<ScriptResult> stoppedRun = engine.run(generated);
			assertTrue(restartStarted.await(1, TimeUnit.SECONDS));
			generated.requestStop();
			assertTrue(stoppedRun.get(5, TimeUnit.SECONDS).wasCancelled());

			restartStarted = new CountDownLatch(1);
			restartCounter.set(0);
			ScriptResult secondRun = engine.run(generated).get(5, TimeUnit.SECONDS);
			assertTrue(secondRun.wasSuccess());
			assertEquals(10, restartCounter.get());
		}

		@Test
		void testStopAfterRestart() throws ExecutionException, InterruptedException, TimeoutException {
			testStopAfterRestart(false);
		}

		@Test
		void testStopAfterRecompileRestart() throws ExecutionException, InterruptedException, TimeoutException {
			testStopAfterRestart(true);
		}

		void testStopAfterRestart(boolean recompile) throws ExecutionException, InterruptedException, TimeoutException {
			String script = """
					software.coley.recaf.services.script.JavacScriptEngineTest.restartStarted.countDown();
					for (int i = 0; i < 1000; i++) {
						software.coley.recaf.services.script.JavacScriptEngineTest.restartCounter.incrementAndGet();
						try {
							Thread.sleep(10);
						} catch (Throwable t) {}
					}
					""";
			GenerateResult generated = engine.compile(script).get(5, TimeUnit.SECONDS);
			assertTrue(generated.wasSuccess());

			restartStarted = new CountDownLatch(1);
			restartCounter.set(0);
			CompletableFuture<ScriptResult> firstRun = engine.run(generated);
			assertTrue(restartStarted.await(1, TimeUnit.SECONDS));
			awaitCounter(2);
			generated.requestStop();
			assertTrue(firstRun.get(5, TimeUnit.SECONDS).wasCancelled());

			if (recompile)
				generated = engine.compile(script).get(5, TimeUnit.SECONDS);

			restartStarted = new CountDownLatch(1);
			restartCounter.set(0);
			CompletableFuture<ScriptResult> secondRun = engine.run(generated);
			assertTrue(restartStarted.await(1, TimeUnit.SECONDS));
			awaitCounter(2);
			generated.requestStop();

			ScriptResult result = secondRun.get(5, TimeUnit.SECONDS);
			assertTrue(result.wasCancelled());
			assertTrue(restartCounter.get() < 1000);
		}

		@Test
		void testConcurrentRuns() throws ExecutionException, InterruptedException, TimeoutException {
			concurrentStarted = new CountDownLatch(2);
			concurrentRelease = new CountDownLatch(1);
			String template = """
					software.coley.recaf.services.script.JavacScriptEngineTest.concurrentStarted.countDown();
					try {
						software.coley.recaf.services.script.JavacScriptEngineTest.concurrentRelease.await(5, java.util.concurrent.TimeUnit.SECONDS);
					} catch (InterruptedException ex) {
						throw new RuntimeException(ex);
					}
					""";
			GenerateResult first = engine.compile(template + "\n// first").get(5, TimeUnit.SECONDS);
			GenerateResult second = engine.compile(template + "\n// second").get(5, TimeUnit.SECONDS);
			assertTrue(first.wasSuccess());
			assertTrue(second.wasSuccess());

			CompletableFuture<ScriptResult> firstRun = engine.run(first);
			CompletableFuture<ScriptResult> secondRun = engine.run(second);
			try {
				assertTrue(concurrentStarted.await(1, TimeUnit.SECONDS), "Scripts did not run concurrently");
			} finally {
				concurrentRelease.countDown();
			}

			assertTrue(firstRun.get(5, TimeUnit.SECONDS).wasSuccess());
			assertTrue(secondRun.get(5, TimeUnit.SECONDS).wasSuccess());
		}

		@Test
		void repeatedDefinitions() {
			String propertyName = "test-xyz";

			// set = one
			assertSuccess("System.setProperty(\"" + propertyName + "\", \"one\");");
			assertEquals("one", System.getProperty(propertyName),
					"Javac script engine failed to set property: " + propertyName);

			// set = one
			assertSuccess("System.setProperty(\"" + propertyName + "\", \"two\");");
			assertEquals("two", System.getProperty(propertyName),
					"Javac script engine failed to set property correctly after script modifications: " + propertyName);

			// set = two
			assertSuccess("System.setProperty(\"" + propertyName + "\", \"three\");");
			assertEquals("three", System.getProperty(propertyName),
					"Javac script engine failed to set property correctly after script modifications: " + propertyName);
		}
	}

	@Nested
	class Full {
		@Test
		void testConstructorInjection() {
			assertSuccess("""
					@Dependent
					public class Test implements Runnable {
						private final JavacCompiler compiler;
										
						@Inject
						public Test(JavacCompiler compiler) {
							this.compiler = compiler;
						}
						
						@Override
						public void run() {
							System.out.println("hello: " + compiler);
							if (compiler == null) throw new IllegalStateException();
						}
					}
					""");
		}

		@Test
		void testFieldInjection() {
			assertSuccess("""
					@Dependent
					public class Test implements Runnable {
						@Inject
						JavacCompiler compiler;
						
						@Override
						public void run() {
							System.out.println("hello: " + compiler);
							if (compiler == null) throw new IllegalStateException();
						}
					}
					""");
		}

		@Test
		void testInjectionWithoutStatedScope() {
			assertSuccess("""
					public class Test implements Runnable {
						@Inject
						JavacCompiler compiler;
						
						@Override
						public void run() {
							System.out.println("hello: " + compiler);
							if (compiler == null) throw new IllegalStateException();
						}
					}
					""");
		}

		@Test
		void repeatedDefinitions() {
			String propertyName = "test-xyz";
			String template = """
					public class Test implements Runnable {
						@Override
						public void run() {
							System.setProperty("%s", "%s");
						}
					}
					""";

			// set = one
			assertSuccess(template.formatted(propertyName, "one"));
			assertEquals("one", System.getProperty(propertyName),
					"Javac script engine failed to set property: " + propertyName);

			// set = one
			assertSuccess(template.formatted(propertyName, "two"));
			assertEquals("two", System.getProperty(propertyName),
					"Javac script engine failed to set property correctly after script modifications: " + propertyName);

			// set = two
			assertSuccess(template.formatted(propertyName, "three"));
			assertEquals("three", System.getProperty(propertyName),
					"Javac script engine failed to set property correctly after script modifications: " + propertyName);
		}
	}

	static void assertSuccess(String code) {
		try {
			engine.run(code).thenAccept(result -> {
				for (CompilerDiagnostic diagnostic : result.getCompileDiagnostics())
					fail("Unexpected diagnostic: " + diagnostic);

				Throwable thrown = result.getRuntimeThrowable();
				if (thrown != null)
					fail("Unexpected exception at runtime", thrown);

				assertTrue(result.wasSuccess());
				assertFalse(result.wasCompileFailure());
				assertFalse(result.wasRuntimeError());
			}).get(5, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException ex) {
			fail(ex);
		}
	}

	static void awaitCounter(int min) throws InterruptedException {
		long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1);
		while (restartCounter.get() < min && System.currentTimeMillis() < end)
			Thread.sleep(10);
		assertTrue(restartCounter.get() >= min);
	}
}
