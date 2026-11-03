package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;
import software.coley.recaf.util.feedback.DefaultCancellableFeedback;

/**
 * Feedback for search operations.
 *
 * @author Matt Coley
 */
public class SearchFeedback extends DefaultCancellableFeedback {
	public static final SearchFeedback DEFAULT = new SearchFeedback();

	/**
	 * Called when a search query finds a matching result.
	 *
	 * @param result
	 * 		Result to consider.
	 *
	 * @return {@code true} to accept the result into the final {@link Results} collection.
	 * {@code false} to drop it.
	 */
	public boolean doAcceptResult(@Nonnull Result<?> result) {
		return true;
	}
}