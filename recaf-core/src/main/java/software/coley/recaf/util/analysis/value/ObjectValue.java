package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.util.Types;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.impl.ObjectValueImpl;
import software.coley.recaf.util.analysis.value.impl.StringValueImpl;

/**
 * Value capable of recording exact content of certain object types.
 * <table>
 *     <tr><th>Content</th><th>Value usage</th></tr>
 *     <tr><td>{@code null}</td><td>{@link #VAL_OBJECT_NULL}</td></tr>
 *     <tr><td>{@code Any.class}</td><td>{@link #VAL_CLASS}</td></tr>
 *     <tr><td>{@code Method::reference}</td><td>{@link #VAL_METHOD_HANDLE}</td></tr>
 *     <tr><td>{@code "strings"}</td><td>{@link #string(String)} or {@link #string(Nullness)}</td></tr>
 * </table>
 *
 * @author Matt Coley
 */
public non-sealed interface ObjectValue extends ReValue {
	ObjectValue VAL_OBJECT = new ObjectValueImpl(Types.OBJECT_TYPE, Nullness.NOT_NULL);
	ObjectValue VAL_OBJECT_NULL = new ObjectValueImpl(Types.OBJECT_TYPE, Nullness.NULL);
	ObjectValue VAL_OBJECT_MAYBE_NULL = new ObjectValueImpl(Types.OBJECT_TYPE, Nullness.UNKNOWN);
	ObjectValue VAL_CLASS = new ObjectValueImpl(Types.CLASS_TYPE, Nullness.NOT_NULL);
	ObjectValue VAL_CLASS_NULL = new ObjectValueImpl(Types.CLASS_TYPE, Nullness.NULL);
	ObjectValue VAL_CLASS_MAYBE_NULL = new ObjectValueImpl(Types.CLASS_TYPE, Nullness.NULL);
	ObjectValue VAL_METHOD_TYPE = new ObjectValueImpl(Type.getObjectType("java/lang/invoke/MethodType"), Nullness.NOT_NULL);
	ObjectValue VAL_METHOD_HANDLE = new ObjectValueImpl(Type.getObjectType("java/lang/invoke/MethodType"), Nullness.NOT_NULL);
	ObjectValue VAL_JSR = new ObjectValueImpl(Type.VOID_TYPE, Nullness.NOT_NULL);

	/**
	 * @param text
	 * 		Exact string content.
	 *
	 * @return String value holding the exact content.
	 */
	@Nonnull
	static StringValue string(@Nullable String text) {
		if (text == null) return StringValue.VAL_STRING_NULL;
		if (text.isEmpty()) return StringValue.VAL_STRING_EMPTY;
		if (text.equals(" ")) return StringValue.VAL_STRING_SPACE;
		return new StringValueImpl(text);
	}

	/**
	 * @param nullness
	 * 		Null state of the {@link Class}.
	 *
	 * @return Object value for a class literal of the given nullness.
	 */
	@Nonnull
	static ObjectValue clazz(@Nonnull Nullness nullness) {
		return switch (nullness) {
			case NULL -> VAL_CLASS_NULL;
			case NOT_NULL -> VAL_CLASS;
			case UNKNOWN -> VAL_CLASS_MAYBE_NULL;
		};
	}

	/**
	 * @param nullness
	 * 		Null state of the string.
	 *
	 * @return String value of the given nullness.
	 */
	@Nonnull
	static StringValue string(@Nonnull Nullness nullness) {
		return switch (nullness) {
			case NULL -> StringValue.VAL_STRING_NULL;
			case NOT_NULL -> StringValue.VAL_STRING;
			case UNKNOWN -> StringValue.VAL_STRING_MAYBE_NULL;
		};
	}

	/**
	 * @param nullness
	 * 		Null state of the string.
	 *
	 * @return Object value of the given nullness with a type of {@link Object}.
	 */
	@Nonnull
	static ObjectValue object(@Nonnull Nullness nullness) {
		return switch (nullness) {
			case NULL -> VAL_OBJECT_NULL;
			case NOT_NULL -> VAL_OBJECT;
			case UNKNOWN -> VAL_OBJECT_MAYBE_NULL;
		};
	}

	@Nonnull
	static ObjectValue object(@Nonnull Type type, @Nonnull Nullness nullness) {
		if (Types.OBJECT_TYPE.equals(type))
			return object(nullness);
		if (Types.STRING_TYPE.equals(type))
			return string(nullness);
		if (Types.CLASS_TYPE.equals(type))
			return clazz(nullness);
		return new ObjectValueImpl(type, nullness);
	}

	/**
	 * @return Null state of this value.
	 */
	@Nonnull
	Nullness nullness();

	/**
	 * @return {@code true} when this value is known to be {@code null}.
	 */
	default boolean isNull() {
		return nullness() == Nullness.NULL;
	}

	@Override
	default int getSize() {
		return 1;
	}
}
