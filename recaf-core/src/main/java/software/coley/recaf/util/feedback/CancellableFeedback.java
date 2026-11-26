package software.coley.recaf.util.feedback;

/**
 * Feedback for operations that can be cancelled.
 *
 * @author Matt Coley
 */
public interface CancellableFeedback extends Feedback {
	/**
	 * Mark operation as cancelled.
	 */
	void cancel();

	/**
	 * @return {@code true} to request operations stop handling input and end early.
	 * {@code false} to continue the operation.
	 */
	default boolean hasRequestedCancellation() {
		return false;
	}
}
