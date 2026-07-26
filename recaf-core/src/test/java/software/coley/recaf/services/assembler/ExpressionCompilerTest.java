package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterAll;
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
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.compile.JavacCompilerConfig;
import software.coley.recaf.test.CompilerTestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.TestConfigSetup;
import software.coley.recaf.test.dummy.ClassWithFieldsAndMethods;
import software.coley.recaf.test.dummy.ClassWithInnerAndMembers;
import software.coley.recaf.test.dummy.ClassWithLambda;
import software.coley.recaf.test.dummy.ClassWithNestedInners;
import software.coley.recaf.test.dummy.ClassWithRequiredConstructor;
import software.coley.recaf.test.dummy.ClassWithToString;
import software.coley.recaf.test.dummy.DummyEnum;
import software.coley.recaf.test.dummy.DummyRecord;
import software.coley.recaf.test.dummy.SealedCircle;
import software.coley.recaf.test.dummy.SealedOtherShape;
import software.coley.recaf.workspace.model.Workspace;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Tests for {@link ExpressionCompiler}
 */
class ExpressionCompilerTest extends CompilerTestBase {
	static Workspace workspace;
	static JvmClassInfo targetClass;
	static JvmClassInfo targetCtorClass;
	static JvmClassInfo targetToStringClass;
	static JvmClassInfo targetEnum;
	static JvmClassInfo targetRecord;
	static JvmClassInfo targetOuterWithInner;
	static JvmClassInfo targetOuterWithNestedInners;
	static JvmClassInfo targetClassWithLambda;

	@BeforeAll
	static void setup() throws IOException {
		targetClass = TestClassUtils.fromRuntimeClass(ClassWithFieldsAndMethods.class);
		targetCtorClass = TestClassUtils.fromRuntimeClass(ClassWithRequiredConstructor.class);
		targetToStringClass = TestClassUtils.fromRuntimeClass(ClassWithToString.class);
		targetEnum = TestClassUtils.fromRuntimeClass(DummyEnum.class);
		targetRecord = TestClassUtils.fromRuntimeClass(DummyRecord.class);
		targetOuterWithInner = TestClassUtils.fromRuntimeClass(ClassWithInnerAndMembers.class);
		targetOuterWithNestedInners = TestClassUtils.fromRuntimeClass(ClassWithNestedInners.class);
		targetClassWithLambda = TestClassUtils.fromRuntimeClass(ClassWithLambda.class);
		workspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(targetClass, targetCtorClass, targetEnum));
		workspaceManager.setCurrent(workspace);

		// For this test, we want to have the javac compiler generate phantom classes for missing types.
		recaf.get(JavacCompilerConfig.class).getGeneratePhantoms().setValue(true);
	}

	@AfterAll
	static void teardown() {
		workspaceManager.setCurrent(null);

		// Trigger the test-default bean to load and restore javac phantom generation to its default value for tests.
		recaf.get(TestConfigSetup.class).configure();
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
	void recordContext() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetRecord);
		assembler.setMethodContext(targetRecord.getFirstDeclaredMethodByName("fooPlus"));
		ExpressionResult result = compile(assembler, """
				int plus = foo + other;
				int mul = foo * other;
				return String.valueOf(mul - plus);
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
		assertTrue(result.getAssembly().contains(".method static <clinit> ()V"));
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
	void classWithManyInnerReferences() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetOuterWithNestedInners);
		ExpressionResult result = compile(assembler, """
				// This just tests that the stubbing emits valid code.
				""");
		assertSuccess(result);
	}

	@Test
	void classWithOuterReferences() {
		compileFull("TopologyOuter", """
				public class TopologyOuter {
					public class Inner {
						public Inner context() {
							return null;
						}
				
						void foo() {}
					}
				
					void bar() {}
				}
				""");
		JvmClassInfo inner = get("TopologyOuter$Inner");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(inner);
		assembler.setMethodContext(inner.getFirstDeclaredMethodByName("context"));

		// We should be able to compile an expression inside the inner
		// class that calls all levels of the enclosing topology.
		ExpressionResult result = compile(assembler, "foo(); bar(); return null;");
		assertSuccess(result);
	}

	@Test
	void classWithStaticOuterReferences() {
		compileFull("StaticTopologyOuter", """
				public class StaticTopologyOuter {
					public static class Inner {
						public void context() {}
				
						void foo() {}
					}
				
					// Static outer method should be callable from the inner class
					static void bar() {}
				
					// Instance outer method cannot be called from the inner class since it has no instance reference.
					void baz() {}
				}
				""");
		JvmClassInfo inner = get("StaticTopologyOuter$Inner");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(inner);
		assembler.setMethodContext(inner.getFirstDeclaredMethodByName("context"));

		// Same idea as the previous test, but the inner class is static.
		ExpressionResult result = compile(assembler, "foo(); bar();");
		assertSuccess(result);

		// Our stubbing should also maintain static/instance context rules, so the following should fail.
		result = compile(assembler, "baz();");
		assertFalse(result.wasSuccess());
		String message = result.getDiagnostics().getFirst().message();
		assertTrue(message.contains("non-static method baz() cannot be referenced from a static context"));
	}

	@Test
	void classWithMissingOuterFallsBackToBinaryNameStub() {
		// Source form:
		/*
		public class MissingOuter {
			public class Middle {
				public class Inner {
					public void context() {}
				}
			}
		}
		 */
		// Create the middle and inner classes, but leave out the outer class.
		// The expression compiler should be able to compile an expression in the inner class,
		// and it should stub the missing outer class.
		JvmClassInfo middle = asmClass("MissingOuter$Middle", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> cw.visitInnerClass("MissingOuter$Middle$Inner", "MissingOuter$Middle", "Inner", ACC_PUBLIC));
		JvmClassInfo inner = asmClass("MissingOuter$Middle$Inner", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> {
					cw.visitInnerClass("MissingOuter$Middle$Inner", "MissingOuter$Middle", "Inner", ACC_PUBLIC);
					cw.visitMethod(ACC_PUBLIC, "context", "()V", null, null);
				});

		// We don't really expose the underlying stub source, so we can just check if this is successful.
		ExpressionResult result = compileInWorkspace(middle, inner, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(inner);
			assembler.setMethodContext(inner.getFirstDeclaredMethodByName("context"));
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void nestedContextCanResolveSiblingNestedTypes() {
		compileFull("NestedSiblingContext", """
				public class NestedSiblingContext {
					static class Dependency {}
				
					static class Target {
						void context(Dependency dependency) {}
					}
				}
				""");
		JvmClassInfo target = get("NestedSiblingContext$Target");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(target);
		assembler.setMethodContext(target.getFirstDeclaredMethodByName("context"));

		// When we compile in the context method, the expression compiler needs to be able to also
		// generate stubbing for the adjacent dependency inner class.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void anonymousContextCanResolvePrivateNestedTypes() {
		compileFull("AnonymousPrivateOuter", """
				public class AnonymousPrivateOuter {
					private static class Hidden {}
				
					interface Action {
						Hidden get();
					}
				
					Action action = new Action() {
						@Override
						public Hidden get() {
							return null;
						}
					};
				}
				""");
		JvmClassInfo anonymous = get("AnonymousPrivateOuter$1");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(anonymous);
		assembler.setMethodContext(anonymous.getFirstDeclaredMethodByName("get"));

		// When we compile in the 'get()' method inside the anonymous class the expression compiler
		// should be able to resolve generate an appropriate stub to let us still operate in the
		// intended context.
		ExpressionResult result = compile(assembler, "return null;");
		assertSuccess(result);
	}

	@Test
	void detachedEnumSwitchContextRetainsEnumConstants() {
		compileFull("DetachedEnumSwitch", """
				public enum DetachedEnumSwitch {
					FIRST, SECOND;
				
					static Object value = new Object() {
						static int state;
						static { state = 1; }
					};
				}
				""");
		JvmClassInfo synthetic = get("DetachedEnumSwitch$1");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(synthetic);
		assembler.setMethodContext(synthetic.getDeclaredMethod("<clinit>", "()V"));

		// TODO: A further improvement would be letting the compiler emit a structure that lets us drop qualified name access to the enum consts.

		// When we compile in the static initializer of the anonymous inner class,
		// the expression compiler should be able to resolve the enum constants.
		ExpressionResult result = compile(assembler, "String s = DetachedEnumSwitch.FIRST.name(); s = DetachedEnumSwitch.SECOND.name();");
		assertSuccess(result);
	}

	@Test
	void interfaceFieldsAreInitializedInSourceStubs() {
		compileFull("InterfaceFields", """
				public interface InterfaceFields {
					int INTEGER = 1;
					boolean BOOLEAN = true;
					String TEXT = "text";
					Object OBJECT = null;
				
					void context();
				}
				""");
		JvmClassInfo iface = get("InterfaceFields");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(iface);
		assembler.setMethodContext(iface.getDeclaredMethod("context", "()V"));

		// When we compile in the interface context with static final fields, the expression compiler
		// should be able to emit stubbing that retains their initialization (otherwise they wouldn't be valid).
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void detachedAnonymousConstructorKeepsSyntheticOuterParameter() {
		compileFull("DetachedAnonymousConstructor", """
				public class DetachedAnonymousConstructor {
					Runnable action = new Runnable() {
						@Override
						public void run() {}
					};
				}
				""");
		JvmClassInfo anonymous = get("DetachedAnonymousConstructor$1");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(anonymous);
		assembler.setMethodContext(anonymous.getDeclaredMethod("<init>", "(LDetachedAnonymousConstructor;)V"));

		// TODO: We currently just validate that this doesn't fail with an empty expression.
		//  The stubbing places the anonymous class as a detached class with the same constructor signature as the source.
		//  This doesn't let us call outer methods sadly...
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void staticNestedConstructorKeepsOrdinaryParameters() {
		// Source form:
		/*
		public class StaticCtorOuter {
			public static class Inner {
				public Inner(StaticCtorOuter outer, int value) {} // Manual passing of outer class
			}
		}
		 */
		JvmClassInfo outer = asmClass("StaticCtorOuter", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> cw.visitInnerClass("StaticCtorOuter$Inner", "StaticCtorOuter", "Inner", ACC_PUBLIC | ACC_STATIC));
		JvmClassInfo inner = asmClass("StaticCtorOuter$Inner", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> {
					cw.visitInnerClass("StaticCtorOuter$Inner", "StaticCtorOuter", "Inner", ACC_PUBLIC | ACC_STATIC);
					cw.visitMethod(ACC_PUBLIC, "<init>", "(LStaticCtorOuter;I)V", null, null);
				});

		// We have some logic in the expression compiler that drops the synthetic outer parameter
		// for non-static inner classes, but static inner classes should keep their parameters.
		ExpressionResult result = compileInWorkspace(outer, inner, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(outer);
			return compile(assembler, "new Inner(null, 1);");
		});
		assertSuccess(result);
	}

	@Test
	void nonStaticNestedConstructorDropsOnlySyntheticOuterParameter() {
		compileFull("InnerCtorOuter", """
				public class InnerCtorOuter {
					public class Inner {
						public Inner(int value, String text) {}
					}
				}
				""");
		JvmClassInfo outer = get("InnerCtorOuter");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(outer);

		// The inner class constructor has a synthetic outer parameter,
		// but the expression compiler should drop it and only keep the user-defined parameters.
		ExpressionResult result = compile(assembler, "new Inner(1, \"value\");");
		assertSuccess(result);
	}

	@Test
	void parentConstructorUsesTypedReferenceDefaults() {
		compileFull("OverloadedChild", """
				class OverloadedParent {
					public OverloadedParent(String value) {}
					public OverloadedParent(Integer value) {}
				}
				
				public class OverloadedChild extends OverloadedParent {
					public OverloadedChild() {
						super((String) null);
					}
				}
				""");
		JvmClassInfo child = get("OverloadedChild");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(child);

		// When we compile in the context of a child class, the expression compiler
		// should be able to resolve the parent constructor reference and disambiguate the potential overloads.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void generatedConstructorAllowsCheckedParentExceptions() {
		compileFull("CheckedParentChild", """
				public class CheckedParentChild extends java.io.FileInputStream {
					public CheckedParentChild(String path) throws java.io.FileNotFoundException {
						super(path);
					}
				
					public void context() {}
				}
				""");
		JvmClassInfo child = get("CheckedParentChild");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(child);
		assembler.setMethodContext(child.getFirstDeclaredMethodByName("context"));

		// The class extends FIS, which has a checked exception on its constructor.
		// The expression compiler should figure things out and let us operate in the context method in peace.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void missingConstructorParameterGetsSafeSyntheticConstructor() {
		// Source form:
		/*
		public class SyntheticParent {
			public SyntheticParent(int value) {}
		}
		public class SyntheticChild extends SyntheticParent {
			public SyntheticChild(missing.Parameter param) {}
		}
		 */
		JvmClassInfo parent = asmClass("SyntheticParent", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> cw.visitMethod(ACC_PUBLIC, "<init>", "(I)V", null, null));
		JvmClassInfo child = asmClass("SyntheticChild", ACC_PUBLIC, "SyntheticParent", null, null,
				cw -> cw.visitMethod(ACC_PUBLIC, "<init>", "(Lmissing/Parameter;)V", null, null));

		// The child class has a constructor that takes a parameter of a type that doesn't exist in the workspace.
		// We should be able to recover by dropping the missing parameter and generating a synthetic constructor
		// that calls the parent constructor.
		ExpressionResult result = compileInWorkspace(parent, child, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(child);
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void missingConstructorParameterDoesNotDuplicateExistingNoArgConstructor() {
		// Source form:
		/*
		public class SyntheticWithNoArg {
			SyntheticWithNoArg() {}
			SyntheticWithNoArg(missing.Parameter param) {}
			void context() {}
		}
		 */
		JvmClassInfo child = asmClass("fixtures/SyntheticWithNoArg", ACC_PUBLIC, "java/lang/Object", null, null,
				cw -> {
					cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
					cw.visitMethod(ACC_PUBLIC, "<init>", "(Lmissing/Parameter;)V", null, null);
					cw.visitMethod(ACC_PUBLIC, "context", "()V", null, null);
				});

		// The child class has a constructor that takes a parameter of a type that doesn't exist in the workspace.
		// We should be able to recover by dropping it and keeping the existing no-arg constructor in our stubbing.
		ExpressionResult result = compileInWorkspace(child, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(child);
			assembler.setMethodContext(child.getFirstDeclaredMethodByName("context"));
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void interfaceExpressionMethodIsDefault() {
		compileFull("ExpressionInterface", """
				public interface ExpressionInterface {
					void run();
				}
				""");
		JvmClassInfo iface = get("ExpressionInterface");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(iface);
		assembler.setMethodContext(iface.getFirstDeclaredMethodByName("run"));

		// When we compile in the context of an interface method, the expression compiler should treat it as a default method
		// so that there can be a method body.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void workspaceOverrideDoesNotDeclareThrowable() {
		compileFull("ExpressionImplementation", """
				interface ExpressionInterface {
					void run();
				}
				
				public class ExpressionImplementation implements ExpressionInterface {
					@Override
					public void run() {}
				
					void context() {}
				}
				""");
		JvmClassInfo impl = get("ExpressionImplementation");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(impl);
		assembler.setMethodContext(impl.getFirstDeclaredMethodByName("run"));

		// When we compile in the context of a class that implements an interface method,
		// the expression compiler should not declare any checked exceptions on the method.
		//
		// Normally we do this so users can write code that throws checked exceptions without
		// bothering to try-catch. But in this case we can't do that. So a normal non-throwing
		// expression should pass, but a throwing one must be enclosed in a try-catch.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);

		// Without a try-catch, we fail here.
		result = compile(assembler, "throw new Exception();");
		assertFalse(result.wasSuccess());

		// With a try-catch, we succeed.
		result = compile(assembler, """
				try {
					throw new Exception();
				} catch (Exception ex) {}
				""");
		assertSuccess(result);

		// To reiterate, if we aren't bound by an inherited method contract then we can write
		// code that throws and not bother writing a try-catch.
		assembler.setMethodContext(impl.getFirstDeclaredMethodByName("context"));
		result = compile(assembler, "throw new Exception();");
		assertSuccess(result);
	}

	@Test
	void covariantBridgeTargetDoesNotCollideWithSibling() {
		compileFull("CovariantBridge", """
				interface ValueProvider {
					Object value();
				}
				
				public class CovariantBridge implements ValueProvider {
					@Override
					public String value() {
						return "";
					}
				}
				""");
		JvmClassInfo bridge = get("CovariantBridge");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(bridge);
		assembler.setMethodContext(bridge.getMethods().stream()
				.filter(method -> method.getName().equals("value"))
				.filter(method -> method.hasBridgeModifier()).findFirst().orElseThrow());

		// The covariant bridge method should not lead to stubbing collision with the sibling method,
		// so we should be able to compile an expression in its context.
		ExpressionResult result = compile(assembler, "return null;");
		assertSuccess(result);

		// Additionally, the sibling method (the source one with the String return value) should also work too.
		assembler.setMethodContext(bridge.getMethods().stream()
				.filter(method -> method.getName().equals("value"))
				.filter(method -> !method.hasBridgeModifier()).findFirst().orElseThrow());
		result = compile(assembler, "return \"hello\";");
		assertSuccess(result);
	}

	@Test
	void genericParameterBridgeDoesNotClashWithInheritedComparableMethod() {
		compileFull("GenericFileBridge", """
				public class GenericFileBridge extends java.io.File {
					public GenericFileBridge(String path) {
						super(path);
					}
				
					@Override
					public int compareTo(java.io.File pathname) {
						return 0;
					}
				
					public void context() {}
				}
				""");
		JvmClassInfo bridge = get("GenericFileBridge");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(bridge);
		assembler.setMethodContext(bridge.getFirstDeclaredMethodByName("context"));

		// File implements Comparable<File>.
		// When we compile our subtype with the overridden 'compareTo' method we will have two methods:
		//  - compareTo(File) - the source method
		//  - compareTo(Object) - the bridge method
		// The expression compiler should be able to handle this situation and not have the two methods collide in the stubbing.
		// First, we'll validate we can build an expression in the 'context' method.
		ExpressionResult result = compile(assembler, "");
		assertSuccess(result);

		// Next, we'll validate we can build an expression in both 'compareTo' methods.
		for (MethodMember method : bridge.getMethods()) {
			if (method.getName().equals("compareTo")) {
				assembler.setMethodContext(method);
				result = compile(assembler, "return 0;");
				assertSuccess(result);
				if (method.hasBridgeModifier())
					assertTrue(result.getAssembly().contains(".method public compareTo (Ljava/lang/Object;)I"));
			}
		}

		// Lastly, we'll validate expression building in the constructor generates an implicit super() call.
		// We will check this by having the expression also generate a super() call which should fail since
		// you can only have one super() call in a constructor.
		assembler.setMethodContext(bridge.getFirstDeclaredMethodByName("<init>"));
		result = compile(assembler, "super(path);");
		assertFalse(result.wasSuccess());
		assertTrue(result.getDiagnostics().getFirst().message().contains("redundant explicit constructor invocation"));

		// The constructor expression should work if we don't supply a redundant super() call.
		result = compile(assembler, "");
		assertSuccess(result);
	}

	@Test
	void incompatibleInheritedReturnMethodsUseOriginalJasmNames() {
		// Source form:
		/*
		public abstract class InvalidReturnChannel extends java.nio.channels.FileChannel {
			@Override
			public abstract java.nio.channels.SeekableByteChannel position(long newPosition);

			@Override
			public abstract java.nio.channels.SeekableByteChannel truncate(long size);
		}
		 */
		// Note, the above is invalid in source because:
		// - FileChannel.position(long) returns FileChannel
		// - FileChannel.truncate(long) returns FileChannel
		// However since the return types are different this is fine in bytecode.
		JvmClassInfo invalidReturn = asmClass("InvalidReturnChannel", ACC_PUBLIC | ACC_ABSTRACT,
				"java/nio/channels/FileChannel", null, null, cw -> {
					cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "position", "(J)Ljava/nio/channels/SeekableByteChannel;", null, null);
					cw.visitMethod(ACC_PUBLIC | ACC_ABSTRACT, "truncate", "(J)Ljava/nio/channels/SeekableByteChannel;", null, null);
				});

		// Our expression context has two methods that are fine in bytecode, but invalid in source.
		// We should be able to compile expressions in the context of both methods.
		// The expression compiler should alias our method context to prevent collisions with the parent FileChannel contracts.
		ExpressionResult result = compileInWorkspace(invalidReturn, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(invalidReturn);
			assembler.setMethodContext(invalidReturn.getDeclaredMethod("position", "(J)Ljava/nio/channels/SeekableByteChannel;"));
			return compile(assembler, "throw new RuntimeException();");
		});
		assertSuccess(result);
		assertTrue(result.getAssembly().contains(".method public position (J)Ljava/nio/channels/SeekableByteChannel;"));
		result = compileInWorkspace(invalidReturn, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(invalidReturn);
			assembler.setMethodContext(invalidReturn.getDeclaredMethod("truncate", "(J)Ljava/nio/channels/SeekableByteChannel;"));
			return compile(assembler, "throw new RuntimeException();");
		});
		assertSuccess(result);
		assertTrue(result.getAssembly().contains(".method public truncate (J)Ljava/nio/channels/SeekableByteChannel;"));
	}

	@Test
	void debugLocalsDoNotCollideWithExistingSourceOverload() {
		// Source form:
		/*
		public class UnsafeOverloadCollision {
			public static void memcpy(long address, java.nio.ByteBuffer buffer, int length) {
				int extra; // not used in code, but defined in debug info.
			}
		}
		 */
		JvmClassInfo unsafe = asmClass("UnsafeOverloadCollision", ACC_PUBLIC, "java/lang/Object", null, null, cw -> {
			// The method we'll use as the expression context has a signature that has 3 parameters.
			Label start = new Label();
			Label end = new Label();
			MethodVisitor context = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "memcpy", "(JLjava/nio/ByteBuffer;I)V", null, null);
			context.visitCode();
			context.visitLabel(start);
			context.visitInsn(RETURN);
			context.visitLabel(end);
			context.visitLocalVariable("extra", "I", null, start, end, 4);
			context.visitMaxs(0, 5);
			context.visitEnd();

			// The other method in this class will have an extra int, which will conflict if we naively stub the expression context method
			// to expand the number of arguments to include local variables (the trick we use to let users write expressions that reference local variables).
			cw.visitMethod(ACC_PRIVATE | ACC_STATIC, "memcpy", "(JLjava/nio/ByteBuffer;II)V", null, null).visitEnd();
		});

		// The expression compiler should be able to compile in the context of the public 'memcpy' method
		// and not collide with the second memcpy overload that has an extra int parameter.
		ExpressionResult result = compileInWorkspace(unsafe, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(unsafe);
			assembler.setMethodContext(unsafe.getDeclaredMethod("memcpy", "(JLjava/nio/ByteBuffer;I)V"));
			return compile(assembler, "throw new RuntimeException();");
		});
		assertSuccess(result);
	}

	@Test
	void nestedChildConstructorDropsInheritedParentOuterParameter() {
		compileFull("NestedConstructorSubtype", """
				class NestedConstructorParent {
					// Will have synthetic 'this' of NestedConstructorParent
					class Parent {
						Parent(String value) {}
					}
				}
				
				public class NestedConstructorSubtype extends NestedConstructorParent {
					// Will have synthetic 'this' of NestedConstructorSubtype.
					class Child extends Parent {
						Child() { super(""); }
					}
				}
				""");
		JvmClassInfo child = get("NestedConstructorSubtype$Child");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(child);
		assembler.setMethodContext(child.getFirstDeclaredMethodByName("<init>"));

		// When we compile in the context of the subtype child constructor,
		// the expression compiler should drop the inherited synthetic outer parameter from the parent constructor
		// and only keep the synthetic outer parameter of the child constructor.
		assertSuccess(compile(assembler, ""));
	}

	@Test
	void detachedNestedAnonymousContextUsesBinaryTopLevelName() {
		// Anonymous classes follow the pattern 'Outer$N' where N is an incrementing integer.
		// For multiple nested anonymous classes the pattern continues as 'Outer$N$M'.
		compileFull("DeepAnonymousContext", """
				public class DeepAnonymousContext {
					Object value = new Object() {
						Object nested = new Object() {
							void context() {}
						};
					};
				}
				""");
		JvmClassInfo nested = get("DeepAnonymousContext$1$1");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(nested);
		assembler.setMethodContext(nested.getFirstDeclaredMethodByName("context"));

		// TODO: Ideally we could have methods in each level of the class topology, but for now
		//  we're just supporting the deepest level of the nested anonymous class. We don't have
		//  a great way to expose the outer anonymous classes to the expression compiler...
		assertSuccess(compile(assembler, ""));
	}

	@Test
	void constructorInvocationDetectionIgnoresCommentsAndNestedTypes() {
		compileFull("CommentedConstructor", """
				class RequiredConstructorParent {
					RequiredConstructorParent(int value) {}
				}
				
				public class CommentedConstructor extends RequiredConstructorParent {
					public CommentedConstructor() {
						super(1);
					}
				}
				""");
		JvmClassInfo child = get("CommentedConstructor");
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(child);
		assembler.setMethodContext(child.getFirstDeclaredMethodByName("<init>"));

		// The expression compiler should be able to emit a prefix constructor call before our expression
		// in order to satisfy the parent constructor contract. This will let the user write a normal expression
		// without needing to do the super/this call themselves.
		ExpressionResult result = compile(assembler, """
				class LocalType extends RuntimeException {}
				throw new LocalType();
				""");
		assertSuccess(result);
	}

	@Test
	void sealedChild() throws IOException {
		// Tests basic support for compiling within a child of a sealed type.
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(TestClassUtils.fromRuntimeClass(SealedOtherShape.class));
		ExpressionResult result = compile(assembler, """
				System.out.println("area: " + area());
				""");
		assertSuccess(result);

		// Same test but in a record child.
		assembler.setClassContext(TestClassUtils.fromRuntimeClass(SealedCircle.class));
		result = compile(assembler, """
				System.out.println("area: " + area());
				""");
		assertSuccess(result);
	}

	@Test
	void overrideLibraryMethodDoesNotFail() {
		ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
		assembler.setClassContext(targetToStringClass);
		assembler.setMethodContext(targetToStringClass.getFirstDeclaredMethodByName("toString"));
		ExpressionResult result = compile(assembler, """
				return "string";
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
	void phantomTypeForFields() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitField(ACC_PRIVATE, "foo", "Lfoo/Bar;", null, null); // Bogus field type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// When we compile in this context, the bogus field type should be generated as a phantom by the compiler step.
		ExpressionResult result = compileInWorkspace(classInfo, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void phantomTypeForMethodParams() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "foo", "(Lfoo/Bar;)V", null, null); // Bogus parameter type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// When we compile in this context, the bogus method type should be generated as a phantom by the compiler step.
		ExpressionResult result = compileInWorkspace(classInfo, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void phantomTypeForMethodReturns() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "foo", "()Lfoo/Bar;", null, null); // Bogus method parameter type
		cw.visitMethod(ACC_PRIVATE, "methodName", "()V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// When we compile in this context, the bogus method type should be generated as a phantom by the compiler step.
		ExpressionResult result = compileInWorkspace(classInfo, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			return compile(assembler, "");
		});
		assertSuccess(result);
	}

	@Test
	void phantomTypeForMethodContext() {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, ACC_PUBLIC, "ExampleClass", null, "java/lang/Object", null);
		cw.visitMethod(ACC_PRIVATE, "methodName", "(Lfoo/Bar;)V", null, null);
		JvmClassInfo classInfo = new JvmClassInfoBuilder(cw.toByteArray()).build();

		// When we compile in this context, the bogus method type should be generated as a phantom by the compiler step.
		ExpressionResult result = compileInWorkspace(classInfo, () -> {
			ExpressionCompiler assembler = recaf.get(ExpressionCompiler.class);
			assembler.setClassContext(classInfo);
			assembler.setMethodContext(classInfo.getFirstDeclaredMethodByName("methodName"));
			return compile(assembler, "");
		});
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

	@Nonnull
	private static JvmClassInfo asmClass(@Nonnull String name, int access, @Nonnull String superName,
	                                     String[] interfaces, String signature, @Nonnull Consumer<ClassWriter> content) {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_8, access, name, signature, superName, interfaces);
		content.accept(cw);
		cw.visitEnd();
		return new JvmClassInfoBuilder(cw.toByteArray()).build();
	}

	@Nonnull
	private static ExpressionResult compileInWorkspace(@Nonnull JvmClassInfo classInfo,
	                                                   @Nonnull Supplier<ExpressionResult> action) {
		return compileInWorkspace(new JvmClassInfo[]{classInfo}, action);
	}

	@Nonnull
	private static ExpressionResult compileInWorkspace(@Nonnull JvmClassInfo first, @Nonnull JvmClassInfo second,
	                                                   @Nonnull Supplier<ExpressionResult> action) {
		return compileInWorkspace(new JvmClassInfo[]{first, second}, action);
	}

	@Nonnull
	private static ExpressionResult compileInWorkspace(@Nonnull JvmClassInfo[] classes,
	                                                   @Nonnull Supplier<ExpressionResult> action) {
		Workspace prior = workspaceManager.getCurrent();
		workspaceManager.setCurrent(TestClassUtils.fromBundle(TestClassUtils.fromClasses(classes)));
		try {
			return action.get();
		} finally {
			workspaceManager.setCurrent(prior);
		}
	}

	private static void assertSuccess(@Nonnull ExpressionResult result) {
		List<CompilerDiagnostic> diagnostics = result.getDiagnostics();
		for (CompilerDiagnostic diagnostic : diagnostics)
			System.err.println(diagnostic);
		assertNull(result.getException(), "Exception thrown when compiling: " + result.getException());
		assertTrue(diagnostics.isEmpty(), "There were " + diagnostics.size() + " compiler messages");
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
