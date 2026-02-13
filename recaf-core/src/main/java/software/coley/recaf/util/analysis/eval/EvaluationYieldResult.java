package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Result of a normal return from the method or instruction block.
 *
 * @param value
 * 		Return value of the method or instruction block.
 * 		If the block has no defined {@code return} instruction then the stack top value at the end of execution.
 *
 * @author Matt Coley
 */
public record EvaluationYieldResult(@Nonnull ReValue value) implements EvaluationResult {}
