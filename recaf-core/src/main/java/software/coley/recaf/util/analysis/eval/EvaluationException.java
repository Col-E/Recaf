package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;

/**
 * Exception thrown when {@link Evaluator} cannot evaluate a method.
 *
 * @author Matt Coley
 */
public class EvaluationException extends Exception {
	/**
	 * @param message
	 * 		Detail message.
	 */
	public EvaluationException(@Nonnull String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		Underlying cause/error.
	 * @param message
	 * 		Detail message.
	 */
	public EvaluationException(@Nonnull Throwable cause, @Nonnull String message) {
		super(message, cause);
	}
}
