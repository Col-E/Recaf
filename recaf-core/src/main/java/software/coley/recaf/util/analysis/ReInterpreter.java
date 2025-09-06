package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.slf4j.Logger;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.lookup.GetFieldLookup;
import software.coley.recaf.util.analysis.lookup.GetStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeStaticLookup;
import software.coley.recaf.util.analysis.lookup.InvokeVirtualLookup;
import software.coley.recaf.util.analysis.value.ArrayValue;
import software.coley.recaf.util.analysis.value.DoubleValue;
import software.coley.recaf.util.analysis.value.FloatValue;
import software.coley.recaf.util.analysis.value.IllegalValueException;
import software.coley.recaf.util.analysis.value.IntValue;
import software.coley.recaf.util.analysis.value.LongValue;
import software.coley.recaf.util.analysis.value.ObjectValue;
import software.coley.recaf.util.analysis.value.ReValue;
import software.coley.recaf.util.analysis.value.UninitializedValue;

import java.util.List;
import java.util.OptionalInt;

/**
 * Interpreter implementation for enhanced value tracking.
 *
 * @author Matt Coley
 * @see ReValue Base enhanced value type.
 */
public class ReInterpreter extends Interpreter<ReValue> implements Opcodes {
	private static final Logger logger = Logging.get(ReInterpreter.class);
	private final InheritanceGraph inheritanceGraph;
	private GetStaticLookup getStaticLookup;
	private GetFieldLookup getFieldLookup;
	private InvokeStaticLookup invokeStaticLookup;
	private InvokeVirtualLookup invokeVirtualLookup;

	public ReInterpreter(@Nonnull InheritanceGraph inheritanceGraph) {
		super(RecafConstants.getAsmVersion());
		this.inheritanceGraph = inheritanceGraph;
	}

	@Nullable
	public GetStaticLookup getGetStaticLookup() {
		return getStaticLookup;
	}

	@Nullable
	public GetFieldLookup getGetFieldLookup() {
		return getFieldLookup;
	}

	@Nullable
	public InvokeStaticLookup getInvokeStaticLookup() {
		return invokeStaticLookup;
	}

	@Nullable
	public InvokeVirtualLookup getInvokeVirtualLookup() {
		return invokeVirtualLookup;
	}

	public void setGetStaticLookup(@Nullable GetStaticLookup getStaticLookup) {
		this.getStaticLookup = getStaticLookup;
	}

	public void setGetFieldLookup(@Nullable GetFieldLookup getFieldLookup) {
		this.getFieldLookup = getFieldLookup;
	}

	public void setInvokeStaticLookup(@Nullable InvokeStaticLookup invokeStaticLookup) {
		this.invokeStaticLookup = invokeStaticLookup;
	}

	public void setInvokeVirtualLookup(@Nullable InvokeVirtualLookup invokeVirtualLookup) {
		this.invokeVirtualLookup = invokeVirtualLookup;
	}

	@Nonnull
	@SuppressWarnings("DataFlowIssue") // Won't happen because we use arrays
	private ReValue newArrayValue(@Nonnull Type type, int dimensions) {
		if (dimensions == 0)
			return newValue(type);
		String descriptor = "[".repeat(Math.max(0, dimensions)) + type.getDescriptor();
		return newValue(Type.getType(descriptor), Nullness.NOT_NULL);
	}

	@Nullable
	public ReValue newValue(@Nullable Type type, @Nonnull Nullness nullness) {
		try {
			return ReValue.ofType(type, nullness);
		} catch (IllegalValueException ex) {
			throw new IllegalStateException(ex);
		}
	}

	@Override
	public ReValue newValue(@Nullable Type type) {
		return newValue(type, Nullness.UNKNOWN);
	}

	@Override
	public ReValue newOperation(@Nonnull AbstractInsnNode insn) throws AnalyzerException {
		switch (insn.getOpcode()) {
			case ACONST_NULL:
				return ObjectValue.VAL_OBJECT_NULL;
			case ICONST_M1:
				return IntValue.VAL_M1;
			case ICONST_0:
				return IntValue.VAL_0;
			case ICONST_1:
				return IntValue.VAL_1;
			case ICONST_2:
				return IntValue.VAL_2;
			case ICONST_3:
				return IntValue.VAL_3;
			case ICONST_4:
				return IntValue.VAL_4;
			case ICONST_5:
				return IntValue.VAL_5;
			case LCONST_0:
				return LongValue.VAL_0;
			case LCONST_1:
				return LongValue.VAL_1;
			case FCONST_0:
				return FloatValue.VAL_0;
			case FCONST_1:
				return FloatValue.VAL_1;
			case FCONST_2:
				return FloatValue.VAL_2;
			case DCONST_0:
				return DoubleValue.VAL_0;
			case DCONST_1:
				return DoubleValue.VAL_1;
			case BIPUSH:
			case SIPUSH:
				IntInsnNode intInsn = (IntInsnNode) insn;
				return IntValue.of(intInsn.operand);
			case LDC:
				Object value = ((LdcInsnNode) insn).cst;
				switch (value) {
					case Integer i -> {
						return IntValue.of(i);
					}
					case Float f -> {
						return FloatValue.of(f);
					}
					case Long l -> {
						return LongValue.of(l);
					}
					case Double d -> {
						return DoubleValue.of(d);
					}
					case String s -> {
						return ObjectValue.string(s);
					}
					case Type type -> {
						int sort = type.getSort();
						if (sort == Type.OBJECT || sort == Type.ARRAY) {
							return ObjectValue.VAL_CLASS;
						} else if (sort == Type.METHOD) {
							return ObjectValue.VAL_METHOD_TYPE;
						} else {
							throw new AnalyzerException(insn, "Illegal LDC value " + value);
						}
					}
					case Handle handle -> {
						return ObjectValue.VAL_METHOD_HANDLE;
					}
					case ConstantDynamic constantDynamic -> {
						Type dynamicType = Type.getType(constantDynamic.getDescriptor());
						return newValue(dynamicType, Nullness.NOT_NULL);
					}
					case null, default -> throw new AnalyzerException(insn, "Illegal LDC value " + value);
				}
			case JSR:
				return ObjectValue.VAL_JSR;
			case GETSTATIC:
				FieldInsnNode field = (FieldInsnNode) insn;
				if (getStaticLookup != null)
					return getStaticLookup.get(field);
				Type fieldType = Type.getType(field.desc);
				return newValue(fieldType);
			case NEW:
				Type objectType = Type.getObjectType(((TypeInsnNode) insn).desc);
				return newValue(objectType, Nullness.NOT_NULL);
			default:
				throw new AssertionError();
		}
	}

	@Override
	public ReValue copyOperation(@Nonnull AbstractInsnNode insn, @Nonnull ReValue value) {
		// Just keep the same value reference
		return value;
	}

	@Override
	public ReValue unaryOperation(@Nonnull AbstractInsnNode insn, @Nonnull ReValue value) throws AnalyzerException {
		switch (insn.getOpcode()) {
			case IINC: {
				int incr = ((IincInsnNode) insn).incr;
				if (value instanceof IntValue iv) return iv.add(incr);
				return IntValue.UNKNOWN;
			}
			case INEG:
				if (value instanceof IntValue iv) return iv.negate();
				return IntValue.UNKNOWN;
			case L2I:
				if (value instanceof LongValue lv) return lv.castInt();
				return IntValue.UNKNOWN;
			case F2I:
				if (value instanceof FloatValue fv) return fv.castInt();
				return IntValue.UNKNOWN;
			case D2I:
				if (value instanceof DoubleValue dv) return dv.castInt();
				return IntValue.UNKNOWN;
			case I2B:
				if (value instanceof IntValue iv) return iv.castByte();
				return IntValue.UNKNOWN;
			case I2C:
				if (value instanceof IntValue iv) return iv.castChar();
				return IntValue.UNKNOWN;
			case I2S:
				if (value instanceof IntValue iv) return iv.castShort();
				return IntValue.UNKNOWN;
			case FNEG:
				if (value instanceof FloatValue fv) return fv.negate();
				return FloatValue.UNKNOWN;
			case I2F:
				if (value instanceof IntValue iv) return iv.castFloat();
				return FloatValue.UNKNOWN;
			case L2F:
				if (value instanceof LongValue lv) return lv.castFloat();
				return FloatValue.UNKNOWN;
			case D2F:
				if (value instanceof DoubleValue dv) return dv.castFloat();
				return FloatValue.UNKNOWN;
			case LNEG:
				if (value instanceof LongValue lv) return lv.negate();
				return LongValue.UNKNOWN;
			case I2L:
				if (value instanceof IntValue iv) return iv.castLong();
				return LongValue.UNKNOWN;
			case F2L:
				if (value instanceof FloatValue fv) return fv.castLong();
				return LongValue.UNKNOWN;
			case D2L:
				if (value instanceof DoubleValue dv) return dv.castLong();
				return LongValue.UNKNOWN;
			case DNEG:
				if (value instanceof DoubleValue dv) return dv.negate();
				return DoubleValue.UNKNOWN;
			case I2D:
				if (value instanceof IntValue iv) return iv.castDouble();
				return DoubleValue.UNKNOWN;
			case L2D:
				if (value instanceof LongValue lv) return lv.castDouble();
				return DoubleValue.UNKNOWN;
			case F2D:
				if (value instanceof FloatValue fv) return fv.castDouble();
				return DoubleValue.UNKNOWN;
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
			case MONITORENTER:
			case MONITOREXIT:
			case IFNULL:
			case IFNONNULL:
				return null;
			case ATHROW:
				return null;
			case GETFIELD: {
				FieldInsnNode field = (FieldInsnNode) insn;
				if (getFieldLookup != null && value.hasKnownValue())
					return getFieldLookup.get(field, value);
				Type fieldType = Type.getType(field.desc);
				return newValue(fieldType);
			}
			case NEWARRAY:
				int arrayKind = ((IntInsnNode) insn).operand;
				if (value instanceof IntValue length) {
					OptionalInt lengthValue = length.value();
					if (lengthValue.isPresent()) {
						Type type = switch (arrayKind) {
							case T_BOOLEAN -> Types.ARRAY_1D_BOOLEAN;
							case T_CHAR -> Types.ARRAY_1D_CHAR;
							case T_BYTE -> Types.ARRAY_1D_BYTE;
							case T_SHORT -> Types.ARRAY_1D_SHORT;
							case T_INT -> Types.ARRAY_1D_INT;
							case T_FLOAT -> Types.ARRAY_1D_FLOAT;
							case T_DOUBLE -> Types.ARRAY_1D_DOUBLE;
							case T_LONG -> Types.ARRAY_1D_LONG;
							default -> throw new AnalyzerException(insn, "Invalid array type");
						};
						return ArrayValue.of(type, Nullness.NOT_NULL, lengthValue.getAsInt());
					}
				}
				return switch (arrayKind) {
					case T_BOOLEAN -> ArrayValue.VAL_BOOLEANS;
					case T_CHAR -> ArrayValue.VAL_CHARS;
					case T_BYTE -> ArrayValue.VAL_BYTES;
					case T_SHORT -> ArrayValue.VAL_SHORTS;
					case T_INT -> ArrayValue.VAL_INTS;
					case T_FLOAT -> ArrayValue.VAL_FLOATS;
					case T_DOUBLE -> ArrayValue.VAL_DOUBLES;
					case T_LONG -> ArrayValue.VAL_LONGS;
					default -> throw new AnalyzerException(insn, "Invalid array type");
				};
			case ANEWARRAY: {
				Type arrayType = Type.getType("[" + Type.getObjectType(((TypeInsnNode) insn).desc));
				if (value instanceof IntValue iv && iv.hasKnownValue())
					return ArrayValue.of(arrayType, Nullness.NOT_NULL, iv.value().getAsInt());
				return newValue(arrayType, Nullness.NOT_NULL);
			}
			case ARRAYLENGTH:
				if (value instanceof ArrayValue array) {
					OptionalInt firstDimensionLength = array.getFirstDimensionLength();
					if (firstDimensionLength.isPresent())
						return IntValue.of(firstDimensionLength.getAsInt());
				}
				return IntValue.UNKNOWN;
			case CHECKCAST:
				Type targetType = Type.getObjectType(((TypeInsnNode) insn).desc);
				return newValue(targetType);
			case INSTANCEOF:
				return IntValue.UNKNOWN;
			default:
				throw new AnalyzerException(insn, "Unknown unary op: " + insn.getOpcode());
		}
	}

	@Override
	public ReValue binaryOperation(@Nonnull AbstractInsnNode insn, @Nonnull ReValue value1, @Nonnull ReValue value2) {
		switch (insn.getOpcode()) {
			case IALOAD:
			case BALOAD:
			case CALOAD:
			case SALOAD:
				if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
					return array.getValue(index.value().getAsInt());
				return IntValue.UNKNOWN;
			case FALOAD:
				if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
					return array.getValue(index.value().getAsInt());
				return FloatValue.UNKNOWN;
			case LALOAD:
				if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
					return array.getValue(index.value().getAsInt());
				return LongValue.UNKNOWN;
			case DALOAD:
				if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
					return array.getValue(index.value().getAsInt());
				return DoubleValue.UNKNOWN;
			case AALOAD:
				if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
					return array.getValue(index.value().getAsInt());
				return ObjectValue.VAL_OBJECT_MAYBE_NULL;
			case IADD:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.add(i2);
				return IntValue.UNKNOWN;
			case ISUB:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.sub(i2);
				return IntValue.UNKNOWN;
			case IMUL:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.mul(i2);
				return IntValue.UNKNOWN;
			case IDIV:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.div(i2);
				return IntValue.UNKNOWN;
			case IREM:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.rem(i2);
				return IntValue.UNKNOWN;
			case ISHL:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.shl(i2);
				return IntValue.UNKNOWN;
			case ISHR:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.shr(i2);
				return IntValue.UNKNOWN;
			case IUSHR:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.ushr(i2);
				return IntValue.UNKNOWN;
			case IAND:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.and(i2);
				return IntValue.UNKNOWN;
			case IOR:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.or(i2);
				return IntValue.UNKNOWN;
			case IXOR:
				if (value1 instanceof IntValue i1 && value2 instanceof IntValue i2) return i1.xor(i2);
				return IntValue.UNKNOWN;
			case FADD:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.add(f2);
				return FloatValue.UNKNOWN;
			case FSUB:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.sub(f2);
				return FloatValue.UNKNOWN;
			case FMUL:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.mul(f2);
				return FloatValue.UNKNOWN;
			case FDIV:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.div(f2);
				return FloatValue.UNKNOWN;
			case FREM:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.rem(f2);
				return FloatValue.UNKNOWN;
			case LADD:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.add(l2);
				return LongValue.UNKNOWN;
			case LSUB:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.sub(l2);
				return LongValue.UNKNOWN;
			case LMUL:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.mul(l2);
				return LongValue.UNKNOWN;
			case LDIV:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.div(l2);
				return LongValue.UNKNOWN;
			case LREM:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.rem(l2);
				return LongValue.UNKNOWN;
			case LSHL:
				if (value1 instanceof LongValue l1 && value2 instanceof IntValue l2) return l1.shl(l2);
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.shl(l2);
				return LongValue.UNKNOWN;
			case LSHR:
				if (value1 instanceof LongValue l1 && value2 instanceof IntValue l2) return l1.shr(l2);
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.shr(l2);
				return LongValue.UNKNOWN;
			case LUSHR:
				if (value1 instanceof LongValue l1 && value2 instanceof IntValue l2) return l1.ushr(l2);
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.ushr(l2);
				return LongValue.UNKNOWN;
			case LAND:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.and(l2);
				return LongValue.UNKNOWN;
			case LOR:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.or(l2);
				return LongValue.UNKNOWN;
			case LXOR:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.xor(l2);
				return LongValue.UNKNOWN;
			case DADD:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.add(d2);
				return DoubleValue.UNKNOWN;
			case DSUB:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.sub(d2);
				return DoubleValue.UNKNOWN;
			case DMUL:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.mul(d2);
				return DoubleValue.UNKNOWN;
			case DDIV:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.div(d2);
				return DoubleValue.UNKNOWN;
			case DREM:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.rem(d2);
				return DoubleValue.UNKNOWN;
			case LCMP:
				if (value1 instanceof LongValue l1 && value2 instanceof LongValue l2) return l1.cmp(l2);
				return IntValue.UNKNOWN;
			case FCMPL:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.cmpl(f2);
				return IntValue.UNKNOWN;
			case FCMPG:
				if (value1 instanceof FloatValue f1 && value2 instanceof FloatValue f2) return f1.cmpg(f2);
				return IntValue.UNKNOWN;
			case DCMPL:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.cmpl(d2);
				return IntValue.UNKNOWN;
			case DCMPG:
				if (value1 instanceof DoubleValue d1 && value2 instanceof DoubleValue d2) return d1.cmpg(d2);
				return IntValue.UNKNOWN;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
			case PUTFIELD:
				// Just popping values, not pushing anything back onto the stack
				return null;
			default:
				throw new AssertionError();
		}
	}

	@Override
	public ReValue ternaryOperation(@Nonnull AbstractInsnNode insn, @Nonnull ReValue value1, @Nonnull ReValue value2, @Nonnull ReValue value3) {
		// This method covers the following instructions:
		//  IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE
		// It will always be an array-store operation.
		// NOTE: Load operations are handled in 'binaryOperation'
		if (value1 instanceof ArrayValue array && value2 instanceof IntValue index && index.value().isPresent())
			return array.setValue(index.value().getAsInt(), value3);
		return value1;
	}

	@Override
	public ReValue naryOperation(@Nonnull AbstractInsnNode insn, @Nonnull List<? extends ReValue> values) {
		int opcode = insn.getOpcode();
		if (opcode == MULTIANEWARRAY) {
			Type type = Type.getType(((MultiANewArrayInsnNode) insn).desc);

			// Extract dimensions from passed values.
			// Unknown values will be negative, which we will sanity check for.
			int[] dimensions = values.stream()
					.mapToInt(v -> v instanceof IntValue intValue && intValue.value().isPresent() ?
							intValue.value().getAsInt() : -1)
					.toArray();
			if (dimensions.length == 0) // Sanity check, should never occur.
				return newValue(type, Nullness.NOT_NULL);
			for (int dimension : dimensions)
				if (dimension < 0) // Validate dimensions are valid
					return newValue(type, Nullness.NOT_NULL);

			// Make array of known dimensions.
			return ArrayValue.multiANewArray(type, dimensions);
		} else if (opcode == INVOKEDYNAMIC) {
			Type returnType = Type.getReturnType(((InvokeDynamicInsnNode) insn).desc);
			return newValue(returnType);
		} else {
			MethodInsnNode method = (MethodInsnNode) insn;
			Type returnType = Type.getReturnType(method.desc);
			if (returnType.getSort() != Type.VOID) {
				// We only support "lookups" as values are immutable.
				// Something like System.arraycopy(...) isn't supported here.
				if (opcode == INVOKESTATIC && invokeStaticLookup != null && values.stream().allMatch(ReValue::hasKnownValue)) {
					return invokeStaticLookup.get(method, values);
				} else if ((opcode == INVOKEVIRTUAL || opcode == INVOKEINTERFACE || opcode == INVOKESPECIAL)
						&& invokeVirtualLookup != null && values.stream().allMatch(ReValue::hasKnownValue)) {
					return invokeVirtualLookup.get(method, values.getFirst(), values.subList(1, values.size()));
				}
			}
			return newValue(returnType);
		}
	}

	@Override
	public void returnOperation(@Nonnull AbstractInsnNode insn, @Nonnull ReValue value, @Nonnull ReValue expected) {
		// no-op
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Implementation adapted from {@link SimpleVerifier}.
	 *
	 * @param value1
	 * 		A value.
	 * @param value2
	 * 		Another value.
	 *
	 * @return The merged value.
	 */
	@Override
	public ReValue merge(@Nonnull ReValue value1, @Nonnull ReValue value2) {
		Type type1 = value1.type();
		Type type2 = value2.type();

		// Null types correspond to UNINITIALIZED_VALUE.
		if (type1 == null || type2 == null)
			return UninitializedValue.UNINITIALIZED_VALUE;

		// If the types are the same, we should merge their tracked value state.
		if (type1.equals(type2)) {
			try {
				return value1.mergeWith(value2);
			} catch (IllegalValueException t) {
				logger.error("Failed ReValue merge of of same type", t);
				throw new IllegalStateException("Failed ReValue merge of of same type", t);
			}
		}

		// The merge of a primitive type with a different type is the type of uninitialized values.
		if (type1.getSort() != Type.OBJECT && type1.getSort() != Type.ARRAY)
			return UninitializedValue.UNINITIALIZED_VALUE;
		if (type2.getSort() != Type.OBJECT && type2.getSort() != Type.ARRAY)
			return UninitializedValue.UNINITIALIZED_VALUE;

		// Special case for the type of the "null" literal.
		if (value1 instanceof ObjectValue ov1 && ov1.isNull())
			return value2;
		if (value2 instanceof ObjectValue ov2 && ov2.isNull())
			return value1;

		// Convert type1 to its element type and array dimension. Arrays of primitive values are seen as
		// Object arrays with one dimension less. Hence, the element type is always of Type.OBJECT sort.
		int dim1 = 0;
		if (type1.getSort() == Type.ARRAY) {
			dim1 = type1.getDimensions();
			type1 = type1.getElementType();
			if (type1.getSort() != Type.OBJECT) {
				dim1 = dim1 - 1;
				type1 = Types.OBJECT_TYPE;
			}
		}

		// Do the same for type2.
		int dim2 = 0;
		if (type2.getSort() == Type.ARRAY) {
			dim2 = type2.getDimensions();
			type2 = type2.getElementType();
			if (type2.getSort() != Type.OBJECT) {
				dim2 = dim2 - 1;
				type2 = Types.OBJECT_TYPE;
			}
		}

		// The merge of array types of different dimensions is an Object array type.
		if (dim1 != dim2)
			return newArrayValue(Types.OBJECT_TYPE, Math.min(dim1, dim2));

		// If the dimensions are the same, and there is no array aspect of these values
		// then we want to merge the values in such a way that tracks state if possible.
		if (dim1 == 0) {
			try {
				if (isAssignableFrom(type1, type2))
					return value1.mergeWith(value2);
				if (isAssignableFrom(type2, type1))
					return value2.mergeWith(value1);
			} catch (Throwable t) {
				logger.error("Failed ReValue merge of {} and {}",
						value1.getClass().getSimpleName(), value2.getClass().getSimpleName(), t);
				throw new IllegalStateException("Failed ReValue merge of of same type", t);
			}
		}

		// Type1 and type2 have a Type.OBJECT sort by construction (see above),
		// as expected by isAssignableFrom.
		if (isAssignableFrom(type1, type2))
			return newArrayValue(type1, dim1);
		if (isAssignableFrom(type2, type1))
			return newArrayValue(type2, dim1);

		if (!isInterface(type1)) {
			while (!Types.OBJECT_TYPE.equals(type1)) {
				type1 = getSuperClass(type1);
				if (isAssignableFrom(type1, type2))
					return newArrayValue(type1, dim1);
			}
		}

		return newArrayValue(Types.OBJECT_TYPE, dim1);
	}

	@Nonnull
	private Type getSuperClass(@Nonnull Type type) {
		String name = type.getInternalName();
		InheritanceVertex vertex = inheritanceGraph.getVertex(name);
		if (vertex == null)
			return Types.OBJECT_TYPE;
		String superName = vertex.getValue().getSuperName();
		return superName == null ? Types.OBJECT_TYPE : Type.getObjectType(superName);
	}

	private boolean isInterface(@Nonnull Type type) {
		String name = type.getInternalName();
		InheritanceVertex vertex = inheritanceGraph.getVertex(name);
		if (vertex == null)
			return false;
		return vertex.getValue().hasInterfaceModifier();
	}

	private boolean isAssignableFrom(@Nonnull Type type1, @Nonnull Type type2) {
		String name1 = type1.getInternalName();
		String name2 = type2.getInternalName();
		return inheritanceGraph.isAssignableFrom(name1, name2);
	}
}
