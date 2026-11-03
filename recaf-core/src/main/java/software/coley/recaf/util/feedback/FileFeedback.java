package software.coley.recaf.util.feedback;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.FileInfo;

/**
 * Feedback for operations that visit files.
 *
 * @author Matt Coley
 */
public interface FileFeedback extends Feedback {
	/**
	 * Called before an operation on a given file.
	 *
	 * @param file
	 * 		File to consider for visitation.
	 *
	 * @return {@code true} to visit a file in operations. {@code false} to skip.
	 */
	default boolean doVisitFile(@Nonnull FileInfo file) {
		return true;
	}
}
