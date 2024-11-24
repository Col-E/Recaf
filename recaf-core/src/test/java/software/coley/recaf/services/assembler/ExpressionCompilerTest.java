package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.ClassWithFieldsAndMethods;
import software.coley.recaf.test.dummy.ClassWithInnerAndMembers;
import software.coley.recaf.test.dummy.ClassWithLambda;
import software.coley.recaf.test.dummy.ClassWithRequiredConstructor;
import software.coley.recaf.test.dummy.DummyEnum;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link ExpressionCompiler}
 */
class ExpressionCompilerTest extends TestBase {
	static Workspace workspace;
	static JvmClassInfo targetClass;
	static JvmClassInfo targetCtorClass;
	static JvmClassInfo targetEnum;
	static JvmClassInfo targetOuterWithInner;
	static JvmClassInfo targetClassWithLambda;

	@BeforeAll
	static void setup() throws IOException {
		targetClass = TestClassUtils.fromRuntimeClass(ClassWithFieldsAndMethods.class);
		targetCtorClass = TestClassUtils.fromRuntimeClass(ClassWithRequiredConstructor.class);
		targetEnum = TestClassUtils.fromRuntimeClass(DummyEnum.class);
		targetOuterWithInner = TestClassUtils.fromRuntimeClass(ClassWithInnerAndMembers.class);
		targetClassWithLambda = TestClassUtils.fromRuntimeClass(ClassWithLambda.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(targetClass, targetCtorClass, targetEnum));
		workspaceManager.setCurrent(workspace);
	}

	@Test
	void importSupport() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		ExpressionResult result = compile(assembler, """
				import java.util.Random;
				    
				try {
					Random random = new Random();
				 	int a = random.nextInt(100);
				 	int b = random.nextInt(100);
				 	System.out.println(a + " / " + b + " = " + (a/b));
				} catch (Exception ex) {
					System.out.println("Fail: " + ex);
				}
				""");
		assertSuccess(result);
	}

	@Test
	void classContext() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClass);
		ExpressionResult result = compile(assembler, """
				int localConst = CONST_INT;
				int localField = finalInt;
				int localMethod = plusTwo();
				int add = localConst + localField + localMethod;
				""");
		assertSuccess(result);
	}

	@Test
	void classContextWithRequiredCtor() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetCtorClass);
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void enumContext() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetEnum);
		ExpressionResult result = compile(assembler, """
				int i1 = ONE.ordinal();
				int i2 = TWO.ordinal();
				int i3 = THREE.ordinal();
				int add = i1 + i2 + i3;
				""");
		assertSuccess(result);
	}

	@Test
	void classAndMethodContextForParameters() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClass);
		assembler.setMethodContext(targetClass.getFirstDeclaredMethodByName("methodWithParameters"));
		ExpressionResult result = compile(assembler, """
				System.out.println(foo + ": " +
						Long.toHexString(wide) +
						"/" +
						Float.floatToIntBits(decimal) +
						" s=" + strings.get(0));
				""");
		assertSuccess(result);
	}

	@Test
	void classAndMethodContextForLocals() {
		// Tests that local variables are accessible to the expression compiler
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClass);
		assembler.setMethodContext(targetClass.getFirstDeclaredMethodByName("methodWithLocalVariables"));
		ExpressionResult result = compile(assembler, """
				out.println(message.contains("0") ? "Has zero" : "No zero found");
				""");
		assertSuccess(result);
	}

	@Test
	void classAndMethodContextForConstructor() {
		// Tests that the assembler works for constructor method contexts
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClass);
		assembler.setMethodContext(targetClass.getFirstDeclaredMethodByName("<init>"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void classAndMethodContextForStaticInitializer() {
		// Tests that the assembler works for static initializer method contexts
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetEnum);
		assembler.setMethodContext(targetEnum.getFirstDeclaredMethodByName("<clinit>"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void classWithInnerReferences() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetOuterWithInner);
		ExpressionResult result = compile(assembler, """
				TheInner inner = new TheInner();
				System.out.println("foo: " + foo);
				System.out.println("bar: " + inner.bar);
				inner.strings.add("something");
				inner.innerToOuter();
				""");
		assertSuccess(result);
	}

	@Test
	void ignoreTooOldTargetVersion() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setVersionTarget(1);
		ExpressionResult result = compile(assembler, """
				System.out.println("We do not compile against Java 1");
				""");
		assertSuccess(result);
	}

	@Test
	void ignoreNonExistingTypeForFields() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitField(ACC_PRIVATE, "foo", "Lfoo/Bar;", null, null); // Bogus field type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// The expression compiler should skip the field since it uses a type not in the workspace.
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(classInfo);
		assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void ignoreNonExistingTypeForMethodParams() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "foo", "(Lfoo/Bar;)V", null, null); // Bogus parameter type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// The expression compiler should skip the method since it uses a type not in the workspace.
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(classInfo);
		assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void ignoreNonExistingTypeForMethodReturns() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "foo", "()Lfoo/Bar;", null, null); // Bogus method parameter type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// The expression compiler should skip the method since it uses a type not in the workspace.
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(classInfo);
		assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void ignoreNonExistingTypeForMethodContext() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "methodName", "(Lfoo/Bar;)V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// The expression compiler should skip the method since it uses a type not in the workspace.
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(classInfo);
		assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void dontStubBogusInnerLikeMethodHandlesLookup() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClassWithLambda);
		ExpressionResult result = compile(assembler, """
				System.out.println("The stub should not reference the MethodHandles$Lookup synthetic inner class");
				""");
		assertSuccess(result);
	}

	@Test
	void errorLineIsOffsetToInputExpressionLineNumber() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetClass);
		assembler.setMethodContext(targetClass.getFirstDeclaredMethodByName("plusTwo"));
		String expression = """
				return "not-an-int";
				""";
		ExpressionResult result = compile(assembler, expression);

		// Should be a failure
		assertFalse(result.wasSuccess());
		assertNull(result.getException());

		// Should have an error on line 1 of our expression
		List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
		assertEquals(1, diagnostics.size());
		CompilerDiagnostic error = diagnostics.getFirst();
		assertEquals(1, error.line());
	}

	@Nested
	class ObfuscatedContexts {
		@ParameterizedTest
		@ValueSource(strings = {"void", "null", "int", "private", "throws", "", "\0", " ", "-10", "100", "<lol>"})
		void ignoreIllegalFieldName(String illegalFieldName) {
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
			cw.visitField(ACC_PRIVATE, illegalFieldName, "I", null, null);
			cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
			JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

			// The expression compiler should skip the field since it has an illegal name.
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			ExpressionResult result = compile(assembler, "");
			assertSuccess(result);
		}

		@ParameterizedTest
		@ValueSource(strings = {"void", "null", "int", "private", "throws", "", "\0", " ", "-10", "100", "<lol>"})
		void ignoreIllegalMethodName(String illegalMethodName) {
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
			cw.visitMethod(ACC_PRIVATE, illegalMethodName, "()I", null, null);
			cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
			JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

			// The expression compiler should skip the method since it has an illegal name.
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			ExpressionResult result = compile(assembler, "");
			assertSuccess(result);
		}

		@ParameterizedTest
		@ValueSource(strings = {"void", "null", "int", "private", "throws", "", "\0", " ", "-10", "100", "<lol>"})
		void ignoreIllegalMethodContextName(String illegalMethodName) {
			ClassWriter cw = new ClassWriter(0);
			cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
			Label start = new Label();
			Label end = new Label();

			MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, illegalMethodName, "(IIII)V", null, null);
			mv.visitCode();
			mv.visitLabel(start);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(IRETURN);
			mv.visitLabel(end);
			mv.visitEnd();
			mv.visitLocalVariable("one", "I", null, start, end, 1);
			mv.visitLocalVariable("two", "I", null, start, end, 2);
			mv.visitLocalVariable("three", "I", null, start, end, 3);
			mv.visitLocalVariable(illegalMethodName, "I", null, start, end, 4); // Add an illegal named parameter
			JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

			// The expression compiler should rename the obfuscated method specified as the context.
			// Variables passed in (that are not illegally named) and such should still be accessible.
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName(illegalMethodName));
			ExpressionResult result = compile(assembler, "int result = one + two + three;");
			assertSuccess(result);
		}
	}

	private static void assertSuccess(@Nonnull ExpressionResult result) {
		assertNull(result.getException(), "Exception thrown when compiling: " + result.getException());
		assertTrue(result.getDiagnostics().isEmpty(), "There were " + result.getDiagnostics().size() + " compiler messages");
		assertTrue(result.wasSuccess(), "Missing assembler output");
	}

	@Nonnull
	private static ExpressionResult compile(@Nonnull ExpressionCompiler assembler, @Nonnull String expressionResult) {
		ExpressionResult result = assembler.compile(expressionResult);
		List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
		diagnostics.forEach(System.out::println);
		ExpressionCompileException exception = result.getException();
		if (exception != null)
			fail(exception);
		String assembly = result.getAssembly();
		if (assembly != null)
			System.out.println(assembly);
		return result;
	}
}