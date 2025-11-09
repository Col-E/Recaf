package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Built in property to track what classes are referenced by this type.
 *
 * @author Matt Coley
 */
public class ReferencedClassesProperty extends BasicProperty<NavigableSet<String>> {
	public static final String KEY = "referenced-classes";

	/**
	 * @param classes
	 * 		Collection of referenced classes.
	 */
	public ReferencedClassesProperty(@Nonnull Collection<String> classes) {
		super(KEY, Collections.unmodifiableNavigableSet(new TreeSet<>(classes)));
	}

	/**
	 * @param info
	 * 		Info instance.
	 *
	 * @return Set of referenced classes, or {@code null} when no association exists.
	 */
	@Nullable
	public static NavigableSet<String> get(@Nonnull ClassInfo info) {
		return info.getPropertyValueOrNull(KEY);
	}

	/**
	 * @param info
	 * 		Info instance.
	 * @param classes
	 * 		Collection of referenced classes.
	 *
	 * @return Sorted set of referenced classes assigned.
	 */
	@Nonnull
	public static NavigableSet<String> set(@Nonnull ClassInfo info, @Nonnull Collection<String> classes) {
		ReferencedClassesProperty property = new ReferencedClassesProperty(classes);
		info.setProperty(property);
		return Objects.requireNonNull(property.value());
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
