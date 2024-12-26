package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track {@link FileInfo} instances that have a ZIP file header within their contents.
 *
 * @author Matt Coley
 */
public class ZipMarkerProperty extends BasicProperty<Boolean> {
	public static final String KEY = "has-zip-marker";

	/**
	 * New property value.
	 */
	public ZipMarkerProperty() {
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
	 * @return {@code true} when the file has a ZIP file header in the contents.
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
	 * Marks the class as inheriting containing a ZIP file header.
	 *
	 * @param info
	 * 		File info instance.
	 */
	public static void set(@Nonnull FileInfo info) {
		info.setProperty(new ZipMarkerProperty());
	}

	/**
	 * @param info
	 * 		File info instance.
	 */
	public static void remove(@Nonnull FileInfo info) {
		info.removeProperty(KEY);
	}
}
