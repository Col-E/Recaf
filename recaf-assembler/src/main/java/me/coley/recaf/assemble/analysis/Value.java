package me.coley.recaf.assemble.analysis;

import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.Types;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Base value type.
 *
 * @author Matt Coley
 */
public abstract class Value {

	// TODO: Track context (Ast) so users know what contributed?
	//  - But format it to look like a consecutive statement if possible
	//    - varName
	//    - varName.method(param)

	/**
	 * @return {@code true} if the type is {@link ObjectValue}.
	 */
	public boolean isObject() {
		return this instanceof ObjectValue;
	}

	/**
	 * @return {@code true} if the type is {@link ArrayValue}.
	 */
	public boolean isArray() {
		return this instanceof ArrayValue;
	}

	/**
	 * @return {@code true} if the type is {@link NullValue}.
	 */
	public boolean isNull() {
		return this instanceof NullValue;
	}

	/**
	 * @return {@code true} if the type is {@link NumericValue}.
	 */
	public boolean isNumeric() {
		return this instanceof NumericValue;
	}

	/**
	 * @return {@code true} if the type is {@link NumericValue}, representing a {@code long} or {@code double}.
	 */
	public boolean isWideNumeric() {
		return isNumeric() && ((NumericValue) this).requireWidePadding();
	}

	/**
	 * @return {@code true} if the type is {@link EmptyPoppedValue}.
	 */
	public boolean isEmptyStack() {
		return this instanceof EmptyPoppedValue;
	}

	/**
	 * @return {@code true} if the type is {@link WideReservedValue}.
	 */
	public boolean isWideReserved() {
		return this instanceof WideReservedValue;
	}

	/**
	 * @return Copy of the value.
	 */
	public Value copy() {
		return this;
	}

	/**
	 * Special case for values popped off an empty stack.
	 */
	public static class EmptyPoppedValue extends Value {
		@Override
		public String toString() {
			return "<Popped-Empty-Stack>";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof EmptyPoppedValue;
		}

		@Override
		public int hashCode() {
			// All empty-stack-popped values should have the same hash
			return -3;
		}
	}

	/**
	 * Special case for reserved values.
	 */
	public static class WideReservedValue extends Value {
		@Override
		public String toString() {
			return "<Wide-Reserved>";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof WideReservedValue;
		}

		@Override
		public int hashCode() {
			// All wide-reserved values should have the same hash
			return -2;
		}
	}

	/**
	 * Special case for null.
	 */
	public static class NullValue extends Value {
		@Override
		public String toString() {
			return "Null";
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof NullValue;
		}

		@Override
		public int hashCode() {
			// All null-values should have the same hash
			return -1;
		}
	}

	/**
	 * Value representing an array.
	 */
	public static class ArrayValue extends Value {
		private final Value[] array;
		private final Type elementType;
		private final int dimensions;

		/**
		 * @param dimensions
		 * 		Number of dimensions of the array.
		 * @param elementType
		 * 		Element type.
		 */
		public ArrayValue(int dimensions, Type elementType) {
			this(dimensions, -1, elementType);
		}

		/**
		 * @param dimensions
		 * 		Number of dimensions of the array.
		 * @param size
		 * 		Size of the array.
		 * @param elementType
		 * 		Element type.
		 */
		public ArrayValue(int dimensions, int size, Type elementType) {
			// Validate passed element type
			if (elementType.getSort() == Type.ARRAY)
				throw new IllegalArgumentException("Given element type still has array in descriptor");

			this.dimensions = dimensions;
			this.elementType = elementType;

			if (dimensions == 1 && size >= 0) {
				// Single dimensional array
				this.array = new Value[size];

				// We can fill in for unknown values
				if (elementType.getSort() <= Type.DOUBLE)
					for (int i = 0; i < size; i++)
						array[i] = new NumericValue(elementType);
				else
					for (int i = 0; i < size; i++)
						array[i] = new ObjectValue(Types.OBJECT_TYPE);
			} else if (dimensions > 1) {
				// If there are more than 1 dimensions, the size should == the num of dimensions
				// The size of each sub-array is declared on the stack
				this.array = new Value[dimensions];
				for (int i = 0; i < array.length; i++)
					array[i] = new ArrayValue(dimensions - 1, elementType);
			} else {
				// Unhandled
				this.array = null;
			}
		}

		/**
		 * @return The backing array.
		 * Will be {@code null} if the size is unknown.
		 */
		public Value[] getArray() {
			return array;
		}

		/**
		 * @return Array element type.
		 */
		public Type getElementType() {
			return elementType;
		}

		/**
		 * @return Array type.
		 */
		public Type getArrayType() {
			return Types.array(getElementType(), getDimensions());
		}

		/**
		 * @return Number of dimensions of the array.
		 */
		public int getDimensions() {
			return dimensions;
		}

		@Override
		public Value copy() {
			int size = (array != null) ? array.length : -1;
			ArrayValue copy = new ArrayValue(dimensions, size, elementType);
			if (array != null) {
				System.arraycopy(array, 0, copy.array, 0, array.length);
			}
			return copy;
		}

		@Override
		public String toString() {
			if (array == null)
				return "[?]";
			return "[" + Arrays.stream(getArray())
					.map(o -> o == null ? "null" : o.toString())
					.collect(Collectors.joining(", ")) +
					"]";
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ArrayValue that = (ArrayValue) o;
			return dimensions == that.dimensions && elementType.equals(that.elementType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(elementType, dimensions);
		}
	}

	/**
	 * Value holding a generic object.
	 */
	public static class ObjectValue extends Value implements Typed {
		private final Type type;

		/**
		 * @param type
		 * 		Type of the object.
		 */
		public ObjectValue(Type type) {
			this.type = Objects.requireNonNull(type, "Type cannot be null!");
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String toString() {
			return type.getInternalName();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ObjectValue that = (ObjectValue) o;
			return Objects.equals(type, that.type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(type);
		}
	}

	/**
	 * Value holding a string.
	 */
	public static class StringValue extends ObjectValue {
		private static final Type STRING_TYPE = Type.getType(String.class);
		private final String text;

		/**
		 * @param text
		 * 		Text of the value.
		 */
		public StringValue(String text) {
			super(STRING_TYPE);
			this.text = text;
		}

		/**
		 * @return Text of the value.
		 */
		public String getText() {
			return text;
		}

		@Override
		public String toString() {
			if (text == null)
				return "Null";
			return "\"" + text + "\"";
		}
	}

	/**
	 * Value holding a type instance.
	 */
	public static class TypeValue extends ObjectValue implements Typed {
		private static final Type CLASS_TYPE = Type.getType(Class.class);
		private final Type argumentType;

		/**
		 * @param type
		 * 		Type instance.
		 */
		public TypeValue(Type type) {
			super(CLASS_TYPE);
			this.argumentType = Objects.requireNonNull(type, "Type cannot be null!");
		}

		/**
		 * @return Class type.
		 */
		public Type getArgumentType() {
			return argumentType;
		}

		@Override
		public String toString() {
			return String.format("%s<%s>", CLASS_TYPE.getInternalName(), argumentType.getInternalName());
		}
	}

	/**
	 * Value holding a numeric value.
	 */
	public static class NumericValue extends Value {
		private final Number number;
		private final boolean primitive;
		private final Type type;

		/**
		 * @param type
		 * 		Numeric type.
		 */
		public NumericValue(Type type) {
			this(type, null, true);
		}

		/**
		 * @param type
		 * 		Numeric type.
		 * @param number
		 * 		Numeric value.
		 */
		public NumericValue(Type type, Number number) {
			this(type, number, true);
		}

		/**
		 * @param type
		 * 		Numeric type.
		 * @param number
		 * 		Numeric value.
		 * @param primitive
		 *        {@code true} to represent a primitive.
		 *        {@code false} to represent a boxed number.
		 */
		public NumericValue(Type type, Number number, boolean primitive) {
			this.type = type;
			this.number = number;
			this.primitive = primitive;
		}

		/**
		 * @return Numeric type.
		 */
		public Type getType() {
			return type;
		}

		/**
		 * @return Numeric value.
		 */
		public Number getNumber() {
			return number;
		}

		/**
		 * @return {@code true} to represent a primitive.
		 * {@code false} to represent a boxed number.
		 */
		public boolean isPrimitive() {
			return primitive;
		}

		/**
		 * @return {@code true} when the value holds a value that requires wide padding.
		 */
		public boolean requireWidePadding() {
			return isPrimitive() &&
					(type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE);
		}

		@Override
		public String toString() {
			if (number == null)
				return type.getClassName();
			return type.getClassName() + " " + number;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			NumericValue that = (NumericValue) o;
			return Objects.equals(type, that.type) &&
					Objects.equals(number, that.number) &&
					primitive == that.primitive;
		}

		@Override
		public int hashCode() {
			return Objects.hash(type, number, primitive);
		}
	}

	/**
	 * Value holding some handle.
	 */
	public static class HandleValue extends Value {
		private final HandleInfo info;

		/**
		 * @param info
		 * 		Some handle.
		 */
		public HandleValue(HandleInfo info) {
			this.info = Objects.requireNonNull(info, "Handle info cannot be null!");
		}

		/**
		 * @return Some handle.
		 */
		public HandleInfo getInfo() {
			return info;
		}

		@Override
		public String toString() {
			return info.print(PrintContext.DEFAULT_CTX);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			HandleValue that = (HandleValue) o;
			return info.equals(that.info);
		}

		@Override
		public int hashCode() {
			return Objects.hash(info);
		}
	}

	/**
	 * Value with a type.
	 */
	public interface Typed {
		/**
		 * @return Value type.
		 */
		Type getType();
	}
}
