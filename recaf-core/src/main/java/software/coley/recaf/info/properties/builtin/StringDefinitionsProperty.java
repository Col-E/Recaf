package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Built in property to track what strings are defined by this type.
 *
 * @author Matt Coley
 */
public class StringDefinitionsProperty extends BasicProperty<SortedSet<String>> {
	public static final String KEY = "strings";

	/**
	 * @param strings
	 * 		Collection of defined strings.
	 */
	public StringDefinitionsProperty(@Nonnull Collection<String> strings) {
		super(KEY, new TreeSet<>(strings));
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Set of defined strings, or {@code null} when no association exists.
	 */
	@Nullable
	public static SortedSet<String> get(@Nonnull ClassInfo info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param strings
	 * 		Collection of defined strings.
	 */
	public static void set(@Nonnull ClassInfo info, @Nonnull Collection<String> strings) {
		info.setProperty(new StringDefinitionsProperty(strings));
	}

	/**
	 * @param info
	 * 		Info instance.
	 */
	public static void remove(@Nonnull ClassInfo info) {
		info.removeProperty(KEY);
	}

	@Override
	public boolean persistent() {
		// Modifications to a class will likely invalidate this data
		return false;
	}
}
