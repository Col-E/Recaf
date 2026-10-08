package software.coley.recaf.ui.control.richtext.suggest.ranking;

import jakarta.annotation.Nonnull;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;

import java.util.Comparator;
import java.util.List;

/**
 * Adaptive ranking for tab completions.
 *
 * @param <T>
 * 		Completion type.
 *
 * @author Matt Coley
 */
public final class CompletionRanker<T> {
	private static final int MAX_BASELINE_BOOST = 6;
	private static final int MAX_LEARNED_BOOST = 8;
	private final TabCompletionConfig config;
	private final RankingKeys<T> keys;
	private final Comparator<T> defaultOrder;
	private final Comparator<T> adaptiveOrder;

	/**
	 * @param config
	 * 		Config holding adaptive ranking settings and learned usage counts.
	 * @param keys
	 * 		Language-specific ranking accessors.
	 */
	public CompletionRanker(@Nonnull TabCompletionConfig config, @Nonnull RankingKeys<T> keys) {
		this.config = config;
		this.keys = keys;
		defaultOrder = Comparator
				.comparingInt(keys.rank())
				.thenComparing(keys.sortKey(), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(keys.displayText(), String.CASE_INSENSITIVE_ORDER);
		adaptiveOrder = Comparator
				.comparingInt(keys.rank())
				.thenComparingInt(this::adaptiveRank)
				.thenComparing(keys.sortKey(), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(keys.displayText(), String.CASE_INSENSITIVE_ORDER);
	}

	/**
	 * @param items
	 * 		Raw completion results.
	 *
	 * @return Sorted completion results.
	 */
	@Nonnull
	public List<T> rank(@Nonnull List<T> items) {
		Comparator<T> order = config.isAdaptiveRankingEnabled() ? adaptiveOrder : defaultOrder;
		return items.stream().sorted(order).toList();
	}

	/**
	 * Track the selection of a completion for adaptive ranking purposes.
	 *
	 * @param item
	 * 		Accepted completion.
	 */
	public void recordSelection(@Nonnull T item) {
		if (!config.isAdaptiveRankingLearningEnabled())
			return;
		String key = keys.learningKey().apply(item);
		if (key != null)
			config.recordAdaptiveRankingSelection(key);
	}

	/**
	 * @param item
	 * 		Completion to rank.
	 *
	 * @return Adaptive rank value. Lower values are ranked higher.
	 */
	private int adaptiveRank(@Nonnull T item) {
		String key = keys.learningKey().apply(item);
		if (key == null)
			return 0;
		int baseline = Math.min(MAX_BASELINE_BOOST, keys.baselineBoost().applyAsInt(item));
		return -(baseline + learnedBoost(key));
	}

	/**
	 * @param key
	 * 		Learning key for a completion.
	 *
	 * @return Learned boost value. Lower values are ranked higher.
	 */
	private int learnedBoost(@Nonnull String key) {
		int count = config.getAdaptiveRankingUsageCount(key);
		if (count <= 0)
			return 0;
		return Math.min(MAX_LEARNED_BOOST, 2 + (31 - Integer.numberOfLeadingZeros(count)));
	}
}
