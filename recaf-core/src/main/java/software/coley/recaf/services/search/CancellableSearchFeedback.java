package software.coley.recaf.services.search;

/**
 * Feedback that allows cancelling a search.
 *
 * @author Matt Coley
 */
public class CancellableSearchFeedback implements SearchFeedback {
	private boolean canceled;

	/**
	 * Mark search as cancelled.
	 */
	public void cancel() {
		canceled = true;
	}

	@Override
	public boolean hasRequestedCancellation() {
		return canceled;
	}
}