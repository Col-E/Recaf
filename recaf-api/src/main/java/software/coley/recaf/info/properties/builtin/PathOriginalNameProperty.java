package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;

/**
 * Built in property to track the original name of an {@link Info} type, primarily {@link ClassInfo} items.
 *
 * @author Matt Coley
 * @see PathPrefixProperty
 * @see PathSuffixProperty
 */
public class PathOriginalNameProperty extends BasicProperty<String> {
	public static final String KEY = "path-original-full";

	/**
	 * @param value
	 * 		Original path name.
	 */
	public PathOriginalNameProperty(@Nonnull String value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Original name of the info if set, otherwise the existing info name.
	 */
	@Nonnull
	public static String map(@Nonnull Info info) {
		String name = info.getName();
		String original = info.getPropertyValueOrNull(KEY);
		if (original != null)
			return original;
		return name;
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Original name associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static String get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param original
	 * 		Original name to associate with the item.
	 */
	public static void set(@Nonnull Info info, @Nonnull String original) {
		info.setProperty(new PathOriginalNameProperty(original));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
