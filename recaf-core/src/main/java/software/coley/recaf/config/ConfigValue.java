package software.coley.recaf.config;

import jakarta.annotation.Nonnull;
import software.coley.observables.Observable;

/**
 * An option stored in a {@link ConfigContainer} object.
 *
 * @param <T>
 * 		Value type.
 *
 * @author Matt Coley
 */
public interface ConfigValue<T> {
	/**
	 * @return Unique ID of this value.
	 */
	@Nonnull
	String getId();

	/**
	 * @return Value type class.
	 */
	@Nonnull
	Class<T> getType();

	/**
	 * @return Observable of value.
	 */
	@Nonnull
	Observable<T> getObservable();

	/**
	 * @param value
	 * 		Value to set.
	 */
	default void setValue(@Nonnull T value) {
		getObservable().setValue(value);
	}

	/**
	 * @return Current value.
	 */
	@Nonnull
	default T getValue() {
		return getObservable().getValue();
	}

	/**
	 * @return {@code true} for hidden values not to be shown to users <i>(Strictly in the UI)</i>.
	 */
	default boolean isHidden() {
		return false;
	}
}
