package software.coley.recaf.services.search;

/**
 * Feedback that allows cancelling a search.
 *
 * @author Matt Coley
 */
public class CancellableSearchFeedback implements SearchFeedback {
	private boolean cancelled;

	/**
	 * Mark search as cancelled.
	 */
	public void cancel() {
		cancelled = true;
	}

	@Override
	public boolean hasRequestedCancellation() {
		return cancelled;
	}
}