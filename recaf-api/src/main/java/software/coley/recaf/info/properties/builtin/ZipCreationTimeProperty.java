package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track the original zip entry creation time used for an {@link Info}
 * value stored inside a ZIP container.
 * <br>
 * Unlike modification time, creation time is not part of the ZIP spec, so it is stored in the
 * extra data field.
 *
 * @author Matt Coley
 */
public class ZipCreationTimeProperty extends BasicProperty<Long> {
	public static final String KEY = "zip-create-time";

	/**
	 * @param value
	 * 		Creation time.
	 */
	public ZipCreationTimeProperty(long value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Creation time.
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
	 * 		Fallback time if there is no recorded type for the info instance.
	 *
	 * @return Creation time.
	 * {@code null} when no property value is assigned.
	 */
	public static long getOr(@Nonnull Info info, long fallback) {
		Long value = get(info);
		if (value == null) return fallback;
		return value;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param value
	 * 		Creation time.
	 */
	public static void set(@Nonnull Info info, long value) {
		info.setProperty(new ZipCreationTimeProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
