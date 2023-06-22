package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

/**
 * Built in property to track the original zip compression method used for an {@link Info}
 * value stored inside a ZIP container.
 *
 * @author Matt Coley
 */
public class ZipCommentProperty extends BasicProperty<String> {
	public static final String KEY = "zip-comment";

	/**
	 * @param value
	 * 		Optional comment.
	 */
	public ZipCommentProperty(@Nullable String value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Optional comment.
	 * {@code null} when no property value is assigned.
	 */
	@Nullable
	public static String get(@Nonnull Info info) {
		Property<String> property = info.getProperty(KEY);
		if (property != null) {
			return property.value();
		}
		return null;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param fallback
	 * 		Fallback comment if there is no recorded type for the info instance.
	 *
	 * @return Optional comment.
	 * {@code null} when no property value is assigned.
	 */
	public static String getOr(@Nonnull Info info, String fallback) {
		String value = get(info);
		if (value == null) return fallback;
		return value;
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param value
	 * 		Optional comment.
	 */
	public static void set(@Nonnull Info info, @Nonnull String value) {
		info.setProperty(new ZipCommentProperty(value));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
