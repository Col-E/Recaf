package me.coley.recaf.parse.bytecode;

import me.coley.recaf.util.OpcodeUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;

/**
 * A modified version of ASM's {@link BasicVerifier} to use {@link RValue}.<br>
 * Additionally, a few extra verification steps are taken and simple math and types are calculated.
 *
 * @author Matt
 */
public class RInterpreter extends Interpreter<RValue> {
	RInterpreter() {
		super(Opcodes.ASM7);
	}

	@Override
	public RValue newValue(Type type) {
		if (type == null)
			return RValue.UNINITIALIZED;
		return RValue.of(type);
	}

	@Override
	public RValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
			case ACONST_NULL:
				return RValue.NULL;
			case ICONST_M1:
				return RValue.of(-1);
			case ICONST_0:
				return RValue.of(0);
			case ICONST_1:
				return RValue.of(1);
			case ICONST_2:
				return RValue.of(2);
			case ICONST_3:
				return RValue.of(3);
			case ICONST_4:
				return RValue.of(4);
			case ICONST_5:
				return RValue.of(5);
			case LCONST_0:
				return RValue.of(0L);
			case LCONST_1:
				return RValue.of(1L);
			case FCONST_0:
				return RValue.of(0.0F);
			case FCONST_1:
				return RValue.of(1.0F);
			case FCONST_2:
				return RValue.of(2.0F);
			case DCONST_0:
				return RValue.of(0.0);
			case DCONST_1:
				return RValue.of(1.0);
			case BIPUSH:
			case SIPUSH:
				return RValue.of(((IntInsnNode) insn).operand);
			case LDC:
				Object value = ((LdcInsnNode) insn).cst;
				if (value instanceof Integer) {
					return RValue.of((int) value);
				} else if (value instanceof Float) {
					return RValue.of((float) value);
				} else if (value instanceof Long) {
					return RValue.of((long) value);
				} else if (value instanceof Double) {
					return RValue.of((double) value);
				} else if (value instanceof String) {
					return RValue.of((String) value);
				} else if (value instanceof Type) {
					Type type =  (Type) value;
					int sort = type.getSort();
					if (sort == Type.OBJECT || sort == Type.ARRAY ) {
						return RValue.ofClass(Type.getObjectType("java/lang/Class"), type);
					} else if (sort == Type.METHOD) {
						return RValue.ofVirtual(Type.getObjectType("java/lang/invoke/MethodType"));
					} else {
						throw new AnalyzerException(insn, "Illegal LDC value " + value);
					}
				} else if (value instanceof Handle) {
					return RValue.ofVirtual(Type.getObjectType("java/lang/invoke/MethodHandle"));
				} else if (value instanceof ConstantDynamic) {
					return RValue.ofVirtual(Type.getType(((ConstantDynamic) value).getDescriptor()));
				} else {
					throw new AnalyzerException(insn, "Illegal LDC value " + value);
				}
			case JSR:
				return RValue.RETURNADDRESS_VALUE;
			case GETSTATIC:
				return RValue.ofVirtual(Type.getType(((FieldInsnNode) insn).desc));
			case NEW:
				return newValue(Type.getObjectType(((TypeInsnNode) insn).desc));
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public RValue copyOperation(AbstractInsnNode insn, RValue value) throws AnalyzerException {
		// Fetch type from instruction
		Type insnType = null;
		switch(insn.getOpcode()) {
			case ILOAD:
			case ISTORE:
				insnType = Type.INT_TYPE;
				break;
			case LLOAD:
			case LSTORE:
				insnType = Type.LONG_TYPE;
				break;
			case FLOAD:
			case FSTORE:
				insnType = Type.FLOAT_TYPE;
				break;
			case DLOAD:
			case DSTORE:
				insnType = Type.DOUBLE_TYPE;
				break;
			case ALOAD:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected a reference type.");
				insnType = value.getType();
				break;
			case ASTORE:
				if (!value.isReference() && !RValue.RETURNADDRESS_VALUE.equals(value))
					throw new AnalyzerException(insn, "Expected a reference or return-address type.");
				insnType = value.getType();
				break;
			default:
				break;
		}
		// Very simple type verification, don't try to mix primitives and non-primitives
		Type argType = value.getType();
		if(insnType != null && argType != null) {
			if(insnType.getSort() == Type.OBJECT && argType.getSort() < Type.ARRAY)
				throw new AnalyzerException(insn, "Cannot mix primitive with type-variable instruction " +
						OpcodeUtil.opcodeToName(insn.getOpcode()));
			else if(argType.getSort() == Type.OBJECT && insnType.getSort() < Type.ARRAY)
				throw new AnalyzerException(insn, "Cannot mix type with primitive-variable instruction " +
						OpcodeUtil.opcodeToName(insn.getOpcode()));
		}
		if (value.getType() == null)
			return RValue.ofVirtual(insnType);
		return value;
	}

	@Override
	public RValue unaryOperation(AbstractInsnNode insn, RValue value) throws AnalyzerException {
		switch(insn.getOpcode()) {
			case INEG:
				return RValue.of(-(int) value.getValue());
			case IINC:
				return RValue.of(((IincInsnNode) insn).incr);
			case L2I:
			case F2I:
			case D2I:
			case I2B:
			case I2C:
			case I2S:
				return RValue.of((int) value.getValue());
			case FNEG:
				return RValue.of(-(float) value.getValue());
			case I2F:
			case L2F:
			case D2F:
				return RValue.of((float) value.getValue());
			case LNEG:
				return RValue.of(-(long) value.getValue());
			case I2L:
			case F2L:
			case D2L:
				return RValue.of((long) value.getValue());
			case DNEG:
				return RValue.of(-(double) value.getValue());
			case I2D:
			case L2D:
			case F2D:
				return RValue.of((double) value.getValue());
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case TABLESWITCH:
			case LOOKUPSWITCH:
				if (!(isSubTypeOf(value.getType(), Type.INT_TYPE) || isSubTypeOf(value.getType(), Type.BOOLEAN_TYPE)))
					throw new AnalyzerException(insn, "Expected int type.");
				return null;
			case IRETURN:
				if (!(isSubTypeOf(value.getType(), Type.INT_TYPE) || isSubTypeOf(value.getType(), Type.BOOLEAN_TYPE)))
					throw new AnalyzerException(insn, "Expected int return type.");
				return null;
			case LRETURN:
				if (!isSubTypeOf(value.getType(), Type.LONG_TYPE))
					throw new AnalyzerException(insn, "Expected long return type.");
				return null;
			case FRETURN:
				if (!isSubTypeOf(value.getType(), Type.FLOAT_TYPE))
					throw new AnalyzerException(insn, "Expected float return type.");
				return null;
			case DRETURN:
				if (!isSubTypeOf(value.getType(), Type.DOUBLE_TYPE))
					throw new AnalyzerException(insn, "Expected double return type.");
				return null;
			case ARETURN:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected a reference type.");
				return null;
			case PUTSTATIC:
				if (!isSubTypeOf(value.getType(), Type.INT_TYPE))
					throw new AnalyzerException(insn, "Expected int type.");
				return null;
			case GETFIELD:
				FieldInsnNode fin = (FieldInsnNode) insn;
				if (!isSubTypeOf(value.getType(), Type.getObjectType(fin.owner)))
					throw new AnalyzerException(insn, "Expected type: " + fin.owner);
				return RValue.ofVirtual(Type.getType(fin.desc));
			case NEWARRAY:
				switch(((IntInsnNode) insn).operand) {
					case T_BOOLEAN:
						return RValue.ofVirtual(Type.getType("[Z"));
					case T_CHAR:
						return RValue.ofVirtual(Type.getType("[C"));
					case T_BYTE:
						return RValue.ofVirtual(Type.getType("[B"));
					case T_SHORT:
						return RValue.ofVirtual(Type.getType("[S"));
					case T_INT:
						return RValue.ofVirtual(Type.getType("[I"));
					case T_FLOAT:
						return RValue.ofVirtual(Type.getType("[F"));
					case T_DOUBLE:
						return RValue.ofVirtual(Type.getType("[D"));
					case T_LONG:
						return RValue.ofVirtual(Type.getType("[J"));
					default:
						break;
				}
				throw new AnalyzerException(insn, "Invalid array type");
			case ANEWARRAY:
				return RValue.ofVirtual(Type.getType("[" + Type.getObjectType(((TypeInsnNode) insn).desc)));
			case ARRAYLENGTH:
				if (value.getValue() instanceof RVirtual && !((RVirtual) value.getValue()).isArray())
					throw new AnalyzerException(insn, "Expected an array type.");
				return RValue.of(Type.INT_TYPE);
			case ATHROW:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Missing exception type on stack.");
				return null;
			case CHECKCAST:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected a reference type.");
				return RValue.ofVirtual(Type.getObjectType(((TypeInsnNode) insn).desc));
			case INSTANCEOF:
				return RValue.of(Type.INT_TYPE);
			case MONITORENTER:
			case MONITOREXIT:
			case IFNULL:
			case IFNONNULL:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected a reference type.");
				return null;
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public RValue binaryOperation(AbstractInsnNode insn, RValue value1, RValue value2) throws AnalyzerException {
		// Modified from BasicVerifier
		Type expected1;
		Type expected2;
		switch (insn.getOpcode()) {
			case IALOAD:
				expected1 = Type.getType("[I");
				expected2 = Type.INT_TYPE;
				break;
			case BALOAD:
				if (isSubTypeOf(value1.getType(), Type.getType("[Z"))) {
					expected1 = Type.getType("[Z");
				} else {
					expected1 = Type.getType("[B");
				}
				expected2 = Type.INT_TYPE;
				break;
			case CALOAD:
				expected1 = Type.getType("[C");
				expected2 = Type.INT_TYPE;
				break;
			case SALOAD:
				expected1 = Type.getType("[S");
				expected2 = Type.INT_TYPE;
				break;
			case LALOAD:
				expected1 = Type.getType("[J");
				expected2 = Type.INT_TYPE;
				break;
			case FALOAD:
				expected1 = Type.getType("[F");
				expected2 = Type.INT_TYPE;
				break;
			case DALOAD:
				expected1 = Type.getType("[D");
				expected2 = Type.INT_TYPE;
				break;
			case AALOAD:
				expected1 = Type.getType("[Ljava/lang/Object;");
				expected2 = Type.INT_TYPE;
				break;
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
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
				expected1 = Type.INT_TYPE;
				expected2 = Type.INT_TYPE;
				break;
			case FADD:
			case FSUB:
			case FMUL:
			case FDIV:
			case FREM:
			case FCMPL:
			case FCMPG:
				expected1 = Type.FLOAT_TYPE;
				expected2 = Type.FLOAT_TYPE;
				break;
			case LADD:
			case LSUB:
			case LMUL:
			case LDIV:
			case LREM:
			case LAND:
			case LOR:
			case LXOR:
			case LCMP:
				expected1 = Type.LONG_TYPE;
				expected2 = Type.LONG_TYPE;
				break;
			case LSHL:
			case LSHR:
			case LUSHR:
				expected1 = Type.LONG_TYPE;
				expected2 = Type.INT_TYPE;
				break;
			case DADD:
			case DSUB:
			case DMUL:
			case DDIV:
			case DREM:
			case DCMPL:
			case DCMPG:
				expected1 = Type.DOUBLE_TYPE;
				expected2 = Type.DOUBLE_TYPE;
				break;
			case IF_ACMPEQ:
			case IF_ACMPNE:
				expected1 = Type.getObjectType("java/lang/Object");
				expected2 = Type.getObjectType("java/lang/Object");
				break;
			case PUTFIELD:
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				expected1 = Type.getObjectType(fieldInsn.owner);
				expected2 = Type.getType(fieldInsn.desc);
				break;
			default:
				throw new IllegalStateException();
		}
		if (value1 != RValue.UNINITIALIZED && value2 != RValue.UNINITIALIZED)
			if (!isSubTypeOf(value1.getType(), expected1))
				throw new AnalyzerException(insn, "First argument not of expected type", expected1, value1);
			else if (!isSubTypeOf(value2.getType(), expected2))
				throw new AnalyzerException(insn, "Second argument not of expected type", expected2, value2);
		// Update values
		switch(insn.getOpcode()) {
			case IADD:
				return value1.add(value2);
			case ISUB:
				return value1.sub(value2);
			case IMUL:
				return value1.mul(value2);
			case IDIV:
				return value1.div(value2);
			case IREM:
				return value1.rem(value2);
			case ISHL:
				return value1.shl(value2);
			case ISHR:
				return value1.shr(value2);
			case IUSHR:
				return value1.ushr(value2);
			case IAND:
				return value1.and(value2);
			case IOR:
				return value1.or(value2);
			case IXOR:
				return value1.xor(value2);
			case FADD:
				return value1.add(value2);
			case FSUB:
				return value1.sub(value2);
			case FMUL:
				return value1.mul(value2);
			case FDIV:
				return value1.div(value2);
			case FREM:
				return value1.rem(value2);
			case LADD:
				return value1.add(value2);
			case LSUB:
				return value1.sub(value2);
			case LMUL:
				return value1.mul(value2);
			case LDIV:
				return value1.div(value2);
			case LREM:
				return value1.rem(value2);
			case LSHL:
				return value1.shl(value2);
			case LSHR:
				return value1.shr(value2);
			case LUSHR:
				return value1.ushr(value2);
			case LAND:
				return value1.and(value2);
			case LOR:
				return value1.or(value2);
			case LXOR:
				return value1.xor(value2);
			case DADD:
				return value1.add(value2);
			case DSUB:
				return value1.sub(value2);
			case DMUL:
				return value1.mul(value2);
			case DDIV:
				return value1.div(value2);
			case DREM:
				return value1.rem(value2);
			case FALOAD:
				return RValue.of(Type.FLOAT_TYPE);
			case LALOAD:
				return RValue.of(Type.LONG_TYPE);
			case DALOAD:
				return RValue.of(Type.DOUBLE_TYPE);
			case AALOAD:
				if (value1.getType() == null)
					return RValue.ofVirtual(Type.getObjectType("java/lang/Object"));
				else
					return RValue.ofVirtual(Type.getType(value1.getType().getDescriptor().substring(1)));
			case IALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return RValue.of(Type.INT_TYPE);
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				if (value1.getValue() == null || value2.getValue() == null)
					return RValue.of(Type.INT_TYPE);
				double v1 = (double) value1.getValue();
				double v2 = (double) value1.getValue();
				if(v1 > v2)
					return RValue.of(1);
				else if(v1 < v2)
					return RValue.of(-1);
				else
					return RValue.of(0);
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
				throw new IllegalStateException();
		}
	}

	@Override
	public RValue ternaryOperation(AbstractInsnNode insn, RValue value1, RValue value2,
								   RValue value3) throws AnalyzerException {
		Type expected1;
		Type expected3;
		switch(insn.getOpcode()) {
			case IASTORE:
				expected1 = Type.getType("[I");
				expected3 = Type.INT_TYPE;
				break;
			case BASTORE:
				if(isSubTypeOf(value1.getType(), Type.getType("[Z"))) {
					expected1 = Type.getType("[Z");
				} else {
					expected1 = Type.getType("[B");
				}
				expected3 = Type.INT_TYPE;
				break;
			case CASTORE:
				expected1 = Type.getType("[C");
				expected3 = Type.INT_TYPE;
				break;
			case SASTORE:
				expected1 = Type.getType("[S");
				expected3 = Type.INT_TYPE;
				break;
			case LASTORE:
				expected1 = Type.getType("[J");
				expected3 = Type.LONG_TYPE;
				break;
			case FASTORE:
				expected1 = Type.getType("[F");
				expected3 = Type.FLOAT_TYPE;
				break;
			case DASTORE:
				expected1 = Type.getType("[D");
				expected3 = Type.DOUBLE_TYPE;
				break;
			case AASTORE:
				expected1 = value1.getType();
				expected3 = Type.getObjectType("java/lang/Object");
				break;
			default:
				throw new AssertionError();
		}
		if(!isSubTypeOf(value1.getType(), expected1))
			throw new AnalyzerException(insn, "First argument not of expected type", expected1, value1);
		else if(!Type.INT_TYPE.equals(value2.getType()))
			throw new AnalyzerException(insn, "Second argument not an integer", BasicValue.INT_VALUE, value2);
		else if(!isSubTypeOf(value3.getType(), expected3))
			throw new AnalyzerException(insn, "Second argument not of expected type", expected3, value3);
		return null;
	}

	@Override
	public RValue naryOperation(AbstractInsnNode insn, List<? extends RValue> values) throws AnalyzerException {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			// Validate args
			for (RValue value : values)
				if (!Type.INT_TYPE.equals(value.getType()))
					throw new AnalyzerException(insn, null, RValue.of(Type.INT_TYPE), value);
			return RValue.ofVirtual(Type.getType(((MultiANewArrayInsnNode) insn).desc));
		} else {
			// From BasicVerifier
			int i = 0;
			int j = 0;
			if(opcode != INVOKESTATIC && opcode != INVOKEDYNAMIC) {
				Type owner = Type.getObjectType(((MethodInsnNode) insn).owner);
				if(!isSubTypeOf(values.get(i++).getType(), owner))
					throw new AnalyzerException(insn, "Method owner does not match type on stack",
							newValue(owner), values.get(0));
			}
			String methodDescriptor = (opcode == INVOKEDYNAMIC) ?
					((InvokeDynamicInsnNode) insn).desc :
					((MethodInsnNode) insn).desc;
			Type[] args = Type.getArgumentTypes(methodDescriptor);
			while(i < values.size()) {
				Type expected = args[j++];
				Type actual = values.get(i++).getType();
				if(!isSubTypeOf(actual, expected)) {
					throw new AnalyzerException(insn, "Argument type was \"" + actual +
							"\" but expected \"" + expected + "\"");
				}
			}
			// Get value
			if (opcode == INVOKEDYNAMIC) {
				return RValue.ofVirtual(Type.getReturnType(((InvokeDynamicInsnNode) insn).desc));
			} else if (opcode == INVOKESTATIC) {
				return RValue.ofVirtual(Type.getReturnType(((MethodInsnNode) insn).desc));
			} else {
				// INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE
				RValue ownerValue = values.get(0);
				if(ownerValue.equals(RValue.UNINITIALIZED))
					throw new AnalyzerException(insn, "Cannot call method on uninitialized reference");
				else if(ownerValue.equals(RValue.NULL))
					throw new AnalyzerException(insn, "Cannot call method on null reference");
				return ownerValue.ref(Type.getMethodType(((MethodInsnNode)insn).desc));
			}
		}
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, RValue value, RValue expected) throws AnalyzerException {
		if(!isSubTypeOf(value.getType(), expected.getType()))
			throw new AnalyzerException(insn, "Incompatible return type", expected, value);
	}

	@Override
	public RValue merge(RValue value1, RValue value2) {
		if (!value1.canMerge(value2))
			return RValue.UNINITIALIZED;
		return value1;
	}

	private static boolean isSubTypeOf(Type type, Type expected) {
		// Can't handle null type
		if (type == null)
			return false;
		// Look at array element type
		boolean wasArray = type.getSort() == Type.ARRAY;
		while (type.getSort() == Type.ARRAY && expected.getSort() == Type.ARRAY) {
			type = type.getElementType();
			expected = expected.getElementType();
		}
		// Check just in case
		if (expected == null)
			return false;
		// All things are objects
		if ((wasArray || type.getSort() >= Type.ARRAY) &&
				expected.getDescriptor().equals("Ljava/lang/Object;"))
			return true;
		// Ensure sorts are same
		if (type.getSort() == expected.getSort()) {
			if (expected.equals(type))
				return true;
			RValue host = RValue.of(type);
			return host != null && host.canMerge(RValue.of(expected));
		}
		return false;
	}
}
