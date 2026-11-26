package software.coley.recaf.util.analysis.value;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.Nullness;
import software.coley.recaf.util.analysis.value.impl.StringValueImpl;

import java.util.Optional;

/**
 * Value capable of recording exact content of strings.
 *
 * @author Matt Coley
 */
public interface StringValue extends ObjectValue {
	StringValue VAL_STRING = new StringValueImpl(Nullness.NOT_NULL);
	StringValue VAL_STRING_MAYBE_NULL = new StringValueImpl(Nullness.UNKNOWN);
	StringValue VAL_STRING_NULL = new StringValueImpl(Nullness.NULL);
	StringValue VAL_STRING_EMPTY = new StringValueImpl("");
	StringValue VAL_STRING_SPACE = new StringValueImpl(" ");

	/**
	 * @return String content of value. Empty if {@link #hasKnownValue() not known}.
	 */
	@Nonnull
	Optional<String> getText();

	/**
	 * @param otherValue
	 * 		Value to check against.
	 *
	 * @return {@code true} when this value is known to equal the given value.
	 * {@code false} is returned when the value is unknown or known to differ.
	 */
	default boolean isEqualTo(@Nullable String otherValue) {
		Optional<String> text = getText();
		return text.map(s -> s.equals(otherValue))
				.orElseGet(() -> otherValue == null && isNull());
	}

	/**
	 * @param otherValue
	 * 		Value to check against.
	 *
	 * @return {@code true} when this value is known to differ from the given value.
	 * {@code false} is returned when the value is unknown or known to equal.
	 */
	default boolean isNotEqualTo(@Nullable String otherValue) {
		Optional<String> text = getText();
		if (text.isPresent())
			return !text.get().equals(otherValue);
		if (otherValue == null)
			return isNotNull();
		return isNull();
	}
}
