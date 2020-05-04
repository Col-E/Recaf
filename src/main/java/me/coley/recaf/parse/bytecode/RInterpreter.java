package me.coley.recaf.parse.bytecode;

import me.coley.recaf.parse.bytecode.exception.LoggedAnalyzerException;
import me.coley.recaf.util.InsnUtil;
import me.coley.recaf.util.OpcodeUtil;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * A modified version of ASM's {@link BasicVerifier} to use {@link RValue}.<br>
 * Additionally, a few extra verification steps are taken and simple math and types are calculated.
 *
 * @author Matt
 */
public class RInterpreter extends Interpreter<RValue> {
	private final Map<AbstractInsnNode, AnalyzerException> badTypeInsns = new HashMap<>();

	RInterpreter() {
		super(Opcodes.ASM8);
	}

	// TODO: Make all of these LoggedAnalyzerException where applicable

	/**
	 * @return Map of instructions to their thrown analyzer errors.
	 */
	public Map<AbstractInsnNode, AnalyzerException> getProblemInsns() {
		return badTypeInsns;
	}

	/**
	 * @return {@code true}  when problems have been reported.
	 */
	public boolean hasReportedProblems() {
		return !badTypeInsns.isEmpty();
	}

	@Override
	public RValue newValue(Type type) {
		if (type == null)
			return RValue.UNINITIALIZED;
		else if (type == Type.VOID_TYPE)
			return null;
		return RValue.ofVirtual(type);
	}



	@Override
	public RValue newParameterValue(boolean isInstanceMethod, int local, Type type) {
		return RValue.ofVirtual(type);
	}

	@Override
	public RValue newExceptionValue(TryCatchBlockNode tryCatch,
									Frame<RValue> handlerFrame, Type exceptionType) {
		return RValue.ofVirtual(exceptionType);
	}

	@Override
	public RValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
			case ACONST_NULL:
				return RValue.NULL;
			case ICONST_M1:
				return RValue.ofInt(-1);
			case ICONST_0:
				return RValue.ofInt(0);
			case ICONST_1:
				return RValue.ofInt(1);
			case ICONST_2:
				return RValue.ofInt(2);
			case ICONST_3:
				return RValue.ofInt(3);
			case ICONST_4:
				return RValue.ofInt(4);
			case ICONST_5:
				return RValue.ofInt(5);
			case LCONST_0:
				return RValue.ofLong(0L);
			case LCONST_1:
				return RValue.ofLong(1L);
			case FCONST_0:
				return RValue.ofFloat(0.0F);
			case FCONST_1:
				return RValue.ofFloat(1.0F);
			case FCONST_2:
				return RValue.ofFloat(2.0F);
			case DCONST_0:
				return RValue.ofDouble(0.0);
			case DCONST_1:
				return RValue.ofDouble(1.0);
			case BIPUSH:
			case SIPUSH:
				return RValue.ofInt(((IntInsnNode) insn).operand);
			case LDC:
				Object value = ((LdcInsnNode) insn).cst;
				if (value instanceof Integer) {
					return RValue.ofInt((int) value);
				} else if (value instanceof Float) {
					return RValue.ofFloat((float) value);
				} else if (value instanceof Long) {
					return RValue.ofLong((long) value);
				} else if (value instanceof Double) {
					return RValue.ofDouble((double) value);
				} else if (value instanceof String) {
					return RValue.ofString((String) value);
				} else if (value instanceof Type) {
					Type type =  (Type) value;
					int sort = type.getSort();
					if (sort == Type.OBJECT || sort == Type.ARRAY) {
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
				Type type = Type.getType(((FieldInsnNode) insn).desc);
				return RValue.ofVirtual(type);
			case NEW:
				return RValue.ofVirtual(Type.getObjectType(((TypeInsnNode) insn).desc));
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public RValue copyOperation(AbstractInsnNode insn, RValue value) throws AnalyzerException {
		// Fetch type from instruction
		Type insnType = null;
		boolean load = false;
		switch(insn.getOpcode()) {
			case ILOAD:
				load = true;
			case ISTORE:
				insnType = Type.INT_TYPE;
				break;
			case LLOAD:
				load = true;
			case LSTORE:
				insnType = Type.LONG_TYPE;
				break;
			case FLOAD:
				load = true;
			case FSTORE:
				insnType = Type.FLOAT_TYPE;
				break;
			case DLOAD:
				load = true;
			case DSTORE:
				insnType = Type.DOUBLE_TYPE;
				break;
			case ALOAD:
				load = true;
				if (!value.isUninitialized() && !value.isReference())
					throw new AnalyzerException(insn, "Expected a reference type.");
				insnType = value.getType();
				break;
			case ASTORE:
				if (!value.isReference() && !value.isJsrRet())
					throw new AnalyzerException(insn, "Expected a reference or return-address type.");
				insnType = value.getType();
				break;
			default:
				break;
		}
		// Very simple type verification, don't try to mix primitives and non-primitives
		Type argType = value.getType();
		if(insnType != null && argType != null) {
			if(insnType.getSort() == Type.OBJECT && isPrimitive(argType))
				throw new AnalyzerException(insn, "Cannot mix primitive with type-variable instruction " +
						OpcodeUtil.opcodeToName(insn.getOpcode()));
			else if(argType.getSort() == Type.OBJECT && isPrimitive(insnType))
				throw new AnalyzerException(insn, "Cannot mix type with primitive-variable instruction " +
						OpcodeUtil.opcodeToName(insn.getOpcode()));
		}

		// If we're operating on a load-instruction we want the return value to
		// relate to the type of the instruction.
		if(load && insnType != value.getType())
			return RValue.ofVirtual(insnType);
		return value;
	}

	@Override
	public RValue unaryOperation(AbstractInsnNode insn, RValue value) throws AnalyzerException {
		switch(insn.getOpcode()) {
			case INEG:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.INT_TYPE);
				return RValue.ofInt(-toInt(value));
			case IINC:
				return RValue.ofInt(((IincInsnNode) insn).incr);
			case L2I:
			case F2I:
			case D2I:
			case I2B:
			case I2C:
			case I2S:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.INT_TYPE);
				return RValue.ofInt(toInt(value));
			case FNEG:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.FLOAT_TYPE);
				return RValue.ofFloat(-toFloat(value));
			case I2F:
			case L2F:
			case D2F:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.FLOAT_TYPE);
				return RValue.ofFloat(toFloat(value));
			case LNEG:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.LONG_TYPE);
				return RValue.ofLong(-toLong(value));
			case I2L:
			case F2L:
			case D2L:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.LONG_TYPE);
				return RValue.ofLong(toLong(value));
			case DNEG:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.DOUBLE_TYPE);
				return RValue.ofDouble(-toDouble(value));
			case I2D:
			case L2D:
			case F2D:
				if (isValueUnknown(value))
					return RValue.ofVirtual(Type.DOUBLE_TYPE);
				return RValue.ofDouble(toDouble(value));
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
					throw new AnalyzerException(insn, "Expected reference return type");
				return null;
			case PUTSTATIC: {
				// Value == item on stack
				FieldInsnNode fin = (FieldInsnNode) insn;
				Type fieldType = Type.getType(fin.desc);
				if (!isSubTypeOf(value.getType(), fieldType))
					markBad(insn, new LoggedAnalyzerException((methodNode, frames) -> {
						// Validate that the argument value is no longer null when stack-frames are filled out
						Frame<RValue> frame = frames[InsnUtil.index(insn)];
						RValue methodContext = frame.getStack(frame.getStackSize() - 1);
						return isSubTypeOfOrNull(methodContext, fieldType);
					}, insn, "Expected " +
							"type: " + fieldType));
				return null;
			}
			case GETFIELD: {
				// Value == field owner instance
				// - Check instance context is of the owner class
				FieldInsnNode fin = (FieldInsnNode) insn;
				Type ownerType = Type.getObjectType(fin.owner);
				if (!isSubTypeOf(value.getType(), ownerType))
					markBad(insn, new LoggedAnalyzerException((methodNode, frames) -> {
						// Validate that the top of the stack matches the expected type
						Frame<RValue> frame = frames[InsnUtil.index(insn)];
						RValue fieldContext = frame.getStack(frame.getStackSize() - 1);
						return isSubTypeOf(fieldContext.getType(), ownerType);
					}, insn, "Expected type: " + fin.owner));
				Type type = Type.getType(fin.desc);
				return RValue.ofVirtual(type);
			}
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
					markBad(insn, new AnalyzerException(insn, "Expected an array type."));
				return RValue.ofVirtual(Type.INT_TYPE);
			case ATHROW:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected reference type on stack for ATHROW.");
				return null;
			case CHECKCAST:
				if (!value.isReference())
					throw new AnalyzerException(insn, "Expected reference type on stack for CHECKCAST.");
				return RValue.ofVirtual(Type.getObjectType(((TypeInsnNode) insn).desc));
			case INSTANCEOF:
				return RValue.ofVirtual(Type.INT_TYPE);
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

	private void markBad(AbstractInsnNode insn, AnalyzerException e) {
		badTypeInsns.put(insn, e);
	}

	@Override
	public RValue binaryOperation(AbstractInsnNode insn, RValue value1, RValue value2)  {
		// Modified from BasicVerifier
		Type expected1;
		Type expected2;
		boolean wasAALOAD = false;
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
				wasAALOAD = true;
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
				expected1 = TypeUtil.OBJECT_TYPE;
				expected2 = TypeUtil.OBJECT_TYPE;
				break;
			case PUTFIELD:
				FieldInsnNode fieldInsn = (FieldInsnNode) insn;
				expected1 = Type.getObjectType(fieldInsn.owner);
				expected2 = Type.getType(fieldInsn.desc);
				break;
			default:
				throw new IllegalStateException();
		}
		if (wasAALOAD && !value1.isUninitialized() && value1.isArray() && value1.getType().getDimensions() > 1) {
			// If we are using AALOAD to load an object reference from an array, we check to see if the
			// reference loaded is the another array (consider int[][], fetching int[]) ...
			// In the bytecode, we don't have any immediate way to validate against an expected type.
			// So we shall do nothing :)
		} else if (!value1.isUninitialized() && !value2.isUninitialized())
		{
			if (!isSubTypeOfOrNull(value1, expected1))
				markBad(insn, new AnalyzerException(insn, "First argument not of expected type", expected1, value1));
			else if (!isSubTypeOfOrNull(value2, expected2))
				markBad(insn, new AnalyzerException(insn, "Second argument not of expected type", expected2, value2));
		} else
			markBad(insn, new AnalyzerException(insn, "Cannot act on uninitialized values", expected2, value2));

		// Update values
		switch(insn.getOpcode()) {
			case IADD:
			case FADD:
			case LADD:
			case DADD:
				return value1.add(value2);
			case ISUB:
			case FSUB:
			case LSUB:
			case DSUB:
				return value1.sub(value2);
			case IMUL:
			case FMUL:
			case LMUL:
			case DMUL:
				return value1.mul(value2);
			case IDIV:
			case FDIV:
			case LDIV:
			case DDIV:
				return value1.div(value2);
			case IREM:
			case FREM:
			case LREM:
			case DREM:
				return value1.rem(value2);
			case ISHL:
			case LSHL:
				return value1.shl(value2);
			case ISHR:
			case LSHR:
				return value1.shr(value2);
			case IUSHR:
			case LUSHR:
				return value1.ushr(value2);
			case IAND:
			case LAND:
				return value1.and(value2);
			case IOR:
			case LOR:
				return value1.or(value2);
			case IXOR:
			case LXOR:
				return value1.xor(value2);
			case FALOAD:
				return RValue.ofVirtual(Type.FLOAT_TYPE);
			case LALOAD:
				return RValue.ofVirtual(Type.LONG_TYPE);
			case DALOAD:
				return RValue.ofVirtual(Type.DOUBLE_TYPE);
			case AALOAD:
				if (value1.getType() == null)
					return RValue.ofVirtual(TypeUtil.OBJECT_TYPE);
				else
					return RValue.ofVirtual(Type.getType(value1.getType().getDescriptor().substring(1)));
			case IALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				return RValue.ofVirtual(Type.INT_TYPE);
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				if (value1.getValue() == null || value2.getValue() == null ||
						isValueUnknown(value1) || isValueUnknown(value2))
					return RValue.ofVirtual(Type.INT_TYPE);
				double v1 = ((Number) value1.getValue()).doubleValue();
				double v2 = ((Number) value1.getValue()).doubleValue();
				if(v1 > v2)
					return RValue.ofInt(1);
				else if(v1 < v2)
					return RValue.ofInt(-1);
				else
					return RValue.ofInt(0);
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
				expected3 = TypeUtil.OBJECT_TYPE;
				break;
			default:
				throw new AssertionError();
		}
		if(!isSubTypeOf(value1.getType(), expected1))
			markBad(insn, new AnalyzerException(insn, "First argument not of expected type", expected1, value1));
		else if(!Type.INT_TYPE.equals(value2.getType()))
			markBad(insn, new AnalyzerException(insn, "Second argument not an integer", BasicValue.INT_VALUE, value2));
		else if(!isSubTypeOf(value3.getType(), expected3))
			markBad(insn, new AnalyzerException(insn, "Second argument not of expected type", expected3, value3));
		return null;
	}

	@Override
	public RValue naryOperation(AbstractInsnNode insn, List<? extends RValue> values) throws AnalyzerException {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			// Multi-dimensional array args must all be numeric
			for (RValue value : values)
				if (!Type.INT_TYPE.equals(value.getType()))
					throw new AnalyzerException(insn, "MULTIANEWARRAY argument was not numeric!",
							RValue.ofVirtual(Type.INT_TYPE), value);
			return RValue.ofVirtual(Type.getType(((MultiANewArrayInsnNode) insn).desc));
		} else {
			String methodDescriptor = (opcode == INVOKEDYNAMIC) ?
					((InvokeDynamicInsnNode) insn).desc :
					((MethodInsnNode) insn).desc;
			Type[] args = Type.getArgumentTypes(methodDescriptor);
			// From BasicVerifier
			int i = 0;
			int j = 0;
			if(opcode != INVOKESTATIC && opcode != INVOKEDYNAMIC) {
				MethodInsnNode min = ((MethodInsnNode) insn);
				Type owner = Type.getObjectType(min.owner);
				RValue actual = values.get(i++);
				if(!isSubTypeOf(actual.getType(), owner) && !(isMethodAddSuppressed(min) && actual.isNullConst()))
					markBad(insn, new LoggedAnalyzerException((methodNode, frames) -> {
						// Validate that the owner value is no longer null when stack-frames are filled out
						Frame<RValue> frame = frames[InsnUtil.index(insn)];
						// TODO: Validate the stack index is correct here
						RValue methodContext = frame.getStack(frame.getStackSize() - (args.length + 1));
						return isSubTypeOf(methodContext.getType(), owner);
					}, insn, "Method owner does not match type on stack",
							RValue.ofVirtual(owner), values.get(0)));
			}
			while(i < values.size()) {
				Type expected = args[j++];
				RValue actual = values.get(i++);
				if(!isSubTypeOfOrNull(actual, expected)) {
					int argIndex = i;
					markBad(insn, new LoggedAnalyzerException((methodNode, frames) -> {
						// Validate that the argument value is no longer null when stack-frames are filled out
						Frame<RValue> frame = frames[InsnUtil.index(insn)];
						RValue methodContext = frame.getStack(frame.getStackSize() - (args.length - argIndex + 1));
						return isSubTypeOfOrNull(methodContext, expected);
					},insn, "Argument type was \"" + actual + "\" but expected \"" + expected + "\""));
				}
			}
			// Get value
			if (opcode == INVOKEDYNAMIC) {
				Type retType = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
				return RValue.ofVirtual(retType);
			} else if (opcode == INVOKESTATIC) {
				Type retType = Type.getReturnType(((MethodInsnNode) insn).desc);
				return RValue.ofVirtual(retType);
			} else {
				// INVOKEVIRTUAL, INVOKESPECIAL, INVOKEINTERFACE
				RValue ownerValue = values.get(0);
				if(ownerValue.isUninitialized())
					throw new AnalyzerException(insn, "Cannot call method on uninitialized reference");
				else if(ownerValue.isNullConst() && !isMethodAddSuppressed((MethodInsnNode) insn))
					markBad(insn, new LoggedAnalyzerException((method, frames) -> {
						// Validate that the owner value is no longer null when stack-frames are filled out
						Frame<RValue> frame = frames[InsnUtil.index(insn)];
						RValue methodContext = frame.getStack(frame.getStackSize() - (args.length + 1));
						return !methodContext.isNull();
					}, insn, "Cannot call method on null reference"));
				return ownerValue.ref(Type.getMethodType(((MethodInsnNode)insn).desc));
			}
		}
	}

	@Override
	public void returnOperation(AbstractInsnNode insn, RValue value, RValue expected) throws AnalyzerException {
		if(!isSubTypeOfOrNull(value, expected))
			markBad(insn, new LoggedAnalyzerException((methodNode, frames) -> {
				// Validate that the top of the stack matches the expected type
				Frame<RValue> frame = frames[InsnUtil.index(insn)];
				RValue returnValue = frame.getStack(frame.getStackSize() - 1);
				return isSubTypeOfOrNull(returnValue, expected);
			}, insn, "Incompatible return type, found '" + value.getType() + "', expected: " +
					expected, expected, value));
	}

	@Override
	public RValue merge(RValue value1, RValue value2) {
		// Handle uninitialized
		if (value1 == RValue.UNINITIALIZED)
			return value2;
		else if (value2 == RValue.UNINITIALIZED)
			return value1;
		// Handle equality
		if (value1.equals(value2))
			return value1;
		// Handle null
		//  - NULL can be ANY type, so... it wins the "common super type" here
		if (value2.isNullConst())
			return value1.isNull() ? RValue.ofDefault(value1.getType()) : RValue.ofVirtual(value1.getType());
		else if (value1.isNullConst())
			return value2.isNull() ? RValue.ofDefault(value2.getType()) : RValue.ofVirtual(value2.getType());
		// Check standard merge
		if (value1.canMerge(value2))
			return RValue.ofVirtual(value1.getType());
		else if (value2.canMerge(value1))
			return RValue.ofVirtual(value2.getType());
		return RValue.UNINITIALIZED;
	}

	private static boolean isSubTypeOfOrNull(RValue value, RValue expected) {
		return isSubTypeOfOrNull(value, expected.getType());
	}

	private static boolean isSubTypeOfOrNull(RValue value, Type expected) {
		// TODO: This should not occur
		if (value == null)
			return false;
		// Null type and primitives do not mix.
		// Null types and object types do.
		if (value.isNullConst() && !isPrimitive(expected))
			return true;
		// Uninitialized values are not subtypes
		if (value.isUninitialized())
			return false;
		// Fallback
		return isSubTypeOf(value.getType(), expected);
	}

	private static boolean isSubTypeOf(Type child, Type parent) {
		// Can't handle null type
		if (child == null)
			return false;
		// Simple equality check
		if (child.equals(parent))
			return true;
		// Look at array element type
		boolean bothArrays = child.getSort() == Type.ARRAY && parent.getSort() == Type.ARRAY;
		if (bothArrays) {
			// TODO: With usage cases of "isSubTypeOf(...)" should we just check the element types are equals?
			//  - Or should sub-typing with array element types be used like it currently is?
			child = child.getElementType();
			parent = parent.getElementType();
			// Dimensions must match, unless both are Object
			if (child.getDimensions() != parent.getDimensions() &&
					!(child.equals(TypeUtil.OBJECT_TYPE) && parent.equals(TypeUtil.OBJECT_TYPE)))
				return false;
		}
		// Null check in case
		if (parent == null)
			return false;
		// Treat lesser primitives as integers.
		//  - Because of boolean consts are ICONST_0/ICONST_1
		//  - Short parameters take the stack value of BIPUSH (int)
		if (parent.getSort() >= Type.BOOLEAN && parent.getSort() <= Type.INT)
			parent = Type.INT_TYPE;
		// Check for primitives
		//  - ASM sorts are in a specific order
		//  - If the expected sort is a larger type (greater sort) then the given type can
		//    be assumed to be compatible.
		if (isPrimitive(parent) && isPrimitive(child))
			return parent.getSort() >= child.getSort();
		// Use a simplified check if the expected type is just "Object"
		//  - Most things can be lumped into an object
		if (!isPrimitive(child) && parent.getDescriptor().equals("Ljava/lang/Object;"))
			return true;
		// Check if types are compatible
		if (child.getSort() == parent.getSort()) {
			RValue host = RValue.ofDefault(parent);
			return host != null && host.canMerge(RValue.ofDefault(child));
		}
		return false;
	}

	private boolean isValueUnknown(RValue value) {
		return value.getValue() == null || value.getValue() instanceof RVirtual;
	}

	private float toFloat(RValue value) {
		return ((Number) value.getValue()).floatValue();
	}

	private double toDouble(RValue value) {
		return ((Number) value.getValue()).doubleValue();
	}

	private int toInt(RValue value) {
		return ((Number) value.getValue()).intValue();
	}

	private long toLong(RValue value) {
		return ((Number) value.getValue()).longValue();
	}

	private static boolean isPrimitive(Type type) {
		return type.getSort() < Type.ARRAY;
	}

	private static boolean isMethodAddSuppressed(MethodInsnNode insn) {
		// Seriously, wtf is this?
		// Compile the code below:
		//
		//// try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {}
		//// finally {  is.close();  }
		//
		// This will literally generate a call that behaves like "null.addSuppressed(Throwable)"
		// - It generates a method call on a variable that is ALWAYS null
		//
		// And that is why we have this check...
		return insn.owner.equals("java/lang/Throwable") &&
				insn.name.equals("addSuppressed") &&
				insn.desc.equals("(Ljava/lang/Throwable;)V");
	}
}
