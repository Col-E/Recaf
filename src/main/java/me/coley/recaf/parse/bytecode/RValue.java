package me.coley.recaf.parse.bytecode;

import me.coley.recaf.Recaf;
import me.coley.recaf.util.TypeUtil;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.Value;

import java.util.*;

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

	public static RValue of(int value) {
		return new RValue(Type.INT_TYPE, value);
	}

	public static RValue of(char value) {
		return new RValue(Type.INT_TYPE, value);
	}

	public static RValue of(byte value) {
		return new RValue(Type.INT_TYPE, value);
	}

	public static RValue of(short value) {
		return new RValue(Type.INT_TYPE, value);
	}

	public static RValue of(boolean value) {
		return new RValue(Type.INT_TYPE, value ? 1 : 0);
	}

	public static RValue of(long value) {
		return new RValue(Type.LONG_TYPE, value);
	}

	public static RValue of(float value) {
		return new RValue(Type.FLOAT_TYPE, value);
	}

	public static RValue of(double value) {
		return new RValue(Type.DOUBLE_TYPE, value);
	}

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
				return new RValue(type, type);
			default:
				throw new IllegalStateException("Unsupported type: " + type);
		}
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
		return new RValue(common, add((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Subtract this value by another.
	 */
	public RValue sub(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, sub((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Multiply this value by another.
	 */
	public RValue mul(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, mul((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Divide this value by another.
	 */
	public RValue div(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, div((Number) value, (Number) other.value));
	}

	/**
	 * @param other Another value.
	 * @return Get remainder of this value by another.
	 */
	public RValue rem(RValue other) {
		Type common = commonMathType(type, other.type);
		if (value == null || other.value == null)
			return new RValue(common, null);
		return new RValue(common, rem((Number) value, (Number) other.value));
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
		return new RValue(common, shl((Number) value, (Number) other.value));
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
		return new RValue(common, shr((Number) value, (Number) other.value));
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
		return new RValue(common, ushr((Number) value, (Number) other.value));
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
		return new RValue(common, and((Number) value, (Number) other.value));
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
		return new RValue(common, or((Number) value, (Number) other.value));
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
		return new RValue(common, xor((Number) value, (Number) other.value));
	}

	/**
	 * @param type Type to act on.
	 * @return Act on a reference
	 */
	public RValue ref(Type type) {
		// Act on return type if passed type is method
		if (type != null && type.getSort() == Type.METHOD)
			return ref(type.getReturnType());
		// Don't act on 'null' values
		if (value == null || value.equals(Type.VOID_TYPE))
			throw new IllegalStateException("Cannot act on null reference value");
		// Don't try to do object stuff with non-objects
		if (!(value instanceof Type))
			throw new IllegalStateException("Cannot act on reference on non-reference value");
		// Nullify voids
		if (type != null && type.equals(Type.VOID_TYPE))
			return null;
		// Take on the type of the other
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
				return ov.type == null;
			else if(value == null)
				return isParent(type, ov.type) && ov.value == null;
			else
				return isParent(type, ov.type) && value.equals(ov.value);
		}
		return false;
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
		else if(other == UNINITIALIZED)
			return false;
		else if(other == NULL || other == null)
			return false;
		else if(type == null)
			return other.type == null;
		else
			return type.equals(other.type) || isParent(type, other.type) || other.isPromotionOf(this);
	}

	@Override
	public String toString() {
		if (this == UNINITIALIZED)
			return "<UNINITIALIZED>";
		else if (this == RETURNADDRESS_VALUE)
			return "<JSR_RET>";
		else
			return type + " - " + value;
	}

	private boolean isPromotionOf(RValue other) {
		int i1 = getSortPrefererence(type.getSort());
		int i2 = getSortPrefererence(other.getType().getSort());
		return i1 >= i2;
	}

	// =========================================================== //

	public static int getSortPrefererence(int sort) {
		return sortOrdering.indexOf(sort);
	}

	private static Type commonMathType(Type a, Type b) {
		if (a == null || b == null)
			throw new IllegalStateException("Cannot find common type of a null type");
		int i1 = getSortPrefererence(a.getSort());
		int i2 = getSortPrefererence(b.getSort());
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

	private static Number add(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() + b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() + b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() + b.longValue();
		else
			return a.intValue() + b.intValue();
	}

	private static Number sub(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() - b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() - b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() - b.longValue();
		else
			return a.intValue() - b.intValue();
	}

	private static Number mul(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() * b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() * b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() * b.longValue();
		else
			return a.intValue() * b.intValue();
	}

	private static Number div(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() / b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() / b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() / b.longValue();
		else
			return a.intValue() / b.intValue();
	}

	private static Number rem(Number a, Number b) {
		if(a instanceof Double || b instanceof Double)
			return a.doubleValue() % b.doubleValue();
		else if(a instanceof Float || b instanceof Float)
			return a.floatValue() % b.floatValue();
		else if(a instanceof Long || b instanceof Long)
			return a.longValue() % b.longValue();
		else
			return a.intValue() % b.intValue();
	}

	private static Number shl(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() << b.longValue();
		else
			return a.intValue() << b.intValue();
	}

	private static Number shr(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() >> b.longValue();
		else
			return a.intValue() >> b.intValue();
	}

	private static Number ushr(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() >>> b.longValue();
		else
			return a.intValue() >>> b.intValue();
	}

	private static Number and(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() & b.longValue();
		else
			return a.intValue() & b.intValue();
	}

	private static Number or(Number a, Number b) {
		if(a instanceof Long || b instanceof Long)
			return a.longValue() | b.longValue();
		else
			return a.intValue() | b.intValue();
	}

	private static Number xor(Number a, Number b) {
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
