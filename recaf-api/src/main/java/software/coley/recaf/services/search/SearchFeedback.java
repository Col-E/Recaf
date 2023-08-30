package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;

/**
 * Outline of search feedback capabilities. Allows for:
 * <ul>
 *     <li>In-progress search cancellation</li>
 *     <li>Filter classes and files visited by the search</li>
 * </ul>
 *
 * @author Matt Coley
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
	default boolean hasRequestedStop() {
		return false;
	}

	/**
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
	 * @param file
	 * 		File to consider for visitation.
	 *
	 * @return {@code true} to visit a file in search operations.
	 * {@code false} to skip.
	 */
	default boolean doVisitFile(@Nonnull FileInfo file) {
		return true;
	}
}
