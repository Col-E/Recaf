package software.coley.recaf.info.member;

import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;

/**
 * Field component of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public interface FieldMember extends ClassMember {
	/**
	 * Fields can declare default values.
	 * Only acknowledged by the JVM when {@link #hasStaticModifier()} is {@code true}.
	 *
	 * @return Default value of the field. May be {@code null}.
	 */
	@Nullable
	Object getDefaultValue();

	@Override
	default boolean isField() {
		return true;
	}

	@Override
	default boolean isMethod() {
		return false;
	}
}
