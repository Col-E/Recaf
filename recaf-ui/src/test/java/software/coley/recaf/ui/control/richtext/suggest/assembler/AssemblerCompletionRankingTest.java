package software.coley.recaf.ui.control.richtext.suggest.assembler;

import org.junit.jupiter.api.Test;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AssemblerCompletionRanking}.
 */
class AssemblerCompletionRankingTest {
	@Test
	void learnedOpcodeMovesAheadOfEqualOpcodes() {
		// Create a ranker, record a selection of an opcode.
		CompletionRanker<AssemblerCompletion> ranker = AssemblerCompletionRanking.create(new TabCompletionConfig());
		AssemblerCompletion iload = new AssemblerCompletion.Opcode("iload");
		AssemblerCompletion istore = new AssemblerCompletion.Opcode("istore");

		// Before recording a selection, the order should be the same as the input.
		// After recording a selection, the selected opcode should move ahead of the other.
		assertEquals(List.of(iload, istore), ranker.rank(List.of(istore, iload)));
		ranker.recordSelection(istore);
		assertEquals(List.of(istore, iload), ranker.rank(List.of(iload, istore)));
	}

	@Test
	void operandsAreNeverLearnedOrReordered() {
		TabCompletionConfig config = new TabCompletionConfig();

		// Create two operands, record a selection of one, and then rank them.
		CompletionRanker<AssemblerCompletion> ranker = AssemblerCompletionRanking.create(config);
		AssemblerCompletion v1 = new AssemblerCompletion.Operand("v1", AssemblerCompletion.OperandKind.REGISTER);
		AssemblerCompletion v2 = new AssemblerCompletion.Operand("v2", AssemblerCompletion.OperandKind.REGISTER);
		ranker.recordSelection(v2);

		// The order should not change. Indexing register/variable names isn't a thing, so we don't want to learn them.
		assertEquals(List.of(v1, v2), ranker.rank(List.of(v2, v1)));
		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
	}

	@Test
	void strongerTypeMatchRanksAheadOfWeakerMatch() {
		// Create two type completions, one with a direct match and one with a camel-hump match.
		ClassPathNode path = mock(ClassPathNode.class);
		AssemblerCompletion.Type direct = new AssemblerCompletion.Type(path, "java/lang/String", false, 0);
		AssemblerCompletion.Type camelHump = new AssemblerCompletion.Type(path, "java/lang/StringBuilder", true, 2);

		// Regardless of whether adaptive ranking is enabled,
		// the direct match should always rank ahead of the camel-hump match.
		for (boolean adaptiveRanking : List.of(true, false)) {
			TabCompletionConfig config = new TabCompletionConfig();
			config.setAdaptiveRankingEnabled(adaptiveRanking);
			CompletionRanker<AssemblerCompletion> ranker = AssemblerCompletionRanking.create(config);
			assertEquals(List.of(direct, camelHump), ranker.rank(List.of(camelHump, direct)));
		}
	}
}
