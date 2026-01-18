package software.coley.recaf.services.transform;

/**
 * Feedback that allows cancelling a transformation.
 *
 * @author Matt Coley
 */
public class CancellableTransformationFeedback implements TransformationFeedback {
	private boolean canceled;

	/**
	 * Mark transformation as cancelled.
	 */
	public void cancel() {
		canceled = true;
	}

	@Override
	public boolean hasRequestedCancellation() {
		return canceled;
	}
}
