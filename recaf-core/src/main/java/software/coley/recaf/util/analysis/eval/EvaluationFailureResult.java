package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Result of the method or block not being able to be evaluated.
 *
 * @param reason
 * 		Reason why evaluation failed.
 * @param cause
 * 		Optional cause of failure.
 *
 * @author Matt Coley
 */
public record EvaluationFailureResult(@Nonnull String reason,
                                      @Nullable EvaluationException cause) implements EvaluationResult {}
