package me.coley.recaf.parse.bytecode;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

/**
 * Value recording the type and value<i>(primitives only)</i>.
 *
 * @author Matt
 */
public class RValue implements Value {
	public static final RValue UNINITIALIZED = new RValue(null, null);
	public static final RValue RETURNADDRESS_VALUE = new RValue(Type.VOID_TYPE, null);
	public static final RValue NULL = new RValue(Type.getObjectType("java/lang/Object"), null);
	private static final List<Integer> sortOrdering = new ArrayList<>();
	private final Type type;
	private final Object value;

	private RValue(Type type, final Object value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * @param value Int.
	 * @return int value.
	 */
	public static RValue of(int value) {
		return new RValue(Type.INT_TYPE, value);
	}

	/**
	 * @param value Character.
	 * @return char value.
	 */
	public static RValue of(char value) {
		return new RValue(Type.INT_TYPE, value);
	}

	/**
	 * @param value Byte.
	 * @return byte value.
	 */
	public static RValue of(byte value) {
		return new RValue(Type.INT_TYPE, value);
	}

	/**
	 * @param value Short.
	 * @return short value.
	 */
	public static RValue of(short value) {
		return new RValue(Type.INT_TYPE, value);
	}

	/**
	 * @param value Boolean.
	 * @return boolean value.
	 */
	public static RValue of(boolean value) {
		return new RValue(Type.INT_TYPE, value ? 1 : 0);
	}

	/**
	 * @param value Long.
	 * @return long value.
	 */
	public static RValue of(long value) {
		return new RValue(Type.LONG_TYPE, value);
	}

	/**
	 * @param value Float.
	 * @return float value.
	 */
	public static RValue of(float value) {
		return new RValue(Type.FLOAT_TYPE, value);
	}

	/**
	 * @param value Double.
	 * @return double value.
	 */
	public static RValue of(double value) {
		return new RValue(Type.DOUBLE_TYPE, value);
	}

	/**
	 * @param value String.
	 * @return String value.
	 */
	public static RValue of(String value) {
		return new RValue(Type.getObjectType("java/lang/String"), value);
	}

	/**
	 * @param type Type.
	 * @return Type value.
	 */
	public static RValue of(Type type) {
		if (type == null)
			return UNINITIALIZED;
		switch(type.getSort()) {
			case Type.VOID:
				return null;
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
				return new RValue(Type.INT_TYPE, null);
			case Type.FLOAT:
				return new RValue(Type.FLOAT_TYPE, null);
			case Type.LONG:
				return new RValue(Type.LONG_TYPE, null);
			case Type.DOUBLE:
				return new RValue(Type.DOUBLE_TYPE, null);
			case Type.ARRAY:
			case Type.OBJECT:
				return new RValue(type, null);
			default:
				throw new IllegalStateException("Unsupported type: " + type);
		}
	}

	/**
	 * @param type Type to virtualize.
	 * @return Virtual value of type.
	 */
	public static RValue ofVirtual(Type type) {
		return new RValue(type, new RVirtual(type));
	}

	/**
	 * @param type Type <i>(always {@code java/lang/Class})</i>
	 * @param value The value / type of class.
	 * @return Class value.
	 */
	public static RValue ofClass(Type type, Type value) {
		return new RValue(type, value);
	}

	// =================================== //

	/**
	 * @param other Another value.
	 * @return Adds this value to another.
	 */
	public RValue add(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, addN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Subtract this value by another.
	 */
	public RValue sub(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, subN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Multiply this value by another.
	 */
	public RValue mul(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, mulN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Divide this value by another.
	 */
	public RValue div(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, divN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Get remainder of this value by another.
	 */
	public RValue rem(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, remN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Shift this value by another.
	 */
	public RValue shl(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, shlN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Shift this value by another.
	 */
	public RValue shr(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, shrN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Shift this value by another.
	 */
	public RValue ushr(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, ushrN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Bitwise and this and another value.
	 */
	public RValue and(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, andN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Bitwise or this and another value.
	 */
	public RValue or(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, orN((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Bitwise or this and another value.
	 */
	public RValue xor(RValue other) {
		Type common = commonMathType(type, other.type);
		if (!(common.equals(Type.INT_TYPE) || common.equals(Type.LONG_TYPE)))
			throw new IllegalStateException("Requires int/long types");
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, xorN((Number) value, (Number) other.value));
	}

	/**
	 * @param type Type to act on.
	 * @return Act on a reference
	 */
	public RValue ref(Type type) {
		// Act on return type if passed type is method
		if (type != null && type.getSort() == Type.METHOD) {
			Type retType = type.getReturnType();
			if (retType.getSort() >= Type.ARRAY)
				return ofVirtual(retType);
			return of(retType);
		}
		// Don't act on 'null' values
		if (value == null || value.equals(Type.VOID_TYPE))
			throw new IllegalStateException("Cannot act on null reference value");
		// Don't try to do object stuff with non-objects
		if (!(value instanceof RVirtual))
			throw new IllegalStateException("Cannot act on reference on non-reference value");
		// Nullify voids
		if (type != null && type.equals(Type.VOID_TYPE))
			return null;
		// Take on the type of the other
		if (type.getSort() >= Type.ARRAY)
			return ofVirtual(type);
		return of(type);
	}

	// =========================================================== //

	/**
	 * @return {@code true} if the wrapped type is a reference.
	 */
	public boolean isReference() {
		return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
	}

	/**
	 * @return {@code true} if the {@link #getValue() value} is {@code null}.
	 */
	public boolean isNull() {
		return value == null;
	}

	/**
	 * @return {@code true} if this RValue is {{@link #NULL}}.
	 */
	public boolean isNullConst() {
		return this == NULL;
	}

	/**
	 * @return {@code true} if this RValue is {{@link #UNINITIALIZED}}.
	 */
	public boolean isUninitialized() {
		return this == UNINITIALIZED;
	}

	/**
	 * @return {@code true} if this RValue is {{@link #RETURNADDRESS_VALUE}}.
	 */
	public boolean isJsrRet() {
		return this == RETURNADDRESS_VALUE;
	}

	/**
	 * @return Value.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * @return Type.
	 */
	public Type getType() {
		return type;
	}

	@Override
	public int getSize() {
		if (type == null)
			return 1;
		return TypeUtil.sortToSize(type.getSort());
	}

	@Override
	public int hashCode() {
		if (type == null)
			return 0;
		if (value == null)
			return type.hashCode();
		return Objects.hash(type.getDescriptor(), value);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		else if (other == UNINITIALIZED)
			return false;
		else if (other == NULL)
			return false;
		else if(other instanceof RValue) {
			RValue ov = (RValue) other;
			if(type == null)
				// RET: Are both types null?
				return ov.type == null;
			else if(value == null)
				// RET: Do they share a parent? And is the other value not null?
				return isParent(type, ov.type) && ov.value == null;
			else
				// RET: Do they share a parent? Are the values equal?
				return isParent(type, ov.type) && value.equals(ov.value);
		}
		return false;
	}

	@Override
	public String toString() {
		if (isUninitialized())
			return "<UNINITIALIZED>";
		else if (isNull())
			return "<NULL>";
		else if (isJsrRet())
			return "<JSR_RET>";
		else
			return type + " - " + value;
	}

	/**
	 * @param other
	 * 		Another frame.
	 *
	 * @return {@code true} if merge is optional.
	 */
	public boolean canMerge(RValue other) {
		if(other == this)
			return true;
		else if(other == NULL || other == UNINITIALIZED || other == null)
			return false;
		else if(type == null)
			return other.type == null;
		else
			return type.equals(other.type) || isParent(type, other.type) || other.isPromotionOf(this);
	}


	private boolean isPromotionOf(RValue other) {
		int i1 = getPromotionIndex(type.getSort());
		int i2 = getPromotionIndex(other.getType().getSort());
		return i1 >= i2;
	}

	// =========================================================== //

	/**
	 * @param sort
	 * 		Method sort.
	 *
	 * @return Promotion order.
	 */
	public static int getPromotionIndex(int sort) {
		return sortOrdering.indexOf(sort);
	}

	private static Type commonMathType(Type a, Type b) {
		if (a == null || b == null)
			throw new IllegalStateException("Cannot find common type of a null type");
		int i1 = getPromotionIndex(a.getSort());
		int i2 = getPromotionIndex(b.getSort());
		int max = Math.max(i1, i2);
		if(max <= Type.DOUBLE)
			return max == i1 ? a : b;
		throw new IllegalStateException("Cannot do math on non-primitive types: " +
				a.getDescriptor() + " & " + b.getDescriptor());
	}

	private static boolean isParent(Type parent, Type child) {
		if(parent == null || child == null)
			throw new IllegalStateException("Cannot find common type of parent null type");
		else if(parent.getSort() == Type.OBJECT && child.getSort() == Type.OBJECT) {
			if(parent.equals(child))
				return true;
			if (Recaf.getCurrentWorkspace() == null)
				return false;
			return Recaf.getCurrentWorkspace().getHierarchyGraph()
					.getAllParents(child.getInternalName())
					.anyMatch(n -> n != null && n.equals(parent.getInternalName()));
		} else
			return parent.getSort() < Type.ARRAY && child.getSort() < Type.ARRAY;
	}

	private static Type common(Type a, Type b) {
		if(a == null || b == null)
			throw new IllegalStateException("Cannot find common type of a null type");
		if(a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT)
			return Type.getObjectType(Recaf.getCurrentWorkspace().getHierarchyGraph()
					.getCommon(a.getInternalName(), b.getInternalName()));
		else if(a.getSort() < Type.ARRAY && b.getSort() < Type.ARRAY)
			return commonMathType(a, b);
		return null;
	}

	private static Number addN(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() + b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() + b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() + b.longValue();
		else
			return a.intValue() + b.intValue();
	}

	private static Number subN(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() - b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() - b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() - b.longValue();
		else
			return a.intValue() - b.intValue();
	}

	private static Number mulN(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() * b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() * b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() * b.longValue();
		else
			return a.intValue() * b.intValue();
	}

	private static Number divN(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() / b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() / b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() / b.longValue();
		else
			return a.intValue() / b.intValue();
	}

	private static Number remN(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() % b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() % b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() % b.longValue();
		else
			return a.intValue() % b.intValue();
	}

	private static Number shlN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() << b.longValue();
		else
			return a.intValue() << b.intValue();
	}

	private static Number shrN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() >> b.longValue();
		else
			return a.intValue() >> b.intValue();
	}

	private static Number ushrN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() >>> b.longValue();
		else
			return a.intValue() >>> b.intValue();
	}

	private static Number andN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() & b.longValue();
		else
			return a.intValue() & b.intValue();
	}

	private static Number orN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() | b.longValue();
		else
			return a.intValue() | b.intValue();
	}

	private static Number xorN(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() ^ b.longValue();
		else
			return a.intValue() ^ b.intValue();
	}

	static {
		// 0
		sortOrdering.add(Type.VOID);
		// 1
		sortOrdering.add(Type.BOOLEAN);
		// 8
		sortOrdering.add(Type.BYTE);
		// 16
		sortOrdering.add(Type.SHORT);
		sortOrdering.add(Type.CHAR);
		// 32
		sortOrdering.add(Type.INT);
		sortOrdering.add(Type.FLOAT);
		// 64
		sortOrdering.add(Type.DOUBLE);
		sortOrdering.add(Type.LONG);
		// ?
		sortOrdering.add(Type.ARRAY);
		sortOrdering.add(Type.OBJECT);
	}
}
