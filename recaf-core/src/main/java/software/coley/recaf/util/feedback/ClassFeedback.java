package software.coley.recaf.util.feedback;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;

/**
 * Feedback for operations that visit classes.
 *
 * @author Matt Coley
 */
public interface ClassFeedback extends Feedback {
	/**
	 * Called before an operation on a given class.
	 *
	 * @param cls
	 * 		Class to consider for visitation.
	 *
	 * @return {@code true} to visit a class in operations. {@code false} to skip.
	 */
	default boolean doVisitClass(@Nonnull ClassInfo cls) {
		return true;
	}
}
