package software.coley.recaf.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AccessFlag}
 */
class AccessFlagTest {
	@Test
	void testGetFlag() {
		assertEquals(AccessFlag.ACC_STATIC, AccessFlag.getFlag("static"));
	}

	@Test
	void testGetFlags() {
		assertEquals(Set.of(), AccessFlag.getFlags(0));
		assertEquals(Set.of(AccessFlag.ACC_PUBLIC), AccessFlag.getFlags(Opcodes.ACC_PUBLIC));
		assertEquals(Set.of(AccessFlag.ACC_STATIC), AccessFlag.getFlags(Opcodes.ACC_STATIC));
		assertEquals(List.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC), AccessFlag.getFlags("public static"));
		assertEquals(List.of(AccessFlag.ACC_STATIC, AccessFlag.ACC_STATIC), AccessFlag.getFlags("static static"));
	}

	@Test
	void testToString() {
		assertEquals("public static final", AccessFlag.toString(List.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL)));

		// Super flag ignored
		assertEquals("", AccessFlag.toString(List.of(AccessFlag.ACC_SUPER)));
	}

	@Test
	void testSortAndToString() {
		assertEquals("private static final", AccessFlag.sortAndToString(AccessFlag.Type.FIELD,
				List.of(AccessFlag.ACC_FINAL, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_STATIC)));
		assertEquals("private static final", AccessFlag.sortAndToString(AccessFlag.Type.FIELD,
				Opcodes.ACC_FINAL | Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC));
	}

	@Test
	void testGetCodeFriendlyName() {
		assertEquals("public", AccessFlag.ACC_PUBLIC.getCodeFriendlyName());
		assertEquals("/* synthetic */", AccessFlag.ACC_SYNTHETIC.getCodeFriendlyName());
		assertEquals("/* bridge */", AccessFlag.ACC_BRIDGE.getCodeFriendlyName());
	}

	@Test
	void testGetApplicableFlags() {
		// All applicable flags
		assertEquals(Set.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PRIVATE, AccessFlag.ACC_PROTECTED,
						AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL, AccessFlag.ACC_VOLATILE,
						AccessFlag.ACC_TRANSIENT, AccessFlag.ACC_SYNTHETIC, AccessFlag.ACC_ENUM),
				AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD));

		// Filter with mask
		assertEquals(Set.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC),
				AccessFlag.getApplicableFlags(AccessFlag.Type.FIELD, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));
	}

	@Test
	void testSort() {
		List<AccessFlag> correctOrder = List.of(AccessFlag.ACC_PRIVATE, AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL);
		assertEquals(correctOrder, AccessFlag.sort(AccessFlag.Type.FIELD, correctOrder));
		assertEquals(correctOrder, AccessFlag.sort(AccessFlag.Type.FIELD, List.of(AccessFlag.ACC_STATIC, AccessFlag.ACC_FINAL, AccessFlag.ACC_PRIVATE)));
	}

	@Test
	void testCreateAccess() {
		assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, AccessFlag.createAccess(AccessFlag.ACC_STATIC, AccessFlag.ACC_PUBLIC));
	}

	@Test
	void testHasAll() {
		assertTrue(AccessFlag.hasAll(0, Collections.emptyList()));
		assertTrue(AccessFlag.hasAll(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_PUBLIC)));
		assertFalse(AccessFlag.hasAll(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC)));

		// var-args form
		assertTrue(AccessFlag.hasAll(0));
		assertTrue(AccessFlag.hasAll(Opcodes.ACC_PUBLIC, AccessFlag.ACC_PUBLIC));
		assertFalse(AccessFlag.hasAll(Opcodes.ACC_PUBLIC, AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC));
	}

	@Test
	void testHasNone() {
		assertTrue(AccessFlag.hasNone(0, Collections.emptyList()));
		assertTrue(AccessFlag.hasNone(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_STATIC)));
		assertFalse(AccessFlag.hasNone(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC)));

		// var-args form
		assertTrue(AccessFlag.hasNone(0));
		assertTrue(AccessFlag.hasNone(Opcodes.ACC_PUBLIC, AccessFlag.ACC_STATIC));
		assertFalse(AccessFlag.hasNone(Opcodes.ACC_PUBLIC, AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC));
	}

	@Test
	void testHasAny() {
		assertFalse(AccessFlag.hasAny(0, Collections.emptyList()));
		assertTrue(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_PUBLIC)));
		assertFalse(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_STATIC)));
		assertTrue(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, List.of(AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC)));

		// var-args form
		assertFalse(AccessFlag.hasAny(0));
		assertTrue(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, AccessFlag.ACC_PUBLIC));
		assertFalse(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, AccessFlag.ACC_STATIC));
		assertTrue(AccessFlag.hasAny(Opcodes.ACC_PUBLIC, AccessFlag.ACC_PUBLIC, AccessFlag.ACC_STATIC));
	}

	@Test
	void testIsX() {
		assertTrue(AccessFlag.isPublic(Opcodes.ACC_PUBLIC));
		assertFalse(AccessFlag.isPublic(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isPrivate(Opcodes.ACC_PRIVATE));
		assertFalse(AccessFlag.isPrivate(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isProtected(Opcodes.ACC_PROTECTED));
		assertFalse(AccessFlag.isProtected(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isPackage(0));
		assertFalse(AccessFlag.isPackage(Opcodes.ACC_PRIVATE));
		assertFalse(AccessFlag.isPackage(Opcodes.ACC_PROTECTED));
		assertFalse(AccessFlag.isPackage(Opcodes.ACC_PUBLIC));

		assertTrue(AccessFlag.isStatic(Opcodes.ACC_STATIC));
		assertFalse(AccessFlag.isStatic(Opcodes.ACC_FINAL));

		assertTrue(AccessFlag.isFinal(Opcodes.ACC_FINAL));
		assertFalse(AccessFlag.isFinal(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isSynchronized(Opcodes.ACC_SYNCHRONIZED));
		assertFalse(AccessFlag.isSynchronized(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isSuper(Opcodes.ACC_SUPER));
		assertFalse(AccessFlag.isSuper(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isBridge(Opcodes.ACC_BRIDGE));
		assertFalse(AccessFlag.isBridge(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isVolatile(Opcodes.ACC_VOLATILE));
		assertFalse(AccessFlag.isVolatile(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isVarargs(Opcodes.ACC_VARARGS));
		assertFalse(AccessFlag.isVarargs(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isTransient(Opcodes.ACC_TRANSIENT));
		assertFalse(AccessFlag.isTransient(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isNative(Opcodes.ACC_NATIVE));
		assertFalse(AccessFlag.isNative(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isInterface(Opcodes.ACC_INTERFACE));
		assertFalse(AccessFlag.isInterface(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isAbstract(Opcodes.ACC_ABSTRACT));
		assertFalse(AccessFlag.isAbstract(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isStrict(Opcodes.ACC_STRICT));
		assertFalse(AccessFlag.isStrict(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isSynthetic(Opcodes.ACC_SYNTHETIC));
		assertFalse(AccessFlag.isSynthetic(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isAnnotation(Opcodes.ACC_ANNOTATION));
		assertFalse(AccessFlag.isAnnotation(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isEnum(Opcodes.ACC_ENUM));
		assertFalse(AccessFlag.isEnum(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isModule(Opcodes.ACC_MODULE));
		assertFalse(AccessFlag.isModule(Opcodes.ACC_STATIC));

		assertTrue(AccessFlag.isMandated(Opcodes.ACC_MANDATED));
		assertFalse(AccessFlag.isMandated(Opcodes.ACC_STATIC));
	}
}