package software.coley.recaf.info.properties.builtin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.properties.BasicProperty;
import software.coley.recaf.info.properties.Property;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * Built in property to track if an {@link ClassInfo} contains unknown attributes.
 *
 * @author Matt Coley
 */
public class UnknownAttributesProperty extends BasicProperty<Collection<String>> {
	public static final String KEY = "unknown-attr";

	/**
	 * @param unknownAttributeNames
	 * 		Names of the unknown attributes.
	 */
	public UnknownAttributesProperty(@Nonnull Collection<String> unknownAttributeNames) {
		super(KEY, unknownAttributeNames);
	}

	/**
	 * @param info
	 * 		Class to strip this marker from.
	 */
	public static void remove(@Nonnull ClassInfo info) {
		info.removeProperty(KEY);
	}

	/**
	 * @param info
	 * 		Class to check for this marker on.
	 *
	 * @return {@code true} when the property is set on the class.
	 */
	public static boolean has(@Nonnull ClassInfo info) {
		return info.getProperties().containsKey(KEY);
	}

	/**
	 * @param info
	 * 		Class to check for this marker on.
	 *
	 * @return Collection of unknown attributes on the class.
	 */
	@Nonnull
	public static Collection<String> get(@Nonnull ClassInfo info) {
		Property<?> property = info.getProperties().get(KEY);
		if (property instanceof UnknownAttributesProperty unknown)
			return Objects.requireNonNullElse(unknown.value(), Collections.emptyList());
		return Collections.emptyList();
	}
}
