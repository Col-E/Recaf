package me.coley.recaf.bytecode.analysis;

import java.util.List;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/**
 * Extension of BasicInterpreter that uses extensions of BasicValue to provide
 * additional information for stack analysis.
 * 
 * @author Matt
 */
public class BasicPlusInterpreter extends BasicInterpreter {
	public BasicPlusInterpreter() {
		super(Opcodes.ASM6);
	}

	@Override
	public BasicValue newOperation(final AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case ACONST_NULL:
			return newValue(NULL_TYPE);
		case ICONST_M1:
		case ICONST_0:
		case ICONST_1:
		case ICONST_2:
		case ICONST_3:
		case ICONST_4:
		case ICONST_5:
			return BasicValue.INT_VALUE;
		case LCONST_0:
		case LCONST_1:
			return BasicValue.LONG_VALUE;
		case FCONST_0:
		case FCONST_1:
		case FCONST_2:
			return BasicValue.FLOAT_VALUE;
		case DCONST_0:
		case DCONST_1:
			return BasicValue.DOUBLE_VALUE;
		case BIPUSH:
		case SIPUSH:
			return BasicValue.INT_VALUE;
		case LDC:
			Object value = ((LdcInsnNode) insn).cst;
			if (value instanceof Integer) {
				return BasicValue.INT_VALUE;
			} else if (value instanceof Float) {
				return BasicValue.FLOAT_VALUE;
			} else if (value instanceof Long) {
				return BasicValue.LONG_VALUE;
			} else if (value instanceof Double) {
				return BasicValue.DOUBLE_VALUE;
			} else if (value instanceof String) {
				return newValue(Type.getObjectType("java/lang/String"));
			} else if (value instanceof Type) {
				int sort = ((Type) value).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY) {
					return newValue(Type.getObjectType("java/lang/Class"));
				} else if (sort == Type.METHOD) {
					return newValue(Type.getObjectType("java/lang/invoke/MethodType"));
				} else {
					throw new AnalyzerException(insn, "Illegal LDC value " + value);
				}
			} else if (value instanceof Handle) {
				return newValue(Type.getObjectType("java/lang/invoke/MethodHandle"));
			} else if (value instanceof ConstantDynamic) {
				return newValue(Type.getType(((ConstantDynamic) value).getDescriptor()));
			} else {
				throw new AnalyzerException(insn, "Illegal LDC value " + value);
			}
		case JSR:
			return BasicValue.RETURNADDRESS_VALUE;
		case GETSTATIC:
			return newValue(Type.getType(((FieldInsnNode) insn).desc));
		case NEW:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
		default:
			throw new AssertionError();
		}
	}

	@Override
	public BasicValue unaryOperation(final AbstractInsnNode insn, final BasicValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
		case INEG:
		case IINC:
		case L2I:
		case F2I:
		case D2I:
		case I2B:
		case I2C:
		case I2S:
			return BasicValue.INT_VALUE;
		case FNEG:
		case I2F:
		case L2F:
		case D2F:
			return BasicValue.FLOAT_VALUE;
		case LNEG:
		case I2L:
		case F2L:
		case D2L:
			return BasicValue.LONG_VALUE;
		case DNEG:
		case I2D:
		case L2D:
		case F2D:
			return BasicValue.DOUBLE_VALUE;
		case IFEQ:
		case IFNE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case TABLESWITCH:
		case LOOKUPSWITCH:
		case IRETURN:
		case LRETURN:
		case FRETURN:
		case DRETURN:
		case ARETURN:
		case PUTSTATIC:
			return null;
		case GETFIELD:
			return newValue(Type.getType(((FieldInsnNode) insn).desc));
		case NEWARRAY:
			switch (((IntInsnNode) insn).operand) {
			case T_BOOLEAN:
				return newValue(Type.getType("[Z"));
			case T_CHAR:
				return newValue(Type.getType("[C"));
			case T_BYTE:
				return newValue(Type.getType("[B"));
			case T_SHORT:
				return newValue(Type.getType("[S"));
			case T_INT:
				return newValue(Type.getType("[I"));
			case T_FLOAT:
				return newValue(Type.getType("[F"));
			case T_DOUBLE:
				return newValue(Type.getType("[D"));
			case T_LONG:
				return newValue(Type.getType("[J"));
			default:
				break;
			}
			throw new AnalyzerException(insn, "Invalid array type");
		case ANEWARRAY:
			return newValue(Type.getType("[" + Type.getObjectType(((TypeInsnNode) insn).desc)));
		case ARRAYLENGTH:
			return BasicValue.INT_VALUE;
		case ATHROW:
			return null;
		case CHECKCAST:
			return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
		case INSTANCEOF:
			return BasicValue.INT_VALUE;
		case MONITORENTER:
		case MONITOREXIT:
		case IFNULL:
		case IFNONNULL:
			return null;
		default:
			throw new AssertionError();
		}
	}

	@Override
	public BasicValue binaryOperation(final AbstractInsnNode insn, final BasicValue value1, final BasicValue value2)
			throws AnalyzerException {
		switch (insn.getOpcode()) {
		case IALOAD:
		case BALOAD:
		case CALOAD:
		case SALOAD:
		case IADD:
		case ISUB:
		case IMUL:
		case IDIV:
		case IREM:
		case ISHL:
		case ISHR:
		case IUSHR:
		case IAND:
		case IOR:
		case IXOR:
			return BasicValue.INT_VALUE;
		case FALOAD:
		case FADD:
		case FSUB:
		case FMUL:
		case FDIV:
		case FREM:
			return BasicValue.FLOAT_VALUE;
		case LALOAD:
		case LADD:
		case LSUB:
		case LMUL:
		case LDIV:
		case LREM:
		case LSHL:
		case LSHR:
		case LUSHR:
		case LAND:
		case LOR:
		case LXOR:
			return BasicValue.LONG_VALUE;
		case DALOAD:
		case DADD:
		case DSUB:
		case DMUL:
		case DDIV:
		case DREM:
			return BasicValue.DOUBLE_VALUE;
		case AALOAD:
			return BasicValue.REFERENCE_VALUE;
		case LCMP:
		case FCMPL:
		case FCMPG:
		case DCMPL:
		case DCMPG:
			return BasicValue.INT_VALUE;
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case PUTFIELD:
			return null;
		default:
			throw new AssertionError();
		}
	}

	@Override
	public BasicValue naryOperation(final AbstractInsnNode insn, final List<? extends BasicValue> values)
			throws AnalyzerException {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
		} else if (opcode == INVOKEDYNAMIC) {
			return newValue(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
		} else {
			return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
		}
	}
}
