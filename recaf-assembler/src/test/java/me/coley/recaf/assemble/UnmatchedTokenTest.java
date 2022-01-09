package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic building block for error handling. The basic idea is that anything not matching
 * a {@link me.coley.recaf.assemble.ast.CodeEntry} will be marked as {@link me.coley.recaf.assemble.ast.Unmatched}.
 * We will then be able to handle these unmatched token sequences as raw strings, and handle
 * them any way we like.
 * <br>
 * So TLDR, this tests matching typos and minor screw-ups users would make.
 * Actually giving suggestions is out of scope here.
 */
public class UnmatchedTokenTest extends TestUtil {
	@Test
	public void testTypoInstructionName() {
		handle("name()V\n" + "ICOMST_1", unit -> {
			assertEquals(1, unit.getCode().getUnmatched().size());
			assertEquals("ICOMST_1", unit.getCode().getUnmatched().get(0).print());
		});
	}

	@Test
	public void testTypoInBetweenOkInstructions() {
		handle("name()V\n" + "ICONST_1\n" + "XCONST_1\n" + "ICONST_1\n", unit -> {
			assertEquals(1, unit.getCode().getUnmatched().size());
			assertEquals("XCONST_1", unit.getCode().getUnmatched().get(0).print());
		});
	}

	@Test
	public void testMissingArgs() {
		// NEW expects a <type> argument
		handle("name()V\n" + "NEW", unit -> {
			assertEquals(1, unit.getCode().getUnmatched().size());
			assertEquals("NEW", unit.getCode().getUnmatched().get(0).print());
		});
	}

	@Test
	public void testMissingFieldDesc() {
		handle("name()V\n" + "PUTSTATIC java/lang/String.name", unit -> {
			assertEquals(1, unit.getCode().getUnmatched().size());
			assertEquals("PUTSTATIC java/lang/String.name", unit.getCode().getUnmatched().get(0).print());
		});
	}

	private static void handle(String original, Consumer<Unit> handler) {
		BytecodeParser parser = parser(original);

		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		AntlrToAstTransformer visitor = new AntlrToAstTransformer();
		Unit unit = visitor.visitUnit(unitCtx);

		handler.accept(unit);
	}
}
