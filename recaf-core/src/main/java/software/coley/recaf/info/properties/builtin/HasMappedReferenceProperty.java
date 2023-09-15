package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;

/**
 * Built in property to track if an {@link ClassInfo} references mapped classes or members.
 *
 * @author Matt Coley
 * @see OriginalClassNameProperty Classes that have been renamed also have this applied.
 */
public class HasMappedReferenceProperty extends BasicProperty<Void> {
	public static final String KEY = "has-mapped-ref";
	private static final HasMappedReferenceProperty instance = new HasMappedReferenceProperty();

	private HasMappedReferenceProperty() {
		super(KEY, null);
	}

	/**
	 * @param info
	 * 		Class to mark as having a mapped reference.
	 */
	public static void set(@Nonnull ClassInfo info) {
		info.setProperty(instance);
	}

	/**
	 * @param info
	 * 		Class to strip this marker from.
	 */
	public static void remove(ClassInfo info) {
		info.removeProperty(KEY);
	}

	/**
	 * @param info
	 * 		Class to check for this marker on.
	 *
	 * @return {@code true} when the property is set on the class.
	 */
	public static boolean get(ClassInfo info) {
		return info.getProperties().containsKey(KEY);
	}
}
