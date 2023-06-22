package software.coley.recaf.info.member;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.*;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.reflect.Modifier.*;

/**
 * Tests for {@link MethodMember}
 */
class MethodMemberTest {
	static JvmClassInfo arrayList;
	static JvmClassInfo annotationImpl;
	static JvmClassInfo accessibleMethods;
	static MethodMember acc_publicMethod;
	static MethodMember acc_privateMethod;
	static MethodMember acc_protectedMethod;
	static MethodMember acc_packageMethod;
	static JvmClassInfo variedMethods;
	static MethodMember var_staticMethod;
	static MethodMember var_finalMethod;
	static MethodMember var_synchronizedMethod;
	static MethodMember var_nativeMethod;
	static MethodMember var_abstractMethod;
	static MethodMember var_varargsMethod;
	static JvmClassInfo classWithLambda;

	@BeforeAll
	static void setup() throws IOException {
		arrayList = TestClassUtils.fromRuntimeClass(ArrayList.class);
		annotationImpl = TestClassUtils.fromRuntimeClass(AnnotationImpl.class);
		annotationImpl = TestClassUtils.fromRuntimeClass(ClassWithAnonymousInner.class);

		accessibleMethods = TestClassUtils.fromRuntimeClass(AccessibleMethods.class);
		acc_publicMethod = accessibleMethods.getDeclaredMethod("publicMethod", "()V");
		acc_privateMethod = accessibleMethods.getDeclaredMethod("privateMethod", "()V");
		acc_protectedMethod = accessibleMethods.getDeclaredMethod("protectedMethod", "()V");
		acc_packageMethod = accessibleMethods.getDeclaredMethod("packageMethod", "()V");

		variedMethods = TestClassUtils.fromRuntimeClass(VariedModifierMethods.class);
		var_staticMethod = variedMethods.getDeclaredMethod("staticMethod", "()V");
		var_finalMethod = variedMethods.getDeclaredMethod("finalMethod", "()V");
		var_synchronizedMethod = variedMethods.getDeclaredMethod("synchronizedMethod", "()V");
		var_nativeMethod = variedMethods.getDeclaredMethod("nativeMethod", "()V");
		var_abstractMethod = variedMethods.getDeclaredMethod("abstractMethod", "()V");
		var_varargsMethod = variedMethods.getDeclaredMethod("varargsMethod", "([Ljava/lang/String;)V");

		classWithLambda = TestClassUtils.fromRuntimeClass(ClassWithLambda.class);
	}

	@Test
	void getDeclaringClass() {
		// When a class-info is created all of its fields should be made aware of the declaring class
		for (MethodMember method : arrayList.getMethods()) {
			assertEquals(arrayList, method.getDeclaringClass(), "Method not linked with declaring class");
		}
	}

	@Test
	void isDeclarationAware() {
		// When a class-info is created all of its fields should be made aware of the declaring class
		for (MethodMember method : arrayList.getMethods()) {
			assertTrue(method.isDeclarationAware(), "Method not linked with declaring class");
		}
	}

	@Test
	void hasPublicModifier() {
		assertTrue(acc_publicMethod.hasPublicModifier());
		assertFalse(acc_privateMethod.hasPublicModifier());
		assertFalse(acc_protectedMethod.hasPublicModifier());
		assertFalse(acc_packageMethod.hasPublicModifier());
	}

	@Test
	void hasProtectedModifier() {
		assertFalse(acc_publicMethod.hasProtectedModifier());
		assertFalse(acc_privateMethod.hasProtectedModifier());
		assertTrue(acc_protectedMethod.hasProtectedModifier());
		assertFalse(acc_packageMethod.hasProtectedModifier());
	}

	@Test
	void hasPrivateModifier() {
		assertFalse(acc_publicMethod.hasPrivateModifier());
		assertTrue(acc_privateMethod.hasPrivateModifier());
		assertFalse(acc_protectedMethod.hasPrivateModifier());
		assertFalse(acc_packageMethod.hasPrivateModifier());
	}

	@Test
	void hasPackagePrivateModifier() {
		assertFalse(acc_publicMethod.hasPackagePrivateModifier());
		assertFalse(acc_privateMethod.hasPackagePrivateModifier());
		assertFalse(acc_protectedMethod.hasPackagePrivateModifier());
		assertTrue(acc_packageMethod.hasPackagePrivateModifier());
	}

	@Test
	void hasStaticModifier() {
		assertTrue(var_staticMethod.hasStaticModifier());
		assertFalse(var_finalMethod.hasStaticModifier());
		assertFalse(var_synchronizedMethod.hasStaticModifier());
		assertFalse(var_nativeMethod.hasStaticModifier());
		assertFalse(var_abstractMethod.hasStaticModifier());
		assertFalse(var_varargsMethod.hasStaticModifier());
	}

	@Test
	void hasFinalModifier() {
		assertFalse(var_staticMethod.hasFinalModifier());
		assertTrue(var_finalMethod.hasFinalModifier());
		assertFalse(var_synchronizedMethod.hasFinalModifier());
		assertFalse(var_nativeMethod.hasFinalModifier());
		assertFalse(var_abstractMethod.hasFinalModifier());
		assertFalse(var_varargsMethod.hasFinalModifier());
	}

	@Test
	void hasSynchronizedModifier() {
		assertFalse(var_staticMethod.hasSynchronizedModifier());
		assertFalse(var_finalMethod.hasSynchronizedModifier());
		assertTrue(var_synchronizedMethod.hasSynchronizedModifier());
		assertFalse(var_nativeMethod.hasSynchronizedModifier());
		assertFalse(var_abstractMethod.hasSynchronizedModifier());
		assertFalse(var_varargsMethod.hasSynchronizedModifier());
	}

	@Test
	void hasVolatileModifier() {
		// Not allowed on methods
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.hasVolatileModifier());
		}
	}

	@Test
	void hasTransientModifier() {
		// Not allowed on methods
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.hasTransientModifier());
		}
	}

	@Test
	void hasNativeModifier() {
		assertFalse(var_staticMethod.hasNativeModifier());
		assertFalse(var_finalMethod.hasNativeModifier());
		assertFalse(var_synchronizedMethod.hasNativeModifier());
		assertTrue(var_nativeMethod.hasNativeModifier());
		assertFalse(var_abstractMethod.hasNativeModifier());
		assertFalse(var_varargsMethod.hasNativeModifier());
	}

	@Test
	void hasEnumModifier() {
		// Not allowed on methods
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.hasEnumModifier());
		}
	}

	@Test
	void hasAnnotationModifier() {
		// Not allowed on methods
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.hasEnumModifier());
		}
	}

	@Test
	void hasInterfaceModifier() {
		// Not allowed on methods
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.hasEnumModifier());
		}
	}

	@Test
	void hasAbstractModifier() {
		assertFalse(var_staticMethod.hasAbstractModifier());
		assertFalse(var_finalMethod.hasAbstractModifier());
		assertFalse(var_synchronizedMethod.hasAbstractModifier());
		assertFalse(var_nativeMethod.hasAbstractModifier());
		assertTrue(var_abstractMethod.hasAbstractModifier());
		assertFalse(var_varargsMethod.hasAbstractModifier());
	}

	@Test
	void hasStrictFpModifier() {
		// There are no true asserts because the runtime class compiles with the project's Java version.
		// Since Java 17 'strictfp' is no longer emitted in bytecode since all operations are now strict.
		assertFalse(var_staticMethod.hasStrictFpModifier());
		assertFalse(var_finalMethod.hasStrictFpModifier());
		assertFalse(var_synchronizedMethod.hasStrictFpModifier());
		assertFalse(var_nativeMethod.hasStrictFpModifier());
		assertFalse(var_abstractMethod.hasStrictFpModifier());
		assertFalse(var_varargsMethod.hasStrictFpModifier());
	}

	@Test
	void hasVarargsModifier() {
		assertFalse(var_staticMethod.hasVarargsModifier());
		assertFalse(var_finalMethod.hasVarargsModifier());
		assertFalse(var_synchronizedMethod.hasVarargsModifier());
		assertFalse(var_nativeMethod.hasVarargsModifier());
		assertFalse(var_abstractMethod.hasVarargsModifier());
		assertTrue(var_varargsMethod.hasVarargsModifier());
	}

	@Test
	@Disabled("Bridge methods do not generate in Java 11+")
	void hasBridgeModifier() {
		// Lambda generates synthetic methods, but they are not marked as bridge
		for (MethodMember method : classWithLambda.getMethods()) {
			assertFalse(method.hasBridgeModifier());
		}

		// need to find an existing class with bridge example
	}

	@Test
	void hasSyntheticModifier() {
		int lambda = 0;
		for (MethodMember method : classWithLambda.getMethods()) {
			if (method.getName().startsWith("lambda$")) {
				lambda++;
				assertTrue(method.hasSyntheticModifier());
			} else {
				assertFalse(method.hasSyntheticModifier());
			}
		}
		assertEquals(3, lambda, "ClassWithLambda defines 3 lambdas");
	}

	@Test
	void isCompilerGenerated() {
		int lambda = 0;
		for (MethodMember method : classWithLambda.getMethods()) {
			if (method.getName().startsWith("lambda$")) {
				lambda++;
				assertTrue(method.isCompilerGenerated());
			} else {
				assertFalse(method.isCompilerGenerated());
			}
		}
		assertEquals(3, lambda, "ClassWithLambda defines 3 lambdas");
	}

	@Test
	void hasModifierMask() {
		assertTrue(var_staticMethod.hasModifierMask(STATIC));
		assertFalse(var_staticMethod.hasModifierMask(STATIC | ABSTRACT));
	}

	@Test
	void hasAllModifiers() {
		// True with no-args
		assertTrue(acc_publicMethod.hasAllModifiers());

		// Method is only public
		assertFalse(acc_publicMethod.hasAllModifiers(PRIVATE, STATIC));
		assertFalse(acc_publicMethod.hasAllModifiers(PRIVATE));
		assertTrue(acc_publicMethod.hasAllModifiers(PUBLIC));
	}

	@Test
	void hasAnyModifiers() {
		// False with no-args
		assertFalse(acc_publicMethod.hasAnyModifiers());

		// Method is only public
		assertFalse(acc_publicMethod.hasAnyModifiers(STATIC));
		assertTrue(acc_publicMethod.hasAnyModifiers(STATIC, PUBLIC));
		assertTrue(acc_publicMethod.hasAnyModifiers(PUBLIC));
	}

	@Test
	void hasNoneOfMask() {
		// Method is only public
		assertFalse(acc_publicMethod.hasNoneOfMask(FINAL | PUBLIC));
		assertFalse(acc_publicMethod.hasNoneOfMask(PUBLIC));
		assertTrue(acc_publicMethod.hasNoneOfMask(FINAL));
		assertTrue(acc_publicMethod.hasNoneOfMask(STATIC | ABSTRACT | VOLATILE));
		assertTrue(acc_publicMethod.hasNoneOfMask(STATIC));
	}

	@Test
	void hasNoneOfModifiers() {
		// True with no-args
		assertTrue(acc_publicMethod.hasNoneOfModifiers());

		// Method is only public
		assertFalse(acc_publicMethod.hasNoneOfModifiers(FINAL, PUBLIC));
		assertFalse(acc_publicMethod.hasNoneOfModifiers(PUBLIC));
		assertTrue(acc_publicMethod.hasNoneOfModifiers(FINAL));
		assertTrue(acc_publicMethod.hasNoneOfModifiers(STATIC, ABSTRACT, VOLATILE));
		assertTrue(acc_publicMethod.hasNoneOfModifiers(STATIC));
	}

	@Test
	void isField() {
		for (MethodMember method : arrayList.getMethods()) {
			assertFalse(method.isField());
		}
	}

	@Test
	void isMethod() {
		for (MethodMember method : arrayList.getMethods()) {
			assertTrue(method.isMethod());
		}
	}
}