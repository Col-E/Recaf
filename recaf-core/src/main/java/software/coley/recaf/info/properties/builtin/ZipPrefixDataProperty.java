package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track data appearing before the ZIP header in an archive.
 *
 * @author Matt Coley
 */
public class ZipPrefixDataProperty extends BasicProperty<byte[]> {
	public static final String KEY = "zip-prefix-data";

	/**
	 * @param data
	 * 		Optional data.
	 */
	public ZipPrefixDataProperty(@Nullable byte[] data) {
		super(KEY, data);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Optional data.
	 * {@code null} when no property value is assigned.
	 */
	@Nullable
	public static byte[] get(@Nonnull Info info) {
		Property<byte[]> property = info.getProperty(KEY);
		if (property != null) {
			return property.value();
		}
		return null;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param value
	 * 		Optional data.
	 */
	public static void set(@Nonnull Info info, @Nonnull byte[] value) {
		info.setProperty(new ZipPrefixDataProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
