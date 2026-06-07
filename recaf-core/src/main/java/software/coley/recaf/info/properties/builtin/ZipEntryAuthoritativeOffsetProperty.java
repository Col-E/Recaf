package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track the original offset of the file <i>(Using the central directory header if available)</i>
 * as it appears in the zip structure.
 *
 * @author Matt Coley
 */
public class ZipEntryAuthoritativeOffsetProperty extends BasicProperty<Long> {
	public static final String KEY = "zip-entry-authoritative-offset";

	/**
	 * @param value
	 * 		The offset of the local file in its containing zip archive.
	 */
	public ZipEntryAuthoritativeOffsetProperty(long value) {
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
	 * @return Zip entry offset.
	 * {@code null} when no property value is assigned.
	 */
	@Nullable
	public static Long get(@Nonnull Info info) {
		Property<Long> property = info.getProperty(KEY);
		if (property != null) {
			return property.value();
		}
		return null;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param fallback
	 * 		Fallback offset if there is no recorded offset for the info instance.
	 *
	 * @return Zip entry offset.
	 * {@code null} when no property value is assigned.
	 */
	public static long getOr(@Nonnull Info info, int fallback) {
		Long value = get(info);
		if (value == null) return fallback;
		return value;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param value
	 * 		Zip entry offset.
	 */
	public static void set(@Nonnull Info info, long value) {
		info.setProperty(new ZipEntryAuthoritativeOffsetProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
