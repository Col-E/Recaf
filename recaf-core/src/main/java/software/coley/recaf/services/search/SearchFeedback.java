package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.services.search.result.Result;
import software.coley.recaf.services.search.result.Results;

/**
 * Outline of search feedback capabilities. Allows for:
 * <ul>
 *     <li>In-progress search cancellation</li>
 *     <li>Filter classes and files visited by the search</li>
 * </ul>
 *
 * @author Matt Coley
 * @see CancellableSearchFeedback Basic cancellable implementation.
 */
public interface SearchFeedback {
	/**
	 * Default implementation that runs searches to completion, without any filtering.
	 */
	SearchFeedback NO_OP = new SearchFeedback() {
	};

	/**
	 * @return {@code true} to request {@link SearchService} stops handling input to end the search early.
	 * {@code false} to continue the search.
	 */
	default boolean hasRequestedCancellation() {
		return false;
	}

	/**
	 * Called before checking a given class's contents against some search query.
	 *
	 * @param cls
	 * 		Class to consider for visitation.
	 *
	 * @return {@code true} to visit a class in search operations.
	 * {@code false} to skip.
	 */
	default boolean doVisitClass(@Nonnull ClassInfo cls) {
		return true;
	}

	/**
	 * Called before checking a given file's contents against some search query.
	 *
	 * @param file
	 * 		File to consider for visitation.
	 *
	 * @return {@code true} to visit a file in search operations.
	 * {@code false} to skip.
	 */
	default boolean doVisitFile(@Nonnull FileInfo file) {
		return true;
	}

	/**
	 * Called when a search query finds a matching result.
	 *
	 * @param result
	 * 		Result to consider.
	 *
	 * @return {@code true} to accept the result into the final {@link Results} collection.
	 * {@code false} to drop it.
	 */
	default boolean doAcceptResult(@Nonnull Result<?> result) {
		return true;
	}
}
