package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.llzip.format.compression.ZipCompressions;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track the original zip compression method used for an {@link Info}
 * value stored inside a ZIP container.
 *
 * @author Matt Coley
 */
public class ZipCompressionProperty extends BasicProperty<Integer> {
	public static final String KEY = "zip-compression";

	/**
	 * @param value
	 * 		Compression type. See {@link ZipCompressions} for values.
	 */
	public ZipCompressionProperty(int value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Compression type. See {@link ZipCompressions} for values.
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
	 * 		Fallback compression type if there is no recorded type for the info instance.
	 *
	 * @return Compression type. See {@link ZipCompressions} for values.
	 * {@code fallback} when no property value is assigned.
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
	 * 		Compression type. See {@link ZipCompressions} for values.
	 */
	public static void set(@Nonnull Info info, int value) {
		info.setProperty(new ZipCompressionProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
