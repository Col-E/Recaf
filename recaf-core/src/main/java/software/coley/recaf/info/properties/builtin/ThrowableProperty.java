package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track {@link ClassInfo} instances that inherit {@link Throwable} either directly
 * or indirectly.
 *
 * @author Matt Coley
 */
public class ThrowableProperty extends BasicProperty<Boolean> {
	public static final String KEY = "is-throwable";

	/**
	 * New property value.
	 */
	public ThrowableProperty() {
		super(KEY, true);
	}

	@Override
	public boolean persistent() {
		return false;
	}

	/**
	 * @param info
	 * 		Class info instance
	 *
	 * @return {@code true} when the class inherits from {@link Throwable}.
	 */
	public static boolean get(@Nonnull ClassInfo info) {
		Property<Boolean> property = info.getProperty(KEY);
		if (property != null) {
			Boolean value = property.value();
			return value != null && value;
		}
		return false;
	}

	/**
	 * Marks the class as inheriting {@link Throwable}
	 *
	 * @param info
	 * 		Class info instance.
	 */
	public static void set(@Nonnull ClassInfo info) {
		info.setProperty(new ThrowableProperty());
	}

	/**
	 * @param info
	 * 		Class info instance.
	 */
	public static void remove(@Nonnull ClassInfo info) {
		info.removeProperty(KEY);
	}
}
