package software.coley.recaf.ui.control.richtext.suggest.java;

import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JavaCompletionRanker}.
 */
class JavaCompletionRankerTest {
	@Test
	void learningPromotesPreviouslyAcceptedCompletion() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion alpha = new JavaCompletion(CompletionKind.TYPE, "Alpha", "Alpha", 30, null, "Alpha", 0);
		JavaCompletion beta = new JavaCompletion(CompletionKind.TYPE, "Beta", "Beta", 30, null, "Beta", 0);

		// Initially the order should be tie-broken by sort-key since they have the same rank
		// and there is no special baseline boost for either.
		assertEquals(List.of(alpha, beta), ranker.rank(List.of(beta, alpha)));

		// If we record the second one as selected, it should get a boost that promotes it above the first one.
		ranker.recordSelection(beta);

		// The other should be swapped now.
		assertEquals(List.of(beta, alpha), ranker.rank(List.of(beta, alpha)));
	}

	@Test
	void repeatedLearningStrengthensBoost() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion alpha = new JavaCompletion(CompletionKind.TYPE, "Alpha", "Alpha", 30, null, "Alpha", 0);
		JavaCompletion beta = new JavaCompletion(CompletionKind.TYPE, "Beta", "Beta", 30, null, "Beta", 0);

		// Same idea as before but now both get recorded selections. But beta gets more, so it should be ranked higher.
		ranker.recordSelection(alpha);
		for (int i = 0; i < 8; i++)
			ranker.recordSelection(beta);

		// Order should show beta first again.
		assertEquals(List.of(beta, alpha), ranker.rank(List.of(alpha, beta)));

		// Learned usage count for beta should be higher than alpha.
		assertTrue(config.getAdaptiveRankingUsageCount(ranker.stableKeyOrThrow(beta)) >
				config.getAdaptiveRankingUsageCount(ranker.stableKeyOrThrow(alpha)));
	}

	@Test
	void learningIsKindAware() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion fieldAlpha = new JavaCompletion(CompletionKind.FIELD, "alpha : int", "alpha", 30, null, "alpha", 0);
		JavaCompletion fieldZeta = new JavaCompletion(CompletionKind.FIELD, "zeta : int", "zeta", 30, null, "zeta", 0);
		JavaCompletion methodZeta = new JavaCompletion(CompletionKind.METHOD, "zeta", "zeta", 30, null, "zeta", 0);

		// If we have two completions with the same name but different kinds,
		// they should have different stable keys and not interfere with each other's learning.
		ranker.recordSelection(methodZeta);

		// The field zeta should not have any learned usage count since we only recorded the method zeta as selected.
		assertEquals(0, config.getAdaptiveRankingUsageCount(ranker.stableKeyOrThrow(fieldZeta)));
		assertEquals(1, config.getAdaptiveRankingUsageCount(ranker.stableKeyOrThrow(methodZeta)));
	}

	@Test
	void learningDoesNotCrossStructuralRankBoundaries() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion structurallyBetter = new JavaCompletion(CompletionKind.FIELD, "alpha : int", "alpha", 0, null, "alpha", 0);
		JavaCompletion learnedButWorse = new JavaCompletion(CompletionKind.FIELD, "beta : int", "beta", 1, null, "beta", 0);

		// Even if we record the worse one as selected many times,
		// it should never surpass the completion that is structurally ranked better (1 vs 0).
		for (int i = 0; i < 64; i++)
			ranker.recordSelection(learnedButWorse);
		assertEquals(List.of(structurallyBetter, learnedButWorse),
				ranker.rank(List.of(learnedButWorse, structurallyBetter)));
	}

	@Test
	void resetClearsAdaptiveRankingState() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		ranker.recordSelection(new JavaCompletion(CompletionKind.TYPE, "String", "String", 30, null, "String", 0));

		// I mean, yeah.
		ranker.reset();
		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
	}

	@Test
	void disablingAdaptiveRankingKeepsDefaultOrdering() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion alpha = new JavaCompletion(CompletionKind.TYPE, "Alpha", "Alpha", 30, null, "Alpha", 0);
		JavaCompletion beta = new JavaCompletion(CompletionKind.TYPE, "Beta", "Beta", 30, null, "Beta", 0);

		// Disable adaptive ranking.
		config.setAdaptiveRankingEnabled(false);

		// Even if we record one as selected, it should not affect the order since adaptive ranking is disabled.
		ranker.recordSelection(beta);
		assertEquals(List.of(alpha, beta), ranker.rank(List.of(beta, alpha)));
	}

	@Test
	void disablingAdaptiveLearningStopsRecordingUsage() {
		TabCompletionConfig config = new TabCompletionConfig();
		JavaCompletionRanker ranker = new JavaCompletionRanker(config);
		JavaCompletion beta = new JavaCompletion(CompletionKind.TYPE, "Beta", "Beta", 30, null, "Beta", 0);

		// Disable adaptive ranking learning.
		config.setAdaptiveRankingLearningEnabled(false);

		// Even if we call "hey record this", it should not actually record anything since learning is disabled.
		ranker.recordSelection(beta);
		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
	}
}
