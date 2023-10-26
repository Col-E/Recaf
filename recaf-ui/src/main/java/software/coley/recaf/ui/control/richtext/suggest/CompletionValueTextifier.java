package software.coley.recaf.ui.control.richtext.suggest;

import jakarta.annotation.Nonnull;

/**
 * Basic T value to string mapper.
 *
 * @param <T>
 * 		Completion value type.
 *
 * @author Matt Coley
 */
public interface CompletionValueTextifier<T> {
	/**
	 * @param value
	 * 		Value to represent as a string.
	 *
	 * @return String representation of the value.
	 */
	@Nonnull
	String toText(@Nonnull T value);
}
