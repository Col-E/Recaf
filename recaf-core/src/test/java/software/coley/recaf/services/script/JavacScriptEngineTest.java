package software.coley.recaf.services.script;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.TestBase;
import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavacScriptEngine}
 */
public class JavacScriptEngineTest extends TestBase {
	static JavacScriptEngine engine;

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
}
