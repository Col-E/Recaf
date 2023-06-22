package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;

/**
 * Built in property to track the suffix of an {@link Info} type, primarily the extension of {@link ClassInfo} files.
 * In most cases the value will be {@code .class}.
 *
 * @author Matt Coley
 * @see PathPrefixProperty
 * @see PathOriginalNameProperty
 */
public class PathSuffixProperty extends BasicProperty<String> {
	public static final String KEY = "path-suffix";

	/**
	 * @param value
	 * 		Suffix.
	 */
	public PathSuffixProperty(@Nonnull String value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Name of the info, with the suffix applied if any exist.
	 */
	@Nonnull
	public static String map(@Nonnull Info info) {
		String name = info.getName();
		String suffix = get(info);
		if (suffix != null)
			return name + suffix;
		return name;
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Suffix associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static String get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param suffix
	 * 		Suffix to associate with the item.
	 */
	public static void set(@Nonnull Info info, @Nonnull String suffix) {
		info.setProperty(new PathSuffixProperty(suffix));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
