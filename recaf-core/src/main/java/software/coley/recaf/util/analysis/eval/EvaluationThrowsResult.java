package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Result of an exception being thrown from the method or instruction block.
 *
 * @param exception
 * 		Thrown exception value.
 *
 * @author Matt Coley
 */
public record EvaluationThrowsResult(@Nonnull ReValue exception) implements EvaluationResult {}
