package software.coley.recaf.ui.control.richtext.source;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.source.AstService;
import software.coley.recaf.services.source.ResolverAdapter;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.ClassWithLambda;
import software.coley.recaf.test.dummy.LambdaCalcExample;
import software.coley.recaf.test.dummy.LambdaExample;
import software.coley.recaf.ui.control.richtext.Editor;
import software.coley.recaf.util.Handles;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.sourcesolver.model.ClassModel;
import software.coley.sourcesolver.model.CompilationUnitModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link JavaContextActionSupport} <i>(At this point, only static methods,
 * because the rest of the class is bound to an {@link Editor} which makes things trickier to test...
 * Really ought to work on that at some point.)</i>.
 */
class JavaContextActionSupportTest extends TestBase {
	private static final String SRC_LambdaCalcExample = readSrc(LambdaCalcExample.class);
	private static final String SRC_LambdaExample = readSrc(LambdaExample.class);
	private static final String SRC_ClassWithLambda = readSrc(ClassWithLambda.class);

	@Test
	void resolvesEachLambdaBodyToItsSyntheticMethod() throws IOException {
		ClassMemberPathNode additionPath = resolveLambdaPath(SRC_LambdaCalcExample, LambdaCalcExample.class,
				"software/coley/recaf/test/dummy/LambdaCalcExample", "a + b");
		ClassMemberPathNode multiplicationPath = resolveLambdaPath(SRC_LambdaCalcExample, LambdaCalcExample.class,
				"software/coley/recaf/test/dummy/LambdaCalcExample", "a * b");

		// The two source lambdas must map to their corresponding javac-generated methods, not main().
		assertEquals("lambda$main$0", additionPath.getValue().getName());
		assertEquals("lambda$main$1", multiplicationPath.getValue().getName());
	}

	@Test
	void resolvesStatementLambdaBodyFromReportedExample() throws IOException {
		// We should be able to resolve the 'Runnable' lambda method at the 'println' position.
		//
		// public static void main(String[] args) {
		//   Runnable runnable = () -> System.out.println("lambda");
		//   runnable.run();
		// }
		ClassMemberPathNode path = resolveLambdaPath(SRC_LambdaExample, LambdaExample.class,
				"software/coley/recaf/test/dummy/LambdaExample", "println");
		assertEquals("lambda$main$0", path.getValue().getName());
	}

	@Test
	void resolvesLambdaMethodsWithoutEnclosingMethodName() {
		// Hacky mock setup that reproduces a sample where we had what seemed like a clean javac class
		// but the synthetic lambda method was missing the enclosing method portion of its name.
		JvmClassInfo classInfo = TestClassUtils.createClass("software/coley/recaf/test/dummy/UnnamedLambdaExample",
				node -> {
					MethodNode lambdaMethod = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "lambda$0", "()V", null, null);
					lambdaMethod.instructions.add(new InsnNode(RETURN));
					node.methods.add(lambdaMethod);
				});

		// Same source + test idea as the other test, but we will use the class definition above that has the 'lambda$0'.
		String source = """
				package software.coley.recaf.test.dummy;
				
				public class UnnamedLambdaExample {
				    public static void main(String[] args) {
				        Runnable runnable = () -> System.out.println("lambda");
				        runnable.run();
				    }
				}
				""";
		ClassMemberPathNode path = resolveLambdaPath(
				source, classInfo,
				"software/coley/recaf/test/dummy/UnnamedLambdaExample", "println");
		assertEquals("lambda$0", path.getValue().getName());
	}

	@Test
	void resolvesObfuscatedLambdaTargetFromInvokeDynamic() {
		// Similar mock setup as the previous test, but this time we are modeling the case where javac isn't used
		// or there is obfuscation that changes the lambda method name to break the normal 'lambda$<enclosing>$<index>' pattern.
		// When this occurs, we should fall back to reading the method bytecode and resolving the lambda target from the invokedynamic args.
		String owner = "software/coley/recaf/test/dummy/ObfuscatedLambdaExample";
		JvmClassInfo classInfo = TestClassUtils.createClass(owner, node -> {
			// Just call it "a" instead of the normal pattern.
			Handle implementation = new Handle(H_INVOKESTATIC, owner, "a", "()V", false);

			MethodNode main = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
			main.visitCode();
			main.visitInvokeDynamicInsn("run", "()Ljava/lang/Runnable;", Handles.META_FACTORY, Type.getMethodType("()V"), implementation, Type.getMethodType("()V"));
			main.visitInsn(POP);
			main.visitInsn(RETURN);
			main.visitMaxs(1, 1);
			main.visitEnd();
			node.methods.add(main);

			MethodNode lambdaMethod = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC, "a", "()V", null, null);
			lambdaMethod.instructions.add(new InsnNode(RETURN));
			node.methods.add(lambdaMethod);
		});

		// Again, same source but mocked name for the sake of binding to the mocked class model.
		String source = """
				package software.coley.recaf.test.dummy;
				
				public class ObfuscatedLambdaExample {
				    public static void main(String[] args) {
				        Runnable runnable = () -> System.out.println("lambda");
				        runnable.run();
				    }
				}
				""";
		ClassMemberPathNode path = resolveLambdaPath(source, classInfo, owner, "println");
		assertEquals("a", path.getValue().getName());
	}

	@Test
	void accountsForLambdasInEarlierMethods() throws IOException {
		// In the 'ClassWithLambda' there are multiple methods that contain lambdas.
		// The lambdas in the later methods should be numbered after the lambdas in the earlier methods
		// since the numbering is based on the order of the lambdas in the whole class, not per-method.
		ClassMemberPathNode runnablePath = resolveLambdaPath(SRC_ClassWithLambda, ClassWithLambda.class,
				"software/coley/recaf/test/dummy/ClassWithLambda", "System.out.println(\"foo\")");
		ClassMemberPathNode predicatePath = resolveLambdaPath(SRC_ClassWithLambda, ClassWithLambda.class,
				"software/coley/recaf/test/dummy/ClassWithLambda", "s.isBlank()");

		// The println is the first lambda, the isBlank is the third lambda, so they should be [0, 2] respectively.
		assertEquals("lambda$runnable$0", runnablePath.getValue().getName());
		assertEquals("lambda$predicate$2", predicatePath.getValue().getName());
	}

	private ClassMemberPathNode resolveLambdaPath(String source, Class<?> runtimeClass,
	                                              String className, String lambdaBody) throws IOException {
		return resolveLambdaPath(source, TestClassUtils.fromRuntimeClass(runtimeClass), className, lambdaBody);
	}

	private ClassMemberPathNode resolveLambdaPath(String source, JvmClassInfo classInfo,
	                                              String className, String lambdaBody) {
		// Build the workspace from compiled bytecode so synthetic methods and descriptors are available.
		Workspace workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(classInfo));
		ClassPathNode classPath = workspace.findClass(className);
		assertNotNull(classPath, "Failed to find class path for: " + className);

		// Parse the decompiler-style source and resolve the class context against its workspace path.
		AstService astService = recaf.get(AstService.class);
		CompilationUnitModel unit;
		synchronized (astService.getSharedJavaParser()) {
			unit = astService.getSharedJavaParser().parse(source);
		}
		ResolverAdapter resolver = astService.newJavaResolver(workspace, classPath, unit);
		ClassModel classModel = unit.getDeclaredClasses().stream()
				.filter(model -> model.getName().equals(classPath.getValue().getName().substring(className.lastIndexOf('/') + 1)))
				.findFirst()
				.orElseThrow();

		ClassMemberPathNode lambdaPath = JavaContextActionSupport.getEnclosingLambdaPath(resolver, classModel, source.indexOf(lambdaBody), classPath);
		assertNotNull(lambdaPath, "Failed to resolve lambda path for body: " + lambdaBody);
		return lambdaPath;
	}

	private static String readSrc(Class<?> clazz) {
		return readSrc(clazz.getName().replace('.', '/'));
	}

	private static String readSrc(String name) {
		try {
			Path path = Paths.get("src/testFixtures/java/" + name + ".java");
			if (Files.exists(path))
				return Files.readString(path);
			path = Paths.get("../recaf-core").toAbsolutePath().normalize().resolve("src/testFixtures/java/" + name + ".java");
			return Files.readString(path);
		} catch (IOException ex) {
			fail("Failed to read input : " + name, ex);
			throw new IllegalStateException();
		}
	}
}
