package me.coley.recaf.ssvm.value;

import org.objectweb.asm.Opcodes;

/**
 * Basic primitive operations.
 *
 * @author xDark
 */
public class ValueOperations implements Opcodes {
	private final static float F_NaN = Float.intBitsToFloat(0x7fc00000);
	private final static double D_NaN = Double.longBitsToDouble(0x7ff8000000000000L);

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return Evaluated value.
	 */
	public static long evaluate(int opcode, long v1, long v2) {
		switch (opcode) {
			case LADD:
				return v1 + v2;
			case LSUB:
				return v1 - v2;
			case LMUL:
				return v1 * v2;
			case LDIV:
				return v1 / v2;
			case LREM:
				return v1 % v2;
			case LAND:
				return v1 & v2;
			case LOR:
				return v1 | v2;
			case LXOR:
				return v1 ^ v2;
			case LCMP:
				if (v1 > v2) return 1;
				else if (v1 < v2) return -1;
				return 0;
			case LUSHR:
			case LSHL:
			case LSHR:
				return evaluate(opcode, v1, (int) v2);
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return Evaluated value.
	 */
	public static long evaluate(int opcode, long v1, int v2) {
		switch (opcode) {
			case LSHL:
				return v1 << v2;
			case LSHR:
				return v1 >> v2;
			case LUSHR:
				return v1 >>> v2;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return Evaluated value.
	 */
	public static long evaluate(int opcode, long v) {
		switch (opcode) {
			case LNEG:
				return -v;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return Evaluated value.
	 */
	public static double evaluate(int opcode, double v1, double v2) {
		switch (opcode) {
			case DADD:
				return v1 + v2;
			case DSUB:
				return v1 - v2;
			case DMUL:
				return v1 * v2;
			case DDIV:
				return v1 / v2;
			case DREM:
				return v1 % v2;
			case DCMPG:
				if (v1 == D_NaN || v2 == D_NaN) return 1;
				else if (v1 > v2) return 1;
				else if (v1 < v2) return -1;
				return 0;
			case DCMPL:
				if (v1 == D_NaN || v2 == D_NaN) return -1;
				else if (v1 > v2) return 1;
				else if (v1 < v2) return -1;
				return 0;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return Evaluated value.
	 */
	public static double evaluate(int opcode, double v) {
		switch (opcode) {
			case DNEG:
				return -v;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return Evaluated value.
	 */
	public static int evaluate(int opcode, int v1, int v2) {
		switch (opcode) {
			case IADD:
				return v1 + v2;
			case ISUB:
				return v1 - v2;
			case IMUL:
				return v1 * v2;
			case IDIV:
				return v1 / v2;
			case IREM:
				return v1 % v2;
			case ISHL:
				return v1 << v2;
			case ISHR:
				return v1 >> v2;
			case IUSHR:
				return v1 >>> v2;
			case IAND:
				return v1 & v2;
			case IOR:
				return v1 | v2;
			case IXOR:
				return v1 ^ v2;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return Evaluated value.
	 */
	public static int evaluate(int opcode, int v) {
		switch (opcode) {
			case INEG:
				return -v;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return Evaluated value.
	 */
	public static float evaluate(int opcode, float v1, float v2) {
		switch (opcode) {
			case FADD:
				return v1 + v2;
			case FSUB:
				return v1 - v2;
			case FMUL:
				return v1 * v2;
			case FDIV:
				return v1 / v2;
			case FREM:
				return v1 % v2;
			case FCMPG:
				if (v1 == F_NaN || v2 == F_NaN) return 1;
				else if (v1 > v2) return 1;
				else if (v1 < v2) return -1;
				return 0;
			case FCMPL:
				if (v1 == F_NaN || v2 == F_NaN) return -1;
				else if (v1 > v2) return 1;
				else if (v1 < v2) return -1;
				return 0;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Operation instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return Evaluated value.
	 */
	public static float evaluate(int opcode, float v) {
		switch (opcode) {
			case FNEG:
				return -v;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Flow instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return {@code true} when branch is taken.
	 */
	public static boolean flow(int opcode, int v) {
		switch (opcode) {
			case IFEQ:
				return v == 0;
			case IFNE:
				return v != 0;
			case IFLT:
				return v < 0;
			case IFGE:
				return v >= 0;
			case IFGT:
				return v > 0;
			case IFLE:
				return v <= 0;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Flow instruction.
	 * @param v
	 * 		Stack value <i>(top)</i>
	 *
	 * @return {@code true} when branch is taken.
	 */
	public static boolean flow(int opcode, Object v) {
		switch (opcode) {
			case IFNONNULL:
				return v != null && v != NULL;
			case IFNULL:
				return v == null || v == NULL;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Flow instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return {@code true} when branch is taken.
	 */
	public static boolean flow(int opcode, int v1, int v2) {
		switch (opcode) {
			case IF_ICMPEQ:
				return v1 == v2;
			case IF_ICMPNE:
				return v1 != v2;
			case IF_ICMPLT:
				return v1 < v2;
			case IF_ICMPGE:
				return v1 >= v2;
			case IF_ICMPGT:
				return v1 > v2;
			case IF_ICMPLE:
				return v1 <= v2;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}

	/**
	 * @param opcode
	 * 		Flow instruction.
	 * @param v1
	 * 		First stack value <i>(top)</i>
	 * @param v2
	 * 		Second stack value.
	 *
	 * @return {@code true} when branch is taken.
	 */
	public static boolean flow(int opcode, Object v1, Object v2) {
		switch (opcode) {
			case IF_ACMPEQ:
				return v1 == v2;
			case IF_ACMPNE:
				return v1 != v2;
			default:
				throw new IllegalStateException(Integer.toString(opcode));
		}
	}
}
