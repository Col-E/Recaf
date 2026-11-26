package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

/**
 * Pair of a name and type.
 *
 * @param name
 * 		Element name.
 * @param type
 * 		Element type.
 */
public record NameTypePair(@Nonnull String name, @Nonnull String type) {
	/**
	 * @param method
	 * 		Method to create a pair for.
	 *
	 * @return Pair of the method's name and descriptor.
	 */
	@Nonnull
	public static NameTypePair of(@Nonnull MethodMember method) {
		return new NameTypePair(method.getName(), method.getDescriptor());
	}

	/**
	 * @param field
	 * 		Field to create a pair for.
	 *
	 * @return Pair of the field's name and descriptor.
	 */
	@Nonnull
	public static NameTypePair of(@Nonnull FieldMember field) {
		return new NameTypePair(field.getName(), field.getDescriptor());
	}
}
