package software.coley.recaf.info.member;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.ClassWithInner;
import software.coley.recaf.test.dummy.VariedModifierFields;

import java.io.IOException;
import java.util.List;

import static java.lang.reflect.Modifier.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FieldMember}
 */
class FieldMemberTest {
	static JvmClassInfo accessibleFields;
	static FieldMember acc_CONSTANT_FIELD;
	static FieldMember acc_privateFinalField;
	static FieldMember acc_protectedField;
	static FieldMember acc_publicField;
	static FieldMember acc_packageField;
	static List<FieldMember> acc_fields;
	static JvmClassInfo variousFields;
	static FieldMember var_staticField;
	static FieldMember var_volatileField;
	static FieldMember var_transientField;
	static JvmClassInfo classWithInner$Inner;
	@Nullable
	static FieldMember inner_outerRefField;


	@BeforeAll
	static void setup() throws IOException {
		accessibleFields = TestClassUtils.fromRuntimeClass(AccessibleFields.class);
		acc_CONSTANT_FIELD = accessibleFields.getDeclaredField("CONSTANT_FIELD", "I");
		acc_privateFinalField = accessibleFields.getDeclaredField("privateFinalField", "I");
		acc_protectedField = accessibleFields.getDeclaredField("protectedField", "I");
		acc_publicField = accessibleFields.getDeclaredField("publicField", "I");
		acc_packageField = accessibleFields.getDeclaredField("packageField", "I");
		acc_fields = List.of(acc_CONSTANT_FIELD, acc_privateFinalField, acc_protectedField, acc_publicField, acc_packageField);

		variousFields = TestClassUtils.fromRuntimeClass(VariedModifierFields.class);
		var_staticField = variousFields.getDeclaredField("staticField", "I");
		var_volatileField = variousFields.getDeclaredField("volatileField", "I");
		var_transientField = variousFields.getDeclaredField("transientField", "I");

		classWithInner$Inner = TestClassUtils.fromRuntimeClass(ClassWithInner.TheInner.class);
		if (!classWithInner$Inner.getFields().isEmpty()) {
			inner_outerRefField = classWithInner$Inner.getFields().get(0);
		}
	}

	@Test
	void getDeclaringClass() {
		// When a class-info is created all of its fields should be made aware of the declaring class
		for (FieldMember field : acc_fields) {
			assertEquals(accessibleFields, field.getDeclaringClass(), "Field not linked with declaring class");
		}
	}

	@Test
	void isDeclarationAware() {
		// When a class-info is created all of its fields should be made aware of the declaring class
		for (FieldMember field : acc_fields) {
			assertTrue(field.isDeclarationAware(), "Field not linked with declaring class");
		}
	}

	@Test
	void hasPublicModifier() {
		assertTrue(acc_CONSTANT_FIELD.hasPublicModifier());
		assertFalse(acc_privateFinalField.hasPublicModifier());
		assertFalse(acc_protectedField.hasPublicModifier());
		assertTrue(acc_publicField.hasPublicModifier());
		assertFalse(acc_packageField.hasPublicModifier());
	}

	@Test
	void hasProtectedModifier() {
		assertFalse(acc_CONSTANT_FIELD.hasProtectedModifier());
		assertFalse(acc_privateFinalField.hasProtectedModifier());
		assertTrue(acc_protectedField.hasProtectedModifier());
		assertFalse(acc_publicField.hasProtectedModifier());
		assertFalse(acc_packageField.hasProtectedModifier());
	}

	@Test
	void hasPrivateModifier() {
		assertFalse(acc_CONSTANT_FIELD.hasPrivateModifier());
		assertTrue(acc_privateFinalField.hasPrivateModifier());
		assertFalse(acc_protectedField.hasPrivateModifier());
		assertFalse(acc_publicField.hasPrivateModifier());
		assertFalse(acc_packageField.hasPrivateModifier());
	}

	@Test
	void hasPackagePrivateModifier() {
		assertFalse(acc_CONSTANT_FIELD.hasPackagePrivateModifier());
		assertFalse(acc_privateFinalField.hasPackagePrivateModifier());
		assertFalse(acc_protectedField.hasPackagePrivateModifier());
		assertFalse(acc_publicField.hasPackagePrivateModifier());
		assertTrue(acc_packageField.hasPackagePrivateModifier());
	}

	@Test
	void hasStaticModifier() {
		assertTrue(acc_CONSTANT_FIELD.hasStaticModifier());
		assertFalse(acc_privateFinalField.hasStaticModifier());
		assertFalse(acc_protectedField.hasStaticModifier());
		assertFalse(acc_publicField.hasStaticModifier());
		assertFalse(acc_packageField.hasStaticModifier());
	}

	@Test
	void hasFinalModifier() {
		assertTrue(acc_CONSTANT_FIELD.hasFinalModifier());
		assertTrue(acc_privateFinalField.hasFinalModifier());
		assertTrue(acc_protectedField.hasFinalModifier());
		assertTrue(acc_publicField.hasFinalModifier());
		assertTrue(acc_packageField.hasFinalModifier());
	}

	@Test
	void hasSynchronizedModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasSynchronizedModifier());
		}
	}

	@Test
	void hasVolatileModifier() {
		assertFalse(var_staticField.hasVolatileModifier());
		assertTrue(var_volatileField.hasVolatileModifier());
		assertFalse(var_transientField.hasVolatileModifier());
	}

	@Test
	void hasTransientModifier() {
		assertFalse(var_staticField.hasTransientModifier());
		assertFalse(var_volatileField.hasTransientModifier());
		assertTrue(var_transientField.hasTransientModifier());
	}

	@Test
	void hasNativeModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasNativeModifier());
		}
	}

	@Test
	void hasEnumModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasEnumModifier());
		}
	}

	@Test
	void hasAnnotationModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasAnnotationModifier());
		}
	}

	@Test
	void hasInterfaceModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasInterfaceModifier());
		}
	}

	@Test
	void hasAbstractModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasAbstractModifier());
		}
	}

	@Test
	void hasStrictFpModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasStrictFpModifier());
		}
	}

	@Test
	void hasVarargsModifier() {
		// Not allowed on fields
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasVarargsModifier());
		}
	}

	@Test
	void hasBridgeModifier() {
		// Bridge is for methods, only similar mod is synthetic, but they are not the same.
		if (inner_outerRefField != null) {
			assertFalse(inner_outerRefField.hasBridgeModifier());
		}
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasBridgeModifier());
		}
	}

	@Test
	void hasSyntheticModifier() {
		for (FieldMember field : acc_fields) {
			assertFalse(field.hasSyntheticModifier());
		}
		if (inner_outerRefField != null) {
			assertTrue(inner_outerRefField.hasSyntheticModifier());
		}
	}

	@Test
	void isCompilerGenerated() {
		// Mirror prior test results
		for (FieldMember field : acc_fields) {
			assertFalse(field.isCompilerGenerated());
		}
		if (inner_outerRefField != null) {
			assertTrue(inner_outerRefField.isCompilerGenerated());
		}
	}

	@Test
	void hasModifierMask() {
		assertFalse(acc_publicField.hasModifierMask(PRIVATE | PROTECTED));
		assertTrue(acc_publicField.hasModifierMask(PUBLIC));
	}

	@Test
	void hasAllModifiers() {
		// True with no-args
		assertTrue(acc_publicField.hasAllModifiers());

		// Field is both public/final
		assertTrue(acc_publicField.hasAllModifiers(FINAL));
		assertTrue(acc_publicField.hasAllModifiers(FINAL, PUBLIC));
	}

	@Test
	void hasAnyModifiers() {
		// False with no-args
		assertFalse(acc_publicField.hasAnyModifiers());

		// Field is both public/final
		assertTrue(acc_publicField.hasAnyModifiers(FINAL));
		assertTrue(acc_publicField.hasAnyModifiers(FINAL, PUBLIC));
		assertTrue(acc_publicField.hasAnyModifiers(PUBLIC));
	}

	@Test
	void hasNoneOfMask() {
		// Field is only public/final
		assertFalse(acc_publicField.hasNoneOfMask(FINAL));
		assertFalse(acc_publicField.hasNoneOfMask(FINAL | PUBLIC));
		assertFalse(acc_publicField.hasNoneOfMask(PUBLIC));
		assertTrue(acc_publicField.hasNoneOfMask(STATIC | ABSTRACT | VOLATILE));
		assertTrue(acc_publicField.hasNoneOfMask(STATIC));
	}

	@Test
	void hasNoneOfModifiers() {
		// True with no-args
		assertTrue(acc_publicField.hasNoneOfModifiers());

		// Mirror results of prior test
		assertFalse(acc_publicField.hasNoneOfModifiers(FINAL));
		assertFalse(acc_publicField.hasNoneOfModifiers(FINAL, PUBLIC));
		assertFalse(acc_publicField.hasNoneOfModifiers(PUBLIC));
		assertTrue(acc_publicField.hasNoneOfModifiers(STATIC, ABSTRACT, VOLATILE));
		assertTrue(acc_publicField.hasNoneOfModifiers(STATIC));
	}

	@Test
	void getDefaultValue() {
		assertEquals(16, acc_CONSTANT_FIELD.getDefaultValue());
		assertEquals(8, acc_privateFinalField.getDefaultValue());
		assertEquals(4, acc_protectedField.getDefaultValue());
		assertEquals(2, acc_publicField.getDefaultValue());
		assertEquals(1, acc_packageField.getDefaultValue());
	}

	@Test
	void isField() {
		for (FieldMember field : acc_fields) {
			assertTrue(field.isField());
		}
	}

	@Test
	void isMethod() {
		for (FieldMember field : acc_fields) {
			assertFalse(field.isMethod());
		}
	}
}