package software.coley.recaf.services.compile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JavacCompiler}
 */
public class JavacCompilerTest extends TestBase {
	static JavacCompiler javac;

	@BeforeAll
	static void setup() {
		assertTrue(JavacCompiler.isAvailable(), "javac not available!");
		javac = recaf.get(JavacCompiler.class);
	}

	@Test
	void testJavacWithoutWorkspace() {
		JavacArguments arguments = new JavacArgumentsBuilder()
				.withClassName("HelloWorld")
				.withClassSource("""
						public class HelloWorld {
							public static void main(String[] args) {
								System.out.println("Hello world");
							}
						}""")
				.build();

		// Run compiler
		CompilerResult result = javac.compile(arguments, null, null);

		// Assert no errors
		assertTrue(result.wasSuccess(), "Result does not indicate success");
		assertEquals(0, result.getDiagnostics().size(), "There were unexpected diagnostic messages");
		assertTrue(result.getCompilations().containsKey("HelloWorld"), "Class missing from compile map output");

		// Assert class validity
		byte[] classBytecode = result.getCompilations().get("HelloWorld");
		JvmClassInfo classInfo = new JvmClassInfoBuilder(classBytecode).build();
		assertEquals("HelloWorld", classInfo.getName(), "Class name did not match expected value");
		assertNotNull(classInfo.getDeclaredMethod("main", "([Ljava/lang/String;)V"), "Missing main method");
	}

	@Test
	void testJavacUsesVirtualClasspathFromWorkspace() throws IOException {
		// Create a HelloWorld that uses 'StringConsumer'
		JavacArguments arguments = new JavacArgumentsBuilder()
				.withClassName("HelloWorld")
				.withClassSource("""
						import dummy.StringConsumer;

						public class HelloWorld {
							public static void main(String[] args) {
								for (String arg : args) {
									new StringConsumer().accept(arg);
								}
							}
						}""")
				.build();

		// Run compiler, it should fail with no passed workspace due to the unknown 'StringConsumer'
		CompilerResult result = javac.compile(arguments, null, null);
		assertFalse(result.getDiagnostics().isEmpty(), "Expected compilation failure");
		assertFalse(result.getCompilations().containsKey("HelloWorld"), "Class should have failed compilation");

		// First, create the class that was missing. We cannot use the existing StringConsumer because Javac will find
		// it on our class-path. So, we remap it to a different package.
		JvmClassInfo classInfo = TestClassUtils.fromRuntimeClass(StringConsumer.class);
		ClassWriter writer = new ClassWriter(0);
		ClassReader reader = classInfo.getClassReader();
		ClassRemapper mapper = new ClassRemapper(writer, new SimpleRemapper(classInfo.getName(), "dummy/StringConsumer"));
		reader.accept(mapper, 0);
		classInfo = new JvmClassInfoBuilder(writer.toByteArray()).build();

		// Put it into a workspace and try again. Should work now that it can pull the missing class from the workspace.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classInfo));
		result = javac.compile(arguments, workspace, null);
		assertEquals(0, result.getDiagnostics().size(), "There were unexpected diagnostic messages");
		assertTrue(result.getCompilations().containsKey("HelloWorld"), "Class missing from compile map output");
	}
}
