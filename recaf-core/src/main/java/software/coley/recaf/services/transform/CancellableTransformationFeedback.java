package software.coley.recaf.services.transform;

/**
 * Feedback that allows cancelling a transformation.
 *
 * @author Matt Coley
 */
public class CancellableTransformationFeedback implements TransformationFeedback {
	private boolean cancelled;

	/**
	 * Mark transformation as cancelled.
	 */
	public void cancel() {
		cancelled = true;
	}

	@Override
	public boolean hasRequestedCancellation() {
		return cancelled;
	}
}
