package me.coley.recaf.assemble;

import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.transformer.AntlrToAstTransformer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class AnalysisTests extends TestUtil {
	@Nested
	public class Blocks {
		@Test
		public void testLinear() {
			// This isn't a complete method, but the analyzer is meant to work even in
			// incomplete situations. This will show that there is a single block.
			String code = "static linear()V\n" +
					"a:\n" +
					"getstatic java/lang/System.out Ljava/io/PrintStream;\n" +
					"ldc \"Hello\"\n" +
					"invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V\n" +
					"b:";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(1, results.getBlocks().size());
					assertEquals(5, results.getFrames().size());
					assertEquals(5, results.block(0).getFrames().size());
					assertEquals(5, results.block(0).getInstructions().size());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testIf() {
			// Should be able to split up a simple if into 3 blocks.
			// - before
			// - inside
			// - after
			String code = "static ifCond(Z skip)V\n" +
					"a:\n" +
					"iload skip\n" +
					"ifne end\n" +
					"getstatic java/lang/System.out Ljava/io/PrintStream;\n" +
					"ldc \"Flag is true\"\n" +
					"invokevirtual java/io/PrintStream.println(Ljava/lang/String;)V\n" +
					"end:\n" +
					"nop";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(3, results.getBlocks().size());
					// First block is label + jump
					assertEquals(3, results.block(0).getFrames().size());
					// Second block is contents of the print
					assertEquals(3, results.block(3).getFrames().size());
					// Last block is the jump target and beyond
					assertEquals(2, results.block(6).getFrames().size());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testSwitch() {
			// Each switch case will just be the label and return.
			// The end will have padding no-op instructions for us to differentiate it.
			String code = "static switchMethod(I value)V\n" +
					"start:\n" +
					"iload value\n" +
					"tableswitch range(0:2) offsets(a, b, c) default(d)\n" +
					"a: return\n" +
					"b: return\n" +
					"c: return\n" +
					"d: return\n" +
					"end: nop nop nop";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(6, results.getBlocks().size());
					assertEquals(3, results.block(0).getInstructions().size());
					assertEquals(2, results.block(3).getInstructions().size());
					assertEquals(2, results.block(5).getInstructions().size());
					assertEquals(2, results.block(7).getInstructions().size());
					assertEquals(2, results.block(9).getInstructions().size());
					assertEquals(4, results.block(11).getInstructions().size());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTryCatch() {
			String code = "static tryCatch()V\n" +
					"TRY a b CATCH(*) c\n" +
					"a: nop nop nop\n" +
					"b: \n" +
					"c: astore ex\n" +
					"end: return\n";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(2, results.getBlocks().size());
					assertEquals(5, results.block(0).getInstructions().size());
					assertEquals(4, results.block(5).getInstructions().size());
					// The try block should have an edge to the handler block
					assertEquals(results.block(5), results.block(0).getEdges().get(0).getTo());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTryCatchWithInsideBlocks() {
			String code = "static tryCatch(Z flag)V\n" +
					"TRY tryStart tryEnd CATCH(*) tryHandler\n" +
					"tryStart: \n" +
					"  ifne skip\n" +
					"    nop\n" +
					"  skip: \n" +
					"tryEnd: \n" +
					"tryHandler: \n" +
					"  astore ex\n" +
					"  end: \n" +
					"  return\n";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(4, results.getBlocks().size());
					// The try block should have an edge to the handler block
					//  - There are 3 blocks inside the try-block
					assertEquals(results.block(5), results.block(0).getEdges().get(0).getTo());
					assertEquals(results.block(5), results.block(2).getEdges().get(0).getTo());
					assertEquals(results.block(5), results.block(3).getEdges().get(0).getTo());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}
	}

	private static void handle(String original, Consumer<Unit> handler) {
		// ANTLR parse
		BytecodeParser parser = parser(original);
		BytecodeParser.UnitContext unitCtx = parser.unit();
		assertNotNull(unitCtx, "Parser did not find unit context with input: " + original);

		// Transform to our AST
		AntlrToAstTransformer visitor = new AntlrToAstTransformer();
		Unit unit = visitor.visitUnit(unitCtx);

		// Handle
		handler.accept(unit);
	}
}
