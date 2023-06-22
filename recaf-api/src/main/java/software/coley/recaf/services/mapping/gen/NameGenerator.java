package software.coley.recaf.services.mapping.gen;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.FieldMember;
import software.coley.recaf.info.member.MethodMember;

/**
 * Base name generation outline.
 *
 * @author Matt Coley
 */
public interface NameGenerator {
	/**
	 * @param info
	 * 		Class to rename.
	 *
	 * @return New class name.
	 */
	@Nonnull
	String mapClass(@Nonnull ClassInfo info);

	/**
	 * @param owner
	 * 		Class the field is defined in.
	 * @param field
	 * 		Field to rename.
	 *
	 * @return New field name.
	 */
	@Nonnull
	String mapField(@Nonnull ClassInfo owner, @Nonnull FieldMember field);

	/**
	 * @param owner
	 * 		Class the method is defined in.
	 * @param method
	 * 		Method to rename.
	 *
	 * @return New method name.
	 */
	@Nonnull
	String mapMethod(@Nonnull ClassInfo owner, @Nonnull MethodMember method);
}
