package me.coley.recaf.ui.behavior;

import java.util.EnumSet;
import java.util.List;

/**
 * Outline of a searchable component. Searchable content is generally literal text.
 *
 * @author Matt Coley
 */
public interface Searchable {
	/**
	 * @param modifiers
	 * 		Search modifiers.
	 * @param search
	 * 		Search query text.
	 *
	 * @return Result wrapper around multiple {@link SearchResult}s. The wrapper will contain an {@code int} index.
	 * If the search is the same as the last, this value is the same as the last return value,
	 * but the index is incremented.
	 */
	SearchResults next(EnumSet<SearchModifier> modifiers, String search);

	/**
	 * @param modifiers
	 * 		Search modifiers.
	 * @param search
	 * 		Search query text.
	 *
	 * @return Result wrapper around multiple {@link SearchResult}s. The wrapper will contain an {@code int} index.
	 * If the search is the same as the last, this value is the same as the last return value,
	 * but the index is decremented.
	 */
	SearchResults previous(EnumSet<SearchModifier> modifiers, String search);

	/**
	 * Wrapper of multiple {@link SearchResult}
	 */
	class SearchResults {
		private final List<SearchResult> allResults;
		private final EnumSet<SearchModifier> modifiers;
		private final String search;
		private int index;

		/**
		 * @param allResults
		 * 		All results.
		 * @param modifiers
		 * 		Search modifiers used to get the results.
		 * @param search
		 * 		Search query text  used to get the results.
		 */
		public SearchResults(List<SearchResult> allResults, EnumSet<SearchModifier> modifiers, String search) {
			this.allResults = allResults;
			this.modifiers = EnumSet.copyOf(modifiers);
			this.search = search;
		}

		/**
		 * Moves the index to the next result, wrapping around if needed.
		 */
		public void next() {
			index++;
			if (index >= count()) {
				index = 0;
			}
		}

		/**
		 * Moves the index to the previous result, wrapping around if needed.
		 */
		public void previous() {
			index--;
			if (index < 0) {
				index = Math.max(0, count() - 1);
			}
		}

		/**
		 * @param modifiers
		 * 		Search modifiers.
		 * @param search
		 * 		Search query text.
		 *
		 * @return {@code true} when the given parameters match
		 * those used to get the {@link #getAllResults() results of this wrapper}.
		 */
		public boolean isSameParameters(EnumSet<SearchModifier> modifiers, String search) {
			return this.modifiers.equals(modifiers) && this.search.equals(search);
		}

		/**
		 * @return {@code true} when there is one or more results.
		 */
		public boolean hasResults() {
			return !allResults.isEmpty();
		}

		/**
		 * @return Number of results.
		 */
		public int count() {
			return allResults.size();
		}

		/**
		 * @return Result of the current {@link #getIndex() index} within the {@link #getAllResults() results list}.
		 */
		public SearchResult result() {
			return allResults.get(getIndex());
		}

		/**
		 * @return All results.
		 */
		public List<SearchResult> getAllResults() {
			return allResults;
		}

		/**
		 * @return Current index.
		 *
		 * @see #result()
		 */
		public int getIndex() {
			return index;
		}

		/**
		 * @param index
		 * 		New index.
		 *
		 * @see #result()
		 */
		public void setIndex(int index) {
			this.index = index;
		}
	}

	/**
	 * Search result. Just a range wrapper.
	 */
	interface SearchResult {
		/**
		 * @return Result range start.
		 */
		int getStart();

		/**
		 * @return Result range stop.
		 */
		int getStop();

		/**
		 * Selects the result in the UI where it originates from.
		 */
		void select();
	}

	/**
	 * Search modifier.
	 */
	enum SearchModifier {
		/**
		 * Enables case-sensitive matching.
		 */
		CASE_SENSITIVE,
		/**
		 * Enables word-only matching.
		 */
		WORD,
		/**
		 * Overrides other modifiers; enables regex matching.
		 */
		REGEX
	}
}
