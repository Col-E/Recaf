package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track the original index of the file as it appears in the zip structure.
 *
 * @author Matt Coley
 */
public class ZipEntryIndexProperty extends BasicProperty<Integer> {
	public static final String KEY = "zip-entry-index";

	/**
	 * @param value
	 * 		The index of the local file in its containing zip archive.
	 */
	public ZipEntryIndexProperty(int value) {
		super(KEY, value);
	}


	@Override
	public boolean persistent() {
		return false;
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Zip entry index.
	 * {@code null} when no property value is assigned.
	 */
	@Nullable
	public static Integer get(@Nonnull Info info) {
		Property<Integer> property = info.getProperty(KEY);
		if (property != null) {
			return property.value();
		}
		return null;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param fallback
	 * 		Fallback index if there is no recorded index for the info instance.
	 *
	 * @return Zip entry index.
	 * {@code null} when no property value is assigned.
	 */
	public static int getOr(@Nonnull Info info, int fallback) {
		Integer value = get(info);
		if (value == null) return fallback;
		return value;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param value
	 * 		Zip entry index.
	 */
	public static void set(@Nonnull Info info, int value) {
		info.setProperty(new ZipEntryIndexProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
