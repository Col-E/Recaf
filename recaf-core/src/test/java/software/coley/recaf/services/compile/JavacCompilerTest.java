package software.coley.recaf.services.compile;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.util.JavaVersion;
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
	void testJavacDownsample() {
		JavacArguments arguments = new JavacArgumentsBuilder()
				.withDownsampleTarget(8)
				.withClassName("HelloWorld")
				.withClassSource("""
						import java.util.ArrayList;
						import java.util.List;
						public class HelloWorld {
							public static void main(String[] args) {
								if (args.length < 1)
									return;
								String amountProperty = System.getProperty(args[0]);
								String amount = amountProperty;
						
								// List.of should be replaced since it was added in Java 9
								List<String> values = new ArrayList<>(switch (amount) {
									case "one" -> List.of("one");
									case "two" -> List.of("one", "two");
									case "three" -> List.of("one", "two", "three");
									default -> throw new IllegalStateException("Unexpected value: " + amount);
								});
								while (!values.isEmpty()) {
									// Should removeLast since it was added in Java 21
									String last = values.removeLast();
									System.out.println(last);
								}
							}
						}
						""")
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

		// Assert downgrade was a success
		assertEquals(8, classInfo.getVersion() - JavaVersion.VERSION_OFFSET, "Class was not downgraded to Java 8");
		classInfo.getClassReader().accept(new ClassVisitor(RecafConstants.getAsmVersion()) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				return new MethodVisitor(RecafConstants.getAsmVersion()) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						if (owner.equals("java/util/List")) {
							if (name.equals("of"))
								fail("Did not downgrade away: List.of");
							else if (name.equals("removeLast"))
								fail("Did not downgrade away: List.removeLast");
						}
					}
				};
			}
		}, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);

		// Assert the necessary stubs were added
		assertNotNull(result.getCompilations().get("xyz/wagyourtail/jvmdg/j9/stub/java_base/J_U_List"), "Missing stub for Java 9: List.of");
		assertNotNull(result.getCompilations().get("xyz/wagyourtail/jvmdg/j21/stub/java_base/J_U_List"), "Missing stub for Java 21: List.removeLast");
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
		ClassReader reader = classInfo.getClassReader();
		ClassWriter writer = new ClassWriter(reader, 0);
		ClassRemapper mapper = new ClassRemapper(writer, new SimpleRemapper(RecafConstants.getAsmVersion(), classInfo.getName(), "dummy/StringConsumer"));
		reader.accept(mapper, 0);
		classInfo = new JvmClassInfoBuilder(writer.toByteArray()).build();

		// Put it into a workspace and try again. Should work now that it can pull the missing class from the workspace.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classInfo));
		result = javac.compile(arguments, workspace, null);
		assertEquals(0, result.getDiagnostics().size(), "There were unexpected diagnostic messages");
		assertTrue(result.getCompilations().containsKey("HelloWorld"), "Class missing from compile map output");
	}
}
