package software.coley.recaf.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Types}
 */
@SuppressWarnings("ConstantValue")
public class TypesTest {
	@Test
	void testIsPrimitive() {
		for (Type primitive : Types.PRIMITIVES) {
			assertTrue(Types.isPrimitive(primitive), "Failed on: " + primitive);
		}
		for (String primitiveBox : Types.PRIMITIVE_BOXES) {
			assertFalse(Types.isPrimitive(primitiveBox), "Failed on: " + primitiveBox);
		}
		assertFalse(Types.isPrimitive(Types.STRING_TYPE));
		assertFalse(Types.isPrimitive((Type) null));
		assertFalse(Types.isPrimitive((String) null));
	}

	@Test
	void testIsPrimitiveBox() {
		for (Type primitive : Types.PRIMITIVES) {
			assertFalse(Types.isBoxedPrimitive(primitive.getDescriptor()), "Failed on: " + primitive);
		}
		for (String primitiveBox : Types.PRIMITIVE_BOXES) {
			assertTrue(Types.isBoxedPrimitive(primitiveBox), "Failed on: " + primitiveBox);
		}
		assertFalse(Types.isBoxedPrimitive(null));
	}

	@Test
	void testClassToPrimitive() {
		assertEquals("V", Types.classToPrimitive("void"));
		assertEquals("Z", Types.classToPrimitive("boolean"));
		assertEquals("B", Types.classToPrimitive("byte"));
		assertEquals("C", Types.classToPrimitive("char"));
		assertEquals("S", Types.classToPrimitive("short"));
		assertEquals("I", Types.classToPrimitive("int"));
		assertEquals("F", Types.classToPrimitive("float"));
		assertEquals("D", Types.classToPrimitive("double"));
		assertEquals("J", Types.classToPrimitive("long"));
		assertThrows(IllegalArgumentException.class, () -> Types.classToPrimitive("foo"));
	}

	@Test
	void testIsPrimitiveClassName() {
		assertTrue(Types.isPrimitiveClassName("void"));
		assertTrue(Types.isPrimitiveClassName("boolean"));
		assertTrue(Types.isPrimitiveClassName("byte"));
		assertTrue(Types.isPrimitiveClassName("char"));
		assertTrue(Types.isPrimitiveClassName("short"));
		assertTrue(Types.isPrimitiveClassName("int"));
		assertTrue(Types.isPrimitiveClassName("float"));
		assertTrue(Types.isPrimitiveClassName("double"));
		assertTrue(Types.isPrimitiveClassName("long"));
		//
		assertFalse(Types.isPrimitiveClassName("Z"));
		assertFalse(Types.isPrimitiveClassName("[I"));
		assertFalse(Types.isPrimitiveClassName("VOID"));
		assertFalse(Types.isPrimitiveClassName(""));
		assertFalse(Types.isPrimitiveClassName(null));
	}

	@Test
	void testIsVoid() {
		assertTrue(Types.isVoid(Type.getType("V")));
		assertFalse(Types.isVoid(Type.getType("()V")));
		assertFalse(Types.isVoid(Type.getType("[V")));
		assertFalse(Types.isVoid(null));
	}

	@Test
	void testMakeArray() {
		assertEquals(Type.getType("[I"), Types.array(Type.getType("I"), 1));
		assertEquals(Type.getType("[[I"), Types.array(Type.getType("I"), 2));
		assertEquals(Type.getType("[[[I"), Types.array(Type.getType("I"), 3));
	}

	@Test
	void testCountParameterSlots() {
		assertEquals(0, Types.countParameterSlots(Type.getMethodType("()V")));
		assertEquals(1, Types.countParameterSlots(Type.getMethodType("(I)V")));
		assertEquals(2, Types.countParameterSlots(Type.getMethodType("(II)V")));
		assertEquals(3, Types.countParameterSlots(Type.getMethodType("(III)V")));
		assertEquals(4, Types.countParameterSlots(Type.getMethodType("(JII)V")));
		assertEquals(4, Types.countParameterSlots(Type.getMethodType("(IJI)V")));
		assertEquals(6, Types.countParameterSlots(Type.getMethodType("(JJJ)V")));
		assertEquals(3, Types.countParameterSlots(Type.getMethodType("([I[[J[[I)V")));
		assertEquals(1, Types.countParameterSlots(Type.getMethodType("(Ljava/lang/String;)V")));
	}

	@Test
	void testIsValidDesc() {
		assertTrue(Types.isValidDesc("([I[[J[[I)V"), "method desc");
		assertTrue(Types.isValidDesc("[I"), "array desc");
		assertTrue(Types.isValidDesc("Ljava/lang/String;"), "object desc");
		//
		assertTrue(Types.isValidDesc("LLLLj/av/a/la/ng/S/t/ri/n/g;"), "ugly but valid");
		assertTrue(Types.isValidDesc("L\0;"), "null-terminator is valid");
		assertTrue(Types.isValidDesc("L\n;"), "newline is valid");
		assertTrue(Types.isValidDesc("L;"), "empty is valid");
		//
		assertFalse(Types.isValidDesc(null), "null is invalid");
		assertFalse(Types.isValidDesc(""), "empty string is invalid");
		assertFalse(Types.isValidDesc("["), "array without type is invalid");
		assertFalse(Types.isValidDesc("[P"), "array without valid type is invalid");
		assertFalse(Types.isValidDesc("java/lang/String"), "internal name is invalid");
		assertFalse(Types.isValidDesc("(L;;)V"), "double ;; is unresolvable in method desc args");
	}

	@Test
	void testIsValidSignature() {
		assertTrue(Types.isValidMethodSignature("()V"));
		assertTrue(Types.isValidFieldSignature("Ljava/lang/Supplier;"));
		assertTrue(Types.isValidClassSignature("Ljava/lang/Supplier;"));
		assertTrue(Types.isValidClassSignature("Ljava/lang/Supplier<Ljava/lang/String;>;"));
		assertTrue(Types.isValidClassSignature("Ljava/util/List<[Ljava/util/List<Ljava/lang/String;>;>;"));
		assertTrue(Types.isValidFieldSignature("Ljava/util/List<[Ljava/util/List<Ljava/lang/String;>;>;"));
		//
		assertFalse(Types.isValidClassSignature(""), "Empty signature must be invalid");
		assertFalse(Types.isValidFieldSignature(""), "Empty signature must be invalid");
		assertFalse(Types.isValidMethodSignature(""), "Empty signature must be invalid");
		assertFalse(Types.isValidFieldSignature("V"), "Fields cannot be 'void'");
		assertFalse(Types.isValidClassSignature("Ljava/lang/Supplier<I>;"), "Primitive int cannot be used as type argument");
	}

	@Test
	void testIsWide() {
		assertTrue(Types.isWide(Type.getType("D")));
		assertTrue(Types.isWide(Type.getType("J")));
		//
		assertFalse(Types.isWide(Type.getType("V")));
		assertFalse(Types.isWide(Type.getType("Z")));
		assertFalse(Types.isWide(Type.getType("B")));
		assertFalse(Types.isWide(Type.getType("C")));
		assertFalse(Types.isWide(Type.getType("S")));
		assertFalse(Types.isWide(Type.getType("I")));
		assertFalse(Types.isWide(Type.getType("F")));
		assertFalse(Types.isWide(Type.getType("[D")));
		assertFalse(Types.isWide(Type.getType("[J")));
		assertFalse(Types.isWide(Type.getType("Ljava/lang/String;")));
	}

	@Test
	void testFromVarOpcode() {
		int[] ints = {Opcodes.IINC, Opcodes.ILOAD, Opcodes.ISTORE};
		int[] floats = {Opcodes.FLOAD, Opcodes.FSTORE};
		int[] doubles = {Opcodes.DLOAD, Opcodes.DSTORE};
		int[] longs = {Opcodes.LLOAD, Opcodes.LSTORE};
		int[] objects = {Opcodes.ALOAD, Opcodes.ASTORE};
		for (int v : ints) assertSame(Type.INT_TYPE, Types.fromVarOpcode(v));
		for (int v : floats) assertSame(Type.FLOAT_TYPE, Types.fromVarOpcode(v));
		for (int v : doubles) assertSame(Type.DOUBLE_TYPE, Types.fromVarOpcode(v));
		for (int v : longs) assertSame(Type.LONG_TYPE, Types.fromVarOpcode(v));
		for (int v : objects) assertSame(Types.OBJECT_TYPE, Types.fromVarOpcode(v));
		assertNull(Types.fromVarOpcode(-1));
	}

	@Test
	void testFromArrayOpcode() {
		int[] ints = {Opcodes.ARRAYLENGTH, Opcodes.BALOAD, Opcodes.CALOAD, Opcodes.SALOAD, Opcodes.IALOAD,
				Opcodes.BASTORE, Opcodes.CASTORE, Opcodes.SASTORE, Opcodes.IASTORE};
		int[] floats = {Opcodes.FALOAD, Opcodes.FASTORE};
		int[] doubles = {Opcodes.DALOAD, Opcodes.DASTORE};
		int[] longs = {Opcodes.LALOAD, Opcodes.LASTORE};
		int[] objects = {Opcodes.AALOAD, Opcodes.AASTORE};
		for (int v : ints) assertSame(Type.INT_TYPE, Types.fromArrayOpcode(v));
		for (int v : floats) assertSame(Type.FLOAT_TYPE, Types.fromArrayOpcode(v));
		for (int v : doubles) assertSame(Type.DOUBLE_TYPE, Types.fromArrayOpcode(v));
		for (int v : longs) assertSame(Type.LONG_TYPE, Types.fromArrayOpcode(v));
		for (int v : objects) assertSame(Types.OBJECT_TYPE, Types.fromArrayOpcode(v));
		assertNull(Types.fromArrayOpcode(-1));
	}

	@Test
	void testGetNormalizedSort() {
		assertEquals(Type.METHOD, Types.getNormalizedSort(Type.METHOD));
		assertEquals(Type.OBJECT, Types.getNormalizedSort(Type.OBJECT));
		assertEquals(Type.OBJECT, Types.getNormalizedSort(Type.ARRAY));
		assertEquals(Type.DOUBLE, Types.getNormalizedSort(Type.DOUBLE));
		assertEquals(Type.LONG, Types.getNormalizedSort(Type.LONG));
		assertEquals(Type.FLOAT, Types.getNormalizedSort(Type.FLOAT));
		assertEquals(Type.INT, Types.getNormalizedSort(Type.INT));
		assertEquals(Type.INT, Types.getNormalizedSort(Type.SHORT));
		assertEquals(Type.INT, Types.getNormalizedSort(Type.BYTE));
		assertEquals(Type.INT, Types.getNormalizedSort(Type.CHAR));
		assertEquals(Type.INT, Types.getNormalizedSort(Type.BOOLEAN));
		assertEquals(Type.VOID, Types.getNormalizedSort(Type.VOID));
	}

	@Test
	void testGetSortName() {
		assertEquals("void", Types.getSortName(Type.VOID));
		assertEquals("boolean", Types.getSortName(Type.BOOLEAN));
		assertEquals("char", Types.getSortName(Type.CHAR));
		assertEquals("byte", Types.getSortName(Type.BYTE));
		assertEquals("short", Types.getSortName(Type.SHORT));
		assertEquals("int", Types.getSortName(Type.INT));
		assertEquals("float", Types.getSortName(Type.FLOAT));
		assertEquals("long", Types.getSortName(Type.LONG));
		assertEquals("double", Types.getSortName(Type.DOUBLE));
		assertEquals("array", Types.getSortName(Type.ARRAY));
		assertEquals("object", Types.getSortName(Type.OBJECT));
		assertEquals("method", Types.getSortName(Type.METHOD));
	}

	@Test
	void testPrettify() {
		assertEquals("int", Types.pretty(Type.getType("I")));
		assertEquals("int[]", Types.pretty(Type.getType("[I")));
		assertEquals("(int, float) void", Types.pretty(Type.getMethodType("(IF)V")));
	}
}
