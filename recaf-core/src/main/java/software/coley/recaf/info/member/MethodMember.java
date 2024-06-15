package software.coley.recaf.info.member;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.annotation.AnnotationElement;

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

	/**
	 * @return Element holding the default value for an annotation method.
	 */
	@Nullable
	AnnotationElement getAnnotationDefault();

	/**
	 * @param index
	 * 		Local variable index.
	 *
	 * @return Local variable at the index, or {@code null} if not known.
	 */
	@Nullable
	default LocalVariable getLocalVariable(int index) {
		return getLocalVariables().stream()
				.filter(v -> index == v.getIndex())
				.findFirst().orElse(null);
	}

	@Override
	default boolean isField() {
		return false;
	}

	@Override
	default boolean isMethod() {
		return true;
	}
}
