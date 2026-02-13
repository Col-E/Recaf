package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;

/**
 * Result of evaluating a method or block of instructions.
 *
 * @author Matt Coley
 */
public sealed interface EvaluationResult permits EvaluationFailureResult, EvaluationThrowsResult, EvaluationYieldResult {
	/**
	 * Result of the method or block not being able to be evaluated.
	 *
	 * @param reason
	 * 		Reason why evaluation failed.
	 *
	 * @return Evaluation failure result.
	 */
	static EvaluationFailureResult cannotEvaluate(@Nonnull String reason) {
		return new EvaluationFailureResult(reason, new EvaluationException(reason));
	}

	/**
	 * Result of the method or block not being able to be evaluated.
	 *
	 * @param reason
	 * 		Reason why evaluation failed.
	 * @param cause
	 * 		Optional cause of failure.
	 *
	 * @return Evaluation failure result.
	 */
	static EvaluationFailureResult cannotEvaluate(@Nonnull String reason, @Nonnull Throwable cause) {
		return new EvaluationFailureResult(reason, new EvaluationException(cause, reason));
	}
}
