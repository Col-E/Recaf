package software.coley.recaf.ui.control.richtext.suggest.assembler;

import org.junit.jupiter.api.Test;
import software.coley.recaf.ui.control.richtext.suggest.TabCompletionConfig;
import software.coley.recaf.ui.control.richtext.suggest.ranking.CompletionRanker;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AssemblerCompletionRanking}.
 */
class AssemblerCompletionRankingTest {
	@Test
	void learnedOpcodeMovesAheadOfEqualOpcodes() {
		CompletionRanker<AssemblerCompletion> ranker = AssemblerCompletionRanking.create(new TabCompletionConfig());
		AssemblerCompletion iload = new AssemblerCompletion.Opcode("iload");
		AssemblerCompletion istore = new AssemblerCompletion.Opcode("istore");
		assertEquals(List.of(iload, istore), ranker.rank(List.of(istore, iload)));

		ranker.recordSelection(istore);
		assertEquals(List.of(istore, iload), ranker.rank(List.of(iload, istore)));
	}

	@Test
	void operandsAreNeverLearnedOrReordered() {
		TabCompletionConfig config = new TabCompletionConfig();
		CompletionRanker<AssemblerCompletion> ranker = AssemblerCompletionRanking.create(config);
		AssemblerCompletion v1 = new AssemblerCompletion.Operand("v1", AssemblerCompletion.OperandKind.REGISTER);
		AssemblerCompletion v2 = new AssemblerCompletion.Operand("v2", AssemblerCompletion.OperandKind.REGISTER);

		ranker.recordSelection(v2);
		assertEquals(List.of(v1, v2), ranker.rank(List.of(v2, v1)));
		assertTrue(config.getAdaptiveRankingUsageCounts().isEmpty());
	}
}
