package software.coley.recaf.util.analysis.eval;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.analysis.value.ReValue;

/**
 * Result returned by one {@link EvaluatorModel} after it inspects an operation.
 *
 * @param result
 * 		Terminal result produced by this model, or {@code null} when this model does not handle
 * 		the operation and the registry should offer it to the next model.
 *
 * @author Matt Coley
 */
public record ModelResult(@Nullable EvaluationResult result) {
	/** Constant for handling refusals by model implementations. */
	public static final ModelResult NOT_HANDLED = new ModelResult(null);

	/**
	 * @return {@code true} when this model handled the operation and produced a terminal result;
	 * {@code false} when the registry should continue with another model.
	 */
	public boolean handled() {
		return result != null;
	}

	/**
	 * @param value
	 * 		Value yielded by the model.
	 *
	 * @return Result indicating normal completion.
	 */
	@Nonnull
	public static ModelResult yielded(@Nonnull ReValue value) {
		return new ModelResult(new EvaluationYieldResult(value));
	}

	/**
	 * @param exception
	 * 		Exception thrown by the model.
	 *
	 * @return Result indicating modeled exceptional completion.
	 */
	@Nonnull
	public static ModelResult thrown(@Nonnull ReValue exception) {
		return new ModelResult(new EvaluationThrowsResult(exception));
	}

	/**
	 * @param reason
	 * 		Reason why this model could not complete an operation it handled.
	 *
	 * @return Result indicating that this model handled the operation but evaluation failed.
	 */
	@Nonnull
	public static ModelResult failed(@Nonnull String reason) {
		return new ModelResult(new EvaluationFailureResult(reason, null));
	}
}
