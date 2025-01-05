package software.coley.recaf.util;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NumberUtil}
 */
class NumberUtilTest {
	@Test
	void testToString() {
		assertEquals("0", NumberUtil.toString(0));
		assertEquals("0.0", NumberUtil.toString(0.0));
		assertEquals("0.0", NumberUtil.toString(0D));
		assertEquals("0.0F", NumberUtil.toString(0F));
		assertEquals("1.0F", NumberUtil.toString(1F));
		assertEquals("0L", NumberUtil.toString(0L));
	}

	@Test
	void testParse() {
		assertEquals(0, NumberUtil.parse("0"));
		assertEquals(0L, NumberUtil.parse("0L"));
		assertEquals(0D, NumberUtil.parse("0.0"));
		assertEquals(0D, NumberUtil.parse("0.0d"));
		assertEquals(0D, NumberUtil.parse("0.0D"));
		assertEquals(0F, NumberUtil.parse("0.0f"));
		assertEquals(0F, NumberUtil.parse("0.0F"));
		assertEquals(0D, NumberUtil.parse("0."));
		assertEquals(0F, NumberUtil.parse("0.F"));
		assertEquals(0, NumberUtil.parse("0x"));
		assertEquals(0L, NumberUtil.parse("0xL"));
		assertEquals(0, NumberUtil.parse("0x0"));
		assertEquals(0L, NumberUtil.parse("0x0L"));
	}

	@Test
	void testGetWidestType() {
		assertEquals(Type.DOUBLE_TYPE, NumberUtil.getWidestType(Type.DOUBLE_TYPE, Type.FLOAT_TYPE));
		assertEquals(Type.DOUBLE_TYPE, NumberUtil.getWidestType(Type.DOUBLE_TYPE, Type.INT_TYPE));
		assertEquals(Type.DOUBLE_TYPE, NumberUtil.getWidestType(Type.DOUBLE_TYPE, Type.SHORT_TYPE));
		assertEquals(Type.DOUBLE_TYPE, NumberUtil.getWidestType(Type.DOUBLE_TYPE, Type.BYTE_TYPE));
		assertEquals(Type.DOUBLE_TYPE, NumberUtil.getWidestType(Type.DOUBLE_TYPE, Type.LONG_TYPE));
		assertEquals(Type.LONG_TYPE, NumberUtil.getWidestType(Type.LONG_TYPE, Type.INT_TYPE));
		assertEquals(Type.LONG_TYPE, NumberUtil.getWidestType(Type.LONG_TYPE, Type.FLOAT_TYPE));
		assertEquals(Type.LONG_TYPE, NumberUtil.getWidestType(Type.LONG_TYPE, Type.SHORT_TYPE));
		assertEquals(Type.INT_TYPE, NumberUtil.getWidestType(Type.INT_TYPE, Type.SHORT_TYPE));
		assertEquals(Type.SHORT_TYPE, NumberUtil.getWidestType(Type.SHORT_TYPE, Type.BYTE_TYPE));
	}

	@Test
	void testCmp() {
		// Int input
		assertEquals(0, NumberUtil.cmp(0, 0));
		assertEquals(1, NumberUtil.cmp(1, 0));
		assertEquals(-1, NumberUtil.cmp(0, 1));

		// Long input
		assertEquals(0, NumberUtil.cmp(0L, 0L));
		assertEquals(1, NumberUtil.cmp(1L, 0L));
		assertEquals(-1, NumberUtil.cmp(0L, 1L));

		// Double input
		assertEquals(0, NumberUtil.cmp(0.0, 0.0));
		assertEquals(1, NumberUtil.cmp(1.0, 0.0));
		assertEquals(-1, NumberUtil.cmp(0.0, 1.0));

		// Float input
		assertEquals(0, NumberUtil.cmp(0.0F, 0.0F));
		assertEquals(1, NumberUtil.cmp(1.0F, 0.0F));
		assertEquals(-1, NumberUtil.cmp(0.0F, 1.0F));

		// Mixed input
		assertEquals(0, NumberUtil.cmp(0, 0.0F));
		assertEquals(1, NumberUtil.cmp(1.0F, 0L));
		assertEquals(-1, NumberUtil.cmp(0x0L, 1.0));
	}

	@Test
	void testSub() {
		assertEquals(3, NumberUtil.sub(10, 7));
		assertEquals(3D, NumberUtil.sub(10, 7.0D));
		assertEquals(3F, NumberUtil.sub(10, 7.0F));
		assertEquals(3L, NumberUtil.sub(10, 7L));
	}

	@Test
	void testAdd() {
		assertEquals(10, NumberUtil.add(3, 7));
		assertEquals(10D, NumberUtil.add(3, 7.0D));
		assertEquals(10F, NumberUtil.add(3, 7.0F));
		assertEquals(10L, NumberUtil.add(3, 7L));
	}

	@Test
	void testMul() {
		assertEquals(21, NumberUtil.mul(3, 7));
		assertEquals(21D, NumberUtil.mul(3, 7.0D));
		assertEquals(21F, NumberUtil.mul(3, 7.0F));
		assertEquals(21L, NumberUtil.mul(3, 7L));
	}

	@Test
	void testDiv() {
		assertEquals(3, NumberUtil.div(21, 7));
		assertEquals(3D, NumberUtil.div(21, 7.0D));
		assertEquals(3F, NumberUtil.div(21, 7.0F));
		assertEquals(3L, NumberUtil.div(21, 7L));
	}

	@Test
	void testRem() {
		assertEquals(4, NumberUtil.rem(13, 9));
		assertEquals(4D, NumberUtil.rem(13, 9.0D));
		assertEquals(4F, NumberUtil.rem(13, 9.0F));
		assertEquals(4L, NumberUtil.rem(13, 9L));
	}

	@Test
	void testAnd() {
		assertEquals(0b00100, NumberUtil.and(0b11100, 0b00111));
		assertEquals(0b00100L, NumberUtil.and(0b11100, 0b00111L));
	}

	@Test
	void testOr() {
		assertEquals(0b111111, NumberUtil.or(0b101010, 0b010101));
		assertEquals(0b111111L, NumberUtil.or(0b101010, 0b010101L));
	}

	@Test
	void testXor() {
		assertEquals(0b11011, NumberUtil.xor(0b11100, 0b00111));
		assertEquals(0b11011L, NumberUtil.xor(0b11100, 0b00111L));
	}

	@Test
	void testNeg() {
		assertEquals(-1, NumberUtil.neg(1));
		assertEquals(-1D, NumberUtil.neg(1.0));
		assertEquals(-1F, NumberUtil.neg(1.0F));
		assertEquals(-1L, NumberUtil.neg(1L));
	}

	@Test
	void testShiftLeft() {
		assertEquals(2 << 1, NumberUtil.shiftLeft(2, 1));
		assertEquals(2L << 1, NumberUtil.shiftLeft(2L, 1));
	}

	@Test
	void testShiftRight() {
		assertEquals(16 >> 1, NumberUtil.shiftRight(16, 1));
		assertEquals(16L >> 1, NumberUtil.shiftRight(16L, 1));
	}

	@Test
	void testShiftRightU() {
		assertEquals(16 >> 1, NumberUtil.shiftRightU(16, 1));
		assertEquals(16L >> 1, NumberUtil.shiftRightU(16L, 1));
	}

	@Test
	void testIntPow() {
		assertEquals(1, NumberUtil.intPow(28419284, 0));
		assertEquals(100, NumberUtil.intPow(10, 2));
		assertThrows(IllegalArgumentException.class, () -> NumberUtil.intPow(10, -1));
	}

	@Test
	void testIntClamp() {
		assertEquals(5, NumberUtil.intClamp(100, 0, 5));
		assertEquals(5, NumberUtil.intClamp(-25, 5, 10));
		assertEquals(5, NumberUtil.intClamp(5, 0, 10));
	}

	@Test
	void testDoubleClamp() {
		assertEquals(5D, NumberUtil.doubleClamp(100.0, 0, 5));
		assertEquals(5D, NumberUtil.doubleClamp(-25.0, 5, 10));
		assertEquals(5D, NumberUtil.doubleClamp(5.0, 0, 10));
	}

	@Test
	void testIsNonZero() {
		assertTrue(NumberUtil.isNonZero(1));
		assertTrue(NumberUtil.isNonZero(-1));
		assertFalse(NumberUtil.isNonZero(0));
	}

	@Test
	void testIsZero() {
		assertFalse(NumberUtil.isZero(1));
		assertFalse(NumberUtil.isZero(-1));
		assertTrue(NumberUtil.isZero(0));
	}

	@Test
	void testHaveSameSign() {
		assertTrue(NumberUtil.haveSameSign(1, 1));
		assertTrue(NumberUtil.haveSameSign(-1, -1));
		assertFalse(NumberUtil.haveSameSign(1, -1));
		assertFalse(NumberUtil.haveSameSign(-1, 1));
	}
}