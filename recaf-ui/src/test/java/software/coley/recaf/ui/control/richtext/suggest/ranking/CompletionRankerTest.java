package software.coley.recaf.ui.control.richtext.suggest.ranking;

import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CompletionRanker}.
 */
class CompletionRankerTest {
	private record Item(String name, int rank, @Nullable String key, int baseline) {}

	@Test
	void learningPromotesPreviouslySelectedItemAtEqualRank() {
		TabCompletionConfig config = new TabCompletionConfig();
		RankingKeys<Item> keys = keys();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys);
		Item alpha = new Item("alpha", 0, "alpha", 0);
		Item beta = new Item("beta", 0, "beta", 0);

		assertEquals(List.of(alpha, beta), ranker.rank(List.of(beta, alpha)));
		ranker.recordSelection(beta);
		assertEquals(List.of(beta, alpha), ranker.rank(List.of(beta, alpha)));
	}

	@Test
	void eightSelectionsOfBetaOutrankOneSelectionOfAlpha() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item alpha = new Item("alpha", 0, "alpha", 0);
		Item beta = new Item("beta", 0, "beta", 0);

		ranker.recordSelection(alpha);
		for (int i = 0; i < 8; i++)
			ranker.recordSelection(beta);

		assertEquals(List.of(beta, alpha), ranker.rank(List.of(alpha, beta)));
	}

	@Test
	void nullLearningKeyIsNeverRecordedOrBoosted() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item nullKey = new Item("same", 0, null, 0);
		Item keyed = new Item("same", 0, "same", 0);

		ranker.recordSelection(nullKey);

		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
		assertEquals(List.of(nullKey, keyed), ranker.rank(List.of(nullKey, keyed)));
	}

	@Test
	void adaptiveRankingDisabledUsesStructuralOrder() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item alpha = new Item("alpha", 0, "alpha", 0);
		Item beta = new Item("beta", 1, "beta", 0);
		config.setAdaptiveRankingEnabled(false);

		for (int i = 0; i < 8; i++)
			ranker.recordSelection(beta);

		assertEquals(List.of(alpha, beta), ranker.rank(List.of(beta, alpha)));
	}

	@Test
	void learningDisabledRecordsNothing() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item alpha = new Item("alpha", 0, "alpha", 0);
		config.setAdaptiveRankingLearningEnabled(false);

		ranker.recordSelection(alpha);

		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
	}

	@Test
	void rankZeroStaysAheadOfRankOneRecordedSixtyFourTimes() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item zero = new Item("zero", 0, "zero", 0);
		Item one = new Item("one", 1, "one", 0);

		for (int i = 0; i < 64; i++)
			ranker.recordSelection(one);

		assertEquals(List.of(zero, one), ranker.rank(List.of(one, zero)));
	}

	@Test
	void baselineBoostIsCapped() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<Item> ranker = new CompletionRanker<>(config, keys());
		Item zeta = new Item("zeta", 0, "zeta", 9);
		Item alpha = new Item("alpha", 0, "alpha", 6);

		assertEquals(List.of(alpha, zeta), ranker.rank(List.of(zeta, alpha)));
	}

	private static RankingKeys<Item> keys() {
		return new RankingKeys<>(Item::rank, Item::name, Item::name, Item::key, Item::baseline);
	}
}
