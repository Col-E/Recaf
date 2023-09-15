package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track {@link FileInfo} instances that look like {@link ClassInfo} but could not be parsed.
 *
 * @author Matt Coley
 */
public class IllegalClassSuspectProperty extends BasicProperty<Boolean> {
	public static final IllegalClassSuspectProperty INSTANCE = new IllegalClassSuspectProperty();
	public static final String KEY = "ill-class";

	/**
	 * New property value.
	 */
	private IllegalClassSuspectProperty() {
		super(KEY, true);
	}

	@Override
	public boolean persistent() {
		return false;
	}

	/**
	 * @param info
	 * 		File info instance
	 *
	 * @return {@code true} when the file is a suspected malformed class.
	 */
	public static boolean get(@Nonnull FileInfo info) {
		Property<Boolean> property = info.getProperty(KEY);
		if (property != null) {
			Boolean value = property.value();
			return value != null && value;
		}
		return false;
	}

	/**
	 * Marks the file as being a suspected malformed class.
	 *
	 * @param info
	 * 		File info instance.
	 */
	public static void set(@Nonnull FileInfo info) {
		info.setProperty(IllegalClassSuspectProperty.INSTANCE);
	}

	/**
	 * @param info
	 * 		File info instance.
	 */
	public static void remove(@Nonnull FileInfo info) {
		info.removeProperty(KEY);
	}
}
