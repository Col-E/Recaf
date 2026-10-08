package software.coley.recaf.ui.control.richtext.suggest.java;

import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link JavaCompletionRanking}.
 */
class JavaCompletionRankingTest {
	@Test
	void kindAwareLearning() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<JavaCompletion> ranker = JavaCompletionRanking.create(config);
		JavaCompletion method = new JavaCompletion(CompletionKind.METHOD, "zeta", "zeta", 30, null, "zeta", 0);
		JavaCompletion field = new JavaCompletion(CompletionKind.FIELD, "zeta", "zeta", 30, null, "zeta", 0);

		assertEquals(List.of(method, field), ranker.rank(List.of(method, field)));
		for (int i = 0; i < 8; i++)
			ranker.recordSelection(field);
		assertEquals(List.of(field, method), ranker.rank(List.of(method, field)));
	}

	@Test
	void typeBaselineOrdersStringFirst() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<JavaCompletion> ranker = JavaCompletionRanking.create(config);
		JavaCompletion alpha = new JavaCompletion(CompletionKind.TYPE, "Alpha", "Alpha", 30, null, "Alpha", 0);
		JavaCompletion string = new JavaCompletion(CompletionKind.TYPE, "String", "String", 30, null, "String", 0);

		assertEquals(List.of(string, alpha), ranker.rank(List.of(alpha, string)));
	}

	@Test
	void adaptiveDisabledKeepsStructuralOrder() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<JavaCompletion> ranker = JavaCompletionRanking.create(config);
		JavaCompletion alpha = new JavaCompletion(CompletionKind.TYPE, "Alpha", "Alpha", 30, null, "Alpha", 0);
		JavaCompletion string = new JavaCompletion(CompletionKind.TYPE, "String", "String", 30, null, "String", 0);
		config.setAdaptiveRankingEnabled(false);

		assertEquals(List.of(alpha, string), ranker.rank(List.of(alpha, string)));
	}
}
