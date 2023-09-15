package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.services.mapping.MappingApplier;

/**
 * Built in property to track the original name of an {@link ClassInfo}.
 *
 * @author Matt Coley
 * @see HasMappedReferenceProperty If a class isn't mapped, but has references to something that is, this is added.
 * @see MappingApplier Adds this property to renamed classes.
 */
public class OriginalClassNameProperty extends BasicProperty<String> {
	public static final String KEY = "original-class-name";

	/**
	 * @param value
	 * 		Original class name.
	 */
	public OriginalClassNameProperty(@Nonnull String value) {
		super(KEY, value);
	}

	/**
	 * @param info
	 * 		Class info instance.
	 *
	 * @return Original name of the class if set, otherwise the existing class name.
	 */
	@Nonnull
	public static String map(@Nonnull ClassInfo info) {
		String name = info.getName();
		String original = info.getPropertyValueOrNull(KEY);
		if (original != null)
			return original;
		return name;
	}

	/**
	 * @param info
	 * 		Class info instance.
	 *
	 * @return Class name associated with instance, or {@code null} when no association exists.
	 */
	@Nullable
	public static String get(@Nonnull ClassInfo info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Class info instance.
	 * @param original
	 * 		Original class name to associate with the item.
	 */
	public static void set(@Nonnull ClassInfo info, @Nonnull String original) {
		info.setProperty(new OriginalClassNameProperty(original));
	}

	/**
	 * @param info
	 * 		Class info instance.
	 */
	public static void remove(@Nonnull ClassInfo info) {
		info.removeProperty(KEY);
	}
}
