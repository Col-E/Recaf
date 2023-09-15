package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;

import java.util.List;

/**
 * Field component of a {@link ClassInfo}.
 *
 * @author Matt Coley
 */
public interface MethodMember extends ClassMember {
	/**
	 * @return List of thrown exceptions.
	 */
	@Nonnull
	List<String> getThrownTypes();

	/**
	 * @return List of local variables.
	 */
	@Nonnull
	List<LocalVariable> getLocalVariables();

	@Override
	default boolean isField() {
		return false;
	}

	@Override
	default boolean isMethod() {
		return true;
	}
}
