package me.coley.recaf.util;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.tree.AbstractInsnNode.*;

/**
 * ASM utilities around {@link AbstractInsnNode}.
 *
 * @author Matt Coley
 */
public class InstructionUtil {
	/**
	 * @param op
	 * 		Instruction opcode. Should be of type
	 *        {@link org.objectweb.asm.tree.AbstractInsnNode#INSN}.
	 *
	 * @return value represented by the instruction.
	 *
	 * @throws IllegalArgumentException
	 * 		Thrown if the opcode does not have a known value.
	 */
	public static int getValue(int op) {
		switch (op) {
			case ICONST_M1:
				return -1;
			case FCONST_0:
			case LCONST_0:
			case DCONST_0:
			case ICONST_0:
				return 0;
			case FCONST_1:
			case LCONST_1:
			case DCONST_1:
			case ICONST_1:
				return 1;
			case FCONST_2:
			case ICONST_2:
				return 2;
			case ICONST_3:
				return 3;
			case ICONST_4:
				return 4;
			case ICONST_5:
				return 5;
			default:
				throw new IllegalArgumentException("Invalid opcode, does not have a known value: " + op);
		}
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} when the instruction pushes a constant {@code int} to the stack.
	 */
	public static boolean isConstInt(AbstractInsnNode insn) {
		if (insn == null) return false;
		if (insn instanceof LdcInsnNode) {
			Object cst = ((LdcInsnNode) insn).cst;
			return cst instanceof Integer || cst instanceof Character || cst instanceof Short || cst instanceof Byte;
		}
		int op = insn.getOpcode();
		if (op == Opcodes.BIPUSH || op == Opcodes.SIPUSH) return true;
		return op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5;
	}

	/**
	 * @param insn
	 * 		Instruction to check.
	 *
	 * @return {@code true} if the instruction pushes a constant value onto the stack.
	 */
	public static boolean isConst(AbstractInsnNode insn) {
		int op = insn.getOpcode();
		return op >= ACONST_NULL && op <= LDC;
	}

	/**
	 * @param op
	 * 		Instruction opcode.
	 *
	 * @return {@code true} when it is a return operation.
	 */
	public static boolean isReturn(int op) {
		switch (op) {
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
				return true;
			default:
				return false;
		}
	}

	/**
	 * @param type
	 * 		Some type.
	 *
	 * @return Method return instruction opcode for the given type.
	 */
	public static int getReturnOpcode(Type type) {
		switch (type.getSort()) {
			case Type.VOID:
				return RETURN;
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
				return IRETURN;
			case Type.FLOAT:
				return FRETURN;
			case Type.LONG:
				return LRETURN;
			case Type.DOUBLE:
				return DRETURN;
			default:
				return ARETURN;
		}
	}

	/**
	 * @param type
	 * 		Type to push.
	 *
	 * @return Instruction to push a default value of the given type onto the stack.
	 */
	public static AbstractInsnNode createDefaultPush(Type type) {
		switch (type.getSort()) {
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
				return new InsnNode(ICONST_0);
			case Type.LONG:
				return new InsnNode(LCONST_0);
			case Type.FLOAT:
				return new InsnNode(FCONST_0);
			case Type.DOUBLE:
				return new InsnNode(DCONST_0);
			case Type.OBJECT:
			case Type.ARRAY:
			default:
				return new InsnNode(ACONST_NULL);
		}
	}

	/**
	 * Create an instruction to hold a given value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createPush(Object value) {
		if (value == null)
			return new InsnNode(ACONST_NULL);
		else if (value instanceof Number)
			return createNumberPush((Number) value);
		else if (value instanceof String)
			return new LdcInsnNode(value);
		throw new IllegalStateException("Unsupported value type: " + value.getClass().getName());
	}

	/**
	 * Create an instruction to hold a given numeric value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createNumberPush(Number value) {
		if (value instanceof Long)
			return createLongPush(value.longValue());
		else if (value instanceof Float)
			return createFloatPush(value.floatValue());
		else if (value instanceof Double)
			return createDoublePush(value.doubleValue());
		// int/short/etc
		return createIntPush(value.intValue());
	}

	/**
	 * Create an instruction to hold a given {@code int} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createIntPush(int value) {
		switch (value) {
			case -1:
				return new InsnNode(ICONST_M1);
			case 0:
				return new InsnNode(ICONST_0);
			case 1:
				return new InsnNode(ICONST_1);
			case 2:
				return new InsnNode(ICONST_2);
			case 3:
				return new InsnNode(ICONST_3);
			case 4:
				return new InsnNode(ICONST_4);
			case 5:
				return new InsnNode(ICONST_5);
			default:
				if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
					return new IntInsnNode(BIPUSH, value);
				} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
					return new IntInsnNode(SIPUSH, value);
				} else {
					return new LdcInsnNode(value);
				}
		}
	}

	/**
	 * Create an instruction to hold a given {@code long} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createLongPush(long value) {
		if (value == 0)
			return new InsnNode(LCONST_0);
		else if (value == 1)
			return new InsnNode(LCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code float} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createFloatPush(float value) {
		if (value == 0)
			return new InsnNode(FCONST_0);
		else if (value == 1)
			return new InsnNode(FCONST_1);
		else if (value == 2)
			return new InsnNode(FCONST_2);
		return new LdcInsnNode(value);
	}

	/**
	 * Create an instruction to hold a given {@code double} value.
	 *
	 * @param value
	 * 		Value to hold.
	 *
	 * @return Instruction with const value.
	 */
	public static AbstractInsnNode createDoublePush(double value) {
		if (value == 0)
			return new InsnNode(DCONST_0);
		else if (value == 1)
			return new InsnNode(DCONST_1);
		return new LdcInsnNode(value);
	}

	/**
	 * @param insn
	 * 		Instruction that manipulates the stack.
	 *
	 * @return The number of items to the stack are pushed by the instruction.
	 * If the instruction {@code null} then {@code -1}.
	 * Instructions that pop from the stack do not yield negative numbers,
	 * for that use {@link #getPopCount(AbstractInsnNode)}.
	 */
	public static int getPushCount(AbstractInsnNode insn) {
		if (insn == null) return -1;
		int op = insn.getOpcode();
		int type = insn.getType();
		if (type == AbstractInsnNode.METHOD_INSN) {
			Type methodType = Type.getMethodType(((MethodInsnNode) insn).desc);
			return methodType.getReturnType().getSize();
		} else if (type == AbstractInsnNode.INVOKE_DYNAMIC_INSN) {
			Type methodType = Type.getMethodType(((InvokeDynamicInsnNode) insn).desc);
			return methodType.getReturnType().getSize();
		} else if (type == AbstractInsnNode.FIELD_INSN) {
			Type fieldType = Type.getType(((FieldInsnNode) insn).desc);
			return fieldType.getSize();
		} else if (type == AbstractInsnNode.LDC_INSN) {
			Object cst = ((LdcInsnNode) insn).cst;
			return (cst instanceof Double || cst instanceof Long) ? 2 : 1;
		} else if (type == AbstractInsnNode.JUMP_INSN ||
				type == AbstractInsnNode.FRAME ||
				type == AbstractInsnNode.LABEL ||
				type == AbstractInsnNode.LINE) {
			return 0;
		}
		switch (op) {
			// visitInsn
			case NOP:
				return 0;
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				return 1; // value
			case LCONST_0:
			case LCONST_1:
			case DCONST_0:
			case DCONST_1:
				return 2; // wide-value
			// visitIntInsn
			case BIPUSH:
			case SIPUSH:
				return 1; // value
			// visitVarInsn
			case ILOAD:
			case FLOAD:
			case ALOAD:
				return 1; // value
			case LLOAD:
			case DLOAD:
				return 2; // wide-value
			// visitInsn
			case IALOAD:
			case FALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return 1; // value
			case LALOAD:
			case DALOAD:
				return 2; // wide-value
			// visitVarInsn
			case ISTORE:
			case LSTORE:
			case FSTORE:
			case DSTORE:
			case ASTORE:
				return 0;
			// visitInsn
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return 0;
			case POP:
			case POP2:
				return 0;
			case DUP:
				return 1; // stack stuff
			case DUP_X1:
				return 1; // stack stuff
			case DUP_X2:
				return 1; // stack stuff
			case DUP2:
				return 2; // stack stuff
			case DUP2_X1:
				return 2; // stack stuff
			case DUP2_X2:
				return 2; // stack stuff
			case SWAP:
				return 0; // technically does not introduce

			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IOR:
				return 1; // result
			case LDIV:
			case LMUL:
			case LSUB:
			case LADD:
			case LREM:
			case LNEG:
			case LSHL:
			case LSHR:
			case LUSHR:
			case LAND:
			case LOR:
			case IXOR:
			case LXOR:
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
			case DNEG:
				return 2; // wide-result
			// visitIincInsn
			case IINC:
				return 0;
			// visitInsn

			case I2F:
			case L2I:
			case L2F:
			case F2I:
			case D2I:
			case D2F:
			case I2B:
			case I2C:
			case I2S:
				return 1; // result
			case I2D:
			case L2D:
			case F2D:
			case F2L:
			case D2L:
			case I2L:
				return 2; // wide-result
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				return 1; // result
			// visitVarInsn
			case RET:
				return 0;
			// visiTableSwitchInsn/visitLookupSwitch
			case TABLESWITCH:
			case LOOKUPSWITCH:
				return 0;
			// visitInsn
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
			case RETURN:
				return 0;
			// visitTypeInsn
			case NEW:
				return 1;
			// visitIntInsn
			case NEWARRAY:
				return 1;
			// visitTypeInsn
			case ANEWARRAY:
				return 1;
			// visitInsn
			case ARRAYLENGTH:
				return 1;
			case ATHROW:
				return 0;
			// visitTypeInsn
			case CHECKCAST:
				return 0; // technically does not introduce
			case INSTANCEOF:
				return 1; // result
			// visitInsn
			case MONITORENTER:
				return 0;
			case MONITOREXIT:
				return 0;
			// visitMultiANewArrayInsn
			case MULTIANEWARRAY:
				return 1; // array
			default:
				throw new IllegalArgumentException("Unhandled instruction: " + op);
		}
	}

	/**
	 * @param insn
	 * 		Instruction that manipulates the stack.
	 *
	 * @return The number of items from the stack popped by the instruction.
	 * If the instruction {@code null} then {@code -1}.
	 * Instructions that push to the stack do not yield negative numbers,
	 * for that use {@link #getPushCount(AbstractInsnNode)}.
	 */
	public static int getPopCount(AbstractInsnNode insn) {
		if (insn == null) return -1;
		int op = insn.getOpcode();
		int type = insn.getType();
		if (type == MULTIANEWARRAY_INSN) {
			return ((MultiANewArrayInsnNode) insn).dims;
		} else if (type == METHOD_INSN) {
			Type methodType = Type.getMethodType(((MethodInsnNode) insn).desc);
			int count = op == INVOKESTATIC ? 0 : 1;
			for (Type argType : methodType.getArgumentTypes()) {
				count += argType.getSize();
			}
			return count;
		} else if (type == INVOKE_DYNAMIC_INSN) {
			Type methodType = Type.getMethodType(((InvokeDynamicInsnNode) insn).desc);
			int count = 0;
			for (Type argType : methodType.getArgumentTypes()) {
				count += argType.getSize();
			}
			return count;
		} else if (type == FRAME || type == LABEL || type == LINE) {
			return 0;
		}
		switch (op) {
			// visitInsn
			case NOP:
			case ACONST_NULL:
			case ICONST_M1:
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case LCONST_0:
			case LCONST_1:
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
			case DCONST_0:
			case DCONST_1:
				return 0;
			// visitIntInsn
			case BIPUSH:
			case SIPUSH:
				return 0;
			// visitLdcInsn
			case LDC:
				return 0;
			// visitVarInsn
			case ILOAD:
			case LLOAD:
			case FLOAD:
			case DLOAD:
			case ALOAD:
				return 0;
			// visitInsn
			case IALOAD:
			case LALOAD:
			case FALOAD:
			case DALOAD:
			case AALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return 2; // arrayref, index
			// visitVarInsn
			case ISTORE:
			case FSTORE:
			case ASTORE:
				return 1; // value
			case DSTORE:
			case LSTORE:
				return 2; // wide-value

			// visitInsn
			case IASTORE:
			case FASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				return 3; // arrayref, index, value
			case DASTORE:
			case LASTORE:
				return 4; // arrayref, index, wide-value
			case POP:
				return 1; // value
			case POP2:
				return 2; // value x2 or wide-value
			case DUP:
			case DUP_X1:
			case DUP_X2:
			case DUP2:
			case DUP2_X1:
			case DUP2_X2:
			case SWAP:
				return 0; // Does not "consume" technically
			case IADD:
			case FADD:
			case ISUB:
			case FSUB:
			case IMUL:
			case FMUL:
			case IDIV:
			case FDIV:
			case IREM:
			case FREM:
			case INEG:
			case FNEG:
			case ISHL:
			case ISHR:
			case IUSHR:
			case IAND:
			case IXOR:
			case IOR:
				return 2; // value x2
			case DNEG:
			case DREM:
			case DDIV:
			case DMUL:
			case DSUB:
			case DADD:
			case LUSHR:
			case LSHR:
			case LSHL:
			case LNEG:
			case LREM:
			case LDIV:
			case LMUL:
			case LSUB:
			case LADD:
			case LAND:
			case LOR:
			case LXOR:
				return 4; // wide-value x2
			// visitIincInsn
			case IINC:
				return 0;
			// visitInsn
			case I2L:
			case I2F:
			case I2D:
			case F2I:
			case F2L:
			case F2D:
			case I2B:
			case I2C:
			case I2S:
				return 1; // value
			case D2I:
			case D2L:
			case D2F:
			case L2I:
			case L2F:
			case L2D:
				return 2;
			case FCMPL:
			case FCMPG:
				return 2; // wide-value
			case LCMP:
			case DCMPL:
			case DCMPG:
				return 4; // wide-value x2
			// visitJumpInsn
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFNULL:
			case IFNONNULL:
				return 1; // value
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				return 2; // value x2
			case GOTO:
				return 0;
			case JSR:
				return 0;
			// visitVarInsn
			case RET:
				return 0;
			// visiTableSwitchInsn/visitLookupSwitch
			case TABLESWITCH:
			case LOOKUPSWITCH:
				return 1; // value
			// visitInsn
			case IRETURN:
			case LRETURN:
			case FRETURN:
			case DRETURN:
			case ARETURN:
				return 1; // return-value
			case RETURN:
				return 0;
			// visitFieldInsn
			case GETSTATIC:
				return 0;
			case PUTSTATIC:
				return 1; // value
			case GETFIELD:
				return 1; // owner
			case PUTFIELD:
				return 2; // owner, value
			// visitTypeInsn
			case NEW:
				return 0;
			// visitIntInsn
			case NEWARRAY:
				return 1; // count
			// visitTypeInsn
			case ANEWARRAY:
				return 1; // count
			// visitInsn
			case ARRAYLENGTH:
				return 1; // array
			case ATHROW:
				return 1; // exception
			// visitTypeInsn
			case CHECKCAST:
				return 0; // instance to verify, not technically consumed but referenced
			case INSTANCEOF:
				return 1; // value
			// visitInsn
			case MONITORENTER:
			case MONITOREXIT:
				return 1; // monitor
			default:
				throw new IllegalArgumentException("Unhandled instruction: " + op);
		}
	}

	/**
	 * Replaces the given insn with a {@code NOP}.
	 * Silently ignores errors when the instruction is not contained in the list.
	 *
	 * @param instructions
	 * 		Containing instruction list.
	 * @param insn
	 * 		Instruction to replace with nop.
	 */
	public static void nop(InsnList instructions, AbstractInsnNode insn) {
		try {
			instructions.set(insn, new InsnNode(NOP));
		} catch (IndexOutOfBoundsException ignored) {
			// It's faster to fail/handle here than to call 'contains' or reflectively check if we can replace 'insn'.
			// The failure occurs because 'AbstractInsnNode.index' is '-1' if the 'insn' is not in the 'InsnList'.
		}
	}
}
