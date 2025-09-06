package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;

/**
 * Exception thrown when {@link ReEvaluator} cannot evaluate a method.
 *
 * @author Matt Coley
 */
public class ReEvaluationException extends Exception {
	/**
	 * @param message
	 * 		Detail message.
	 */
	public ReEvaluationException(@Nonnull String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		Underlying cause/error.
	 * @param message
	 * 		Detail message.
	 */
	public ReEvaluationException(@Nonnull Throwable cause, @Nonnull String message) {
		super(message, cause);
	}
}
