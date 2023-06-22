package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.Info;
import software.coley.recaf.info.properties.BasicProperty;

/**
 * Built in property to track the original suffix of an {@link Info} type, primarily {@link ClassInfo} items.
 * <br>
 * Consider a WAR file, which prefixes all classes with {@link software.coley.recaf.info.WarFileInfo#WAR_CLASS_PREFIX}.
 * When we export the workspace, the {@link ClassInfo#getName()} will only yield the JVM name of the class, but not
 * the prefix. Thus, with this property holding the class prefix we can restore the full path of the {@link ClassInfo}
 * when exporting.
 *
 * @author Matt Coley
 * @see PathSuffixProperty
 * @see PathOriginalNameProperty
 */
public class PathPrefixProperty extends BasicProperty<String> {
	public static final String KEY = "path-prefix";

	/**
	 * @param value
	 * 		Prefix.
	 */
	public PathPrefixProperty(@Nonnull String value) {
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
		String prefix = get(info);
		if (prefix != null)
			return prefix + name;
		return name;
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Prefix associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static String get(@Nonnull Info info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param prefix
	 * 		Suffix to associate with the item.
	 */
	public static void set(@Nonnull Info info, @Nonnull String prefix) {
		info.setProperty(new PathPrefixProperty(prefix));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull Info info) {
		info.removeProperty(KEY);
	}
}
