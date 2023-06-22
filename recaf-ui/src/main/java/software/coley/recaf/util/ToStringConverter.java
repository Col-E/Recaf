package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import javafx.util.StringConverter;

import java.util.function.Function;

/**
 * A {@link StringConverter} supporting only the {@code T --> String} conversion.
 *
 * @param <T>
 * 		Type to convert to String.
 *
 * @author Matt Coley
 */
public abstract class ToStringConverter<T> extends StringConverter<T> {
	/**
	 * @param func
	 * 		Conversion func.
	 * @param <T>
	 * 		Type to convert.
	 *
	 * @return Converter from function.
	 */
	@Nonnull
	public static <T> ToStringConverter<T> from(@Nonnull Function<T, String> func) {
		return new ToStringConverter<>() {
			@Override
			public String toString(T t) {
				return func.apply(t);
			}
		};
	}

	@Override
	public T fromString(String s) {
		throw new UnsupportedOperationException();
	}
}
