package me.coley.recaf.assemble;

import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.transformer.JasmToAstTransformer;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.darknet.assembler.parser.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.ParserContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class AnalysisTests extends TestUtil {
	@Nested
	public class Blocks {
		@Test
		public void testLinear() {
			// This isn't a complete method, but the analyzer is meant to work even in
			// incomplete situations. This will show that there is a single block.
			String code = "method .static linear ()V\n" +
					"a:\n" +
					"getstatic java/lang/System.out Ljava/io/PrintStream;\n" +
					"ldc \"Hello\"\n" +
					"invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V\n" +
					"b:" + "\nend";
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
			String code = "method .static ifCond (Z skip)V\n" +
					"a:\n" +
					"iload skip\n" +
					"ifne end\n" +
					"getstatic java/lang/System.out Ljava/io/PrintStream;\n" +
					"ldc \"Flag is true\"\n" +
					"invokevirtual java/io/PrintStream.printlnc (Ljava/lang/String;)V\n" +
					"end:\n" +
					"nop" + "\nend";
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
			String code = "method .static switchMethod (I value)V\n" +
					"start:\n" +
					"  iload value\n" +
					"  tableswitch 0 2 a b c default d \n" +
					"a: return\n" +
					"b: return\n" +
					"c: return\n" +
					"d: return\n" +
					"end: nop nop nop" + "\nend";
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
			String code = "method .static tryCatch ()V\n" +
					"catch * a b c\n" +
					"a: nop nop nop\n" +
					"b: goto end\n" +
					"c: astore ex\n" +
					"end: return\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(3, results.getBlocks().size());
					assertEquals(6, results.block(0).getInstructions().size()); // try block
					assertEquals(2, results.block(6).getInstructions().size()); // handler block
					assertEquals(2, results.block(8).getInstructions().size()); // end block
					// The try block should have an edge to the handler block
					assertEquals(results.block(6), results.block(0).getEdges().get(0).getTo());
					// End block is terminal and has no edges (aside from natural flow into it)
					assertEquals(0, results.block(8).getEdges().size());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTryCatchWithInsideBlocks() {
			String code = "method .static tryCatch (Z flag)V\n" +
					"catch * tryStart tryEnd tryHandler\n" +
					"tryStart: \n" +
					"  ifne skip\n" +
					"    nop\n" +
					"  skip: \n" +
					"tryEnd: \n" +
					"  goto end\n" +
					"tryHandler: \n" +
					"  astore ex\n" +
					"end: \n" +
					"  return\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					assertEquals(5, results.getBlocks().size());
					// The try block should have an edge to the handler block
					//  - There are 3 blocks inside the try-block
					assertEquals(results.block(6), results.block(0).getEdges().get(0).getTo());
					assertEquals(results.block(6), results.block(2).getEdges().get(0).getTo());
					assertEquals(results.block(6), results.block(3).getEdges().get(0).getTo());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}
	}

	@Nested
	public class Frames {
		@Test
		public void testMath() {
			// This isn't a complete method, but the analyzer is meant to work even in incomplete situations.
			String code = "method .static math ()V\n" +
					"a:\n" +
					"iconst_1\n" +
					"ldc 2\n" +
					"iadd\n" + // 1 + 2 = 3
					"bipush 3\n" +
					"imul\n" + // 3 * 3 = 9
					"i2f\n" +
					"fconst_2\n" +
					"swap\n" +
					"fdiv\n" + // 9.0 / 2 = 4.5
					"ldc 0.5f\n" +
					"fadd\n" +  // 4.5 + 0.5 = 5
					"f2i\n" +
					"iconst_1\n" +
					"swap\n" +
					"ishl" + "\nend";  // 5 << 1 = 5 * 2 = 10
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					Frame last = results.getFrames().get(results.getFrames().size() - 1);
					Value.NumericValue value = (Value.NumericValue) last.peek();
					assertEquals(10, value.getNumber());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testArrayHello() {
			String code = "method .static hello ()V\n" +
					"a:\n" +
					// new array[5]
					"iconst_5\n" +
					"newarray byte\n" +
					"astore array\n" +
					// array[0] = 0x48
					"aload array\n" +
					"bipush 0\n" +
					"bipush 0x48\n" +
					"bastore\n" +
					// array[1] = 0x65
					"aload array\n" +
					"bipush 1\n" +
					"bipush 0x65\n" +
					"bastore\n" +
					// array[2] = 0x6c
					"aload array\n" +
					"bipush 2\n" +
					"bipush 0x6c\n" +
					"bastore\n" +
					// array[3] = 0x6c
					"aload array\n" +
					"bipush 3\n" +
					"bipush 0x6c\n" +
					"bastore\n" +
					// array[4] = 0x6f
					"aload array\n" +
					"bipush 2\n" +
					"bipush 0x6f\n" +
					"bastore\n" +
					// text = new String(array)
					"new java/lang/String\n" +
					"dup\n" +
					"invokespecial java/lang/String.<init> ([B)V\n" +
					"astore text\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();
					Frame last = results.getFrames().get(results.getFrames().size() - 1);
					Value.ArrayValue value = (Value.ArrayValue) last.getLocal("array");
					//
					Value[] array = value.getArray();
					assertEquals(Type.BYTE_TYPE, value.getElementType());
					assertEquals(5, array.length);
					// TODO: When tracking strings, ensure 'text' is 'Hello'
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTypeMerge() {
			// This isn't a complete method, but the analyzer is meant to work even in incomplete situations.
			String code = "method .static merge (I type)V\n" +
					"start:\n" +
					"  iload type\n" +
					"  tableswitch 0 2 a b c default d\n" +
					"a: new java/util/List \n goto merge\n" +
					"b: new java/util/Set \n goto merge\n" +
					"c: new java/util/HashSet \n goto merge\n" +
					"d: new java/util/ArrayList \n goto merge\n" +
					"merge:\n" +
					"  astore collection\n" +
					"  nop\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				analyzer.setInheritanceChecker(ReflectiveInheritanceChecker.getInstance());
				try {
					Analysis results = analyzer.analyze();
					// Assert variable is collection
					Frame last = results.getFrames().get(results.getFrames().size() - 1);
					Value value = last.getLocal("collection");
					if (value instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) value;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' not an object!");
					}
					// Assert stack is also collection
					Frame mergeLabel = results.getFrames().get(results.getFrames().size() - 3);
					Value mergeLabelStack = mergeLabel.peek();
					if (mergeLabelStack instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) mergeLabelStack;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' not an object!");
					}
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testNoInfiniteLoop() {
			// If our branch follow conditions are too loose this will infinite loop
			String code = "method .static merge (I type)V\n" +
					"start:\n" +
					"  iload type\n" +
					"  tableswitch 0 2 a b c default d\n" +
					"a: \n goto start\n" +
					"b: \n goto start\n" +
					"c: \n goto start\n" +
					"d: \n goto merge\n" +
					"merge:\n" +
					"  nop\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				analyzer.setInheritanceChecker(ReflectiveInheritanceChecker.getInstance());
				try {
					// Will not finish if we loop to death
					assertNotNull(analyzer.analyze());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}
	}

	private static void handle(String original, Consumer<MethodDefinition> handler) {
		// JASM parse
		ParserContext parser = parser(original);
		try {
			Collection<Group> groups = parser.parse();
			assertNotEquals(groups.isEmpty(), true, "Parser did not find unit context with input: " + original);

			// Transform to our AST
			JasmToAstTransformer transformer = new JasmToAstTransformer(groups);
			Unit unit = transformer.generateUnit();

			// Handle
			handler.accept(unit.getMethod());
		} catch (AssemblerException e) {
			throw new RuntimeException(e.describe(), e);
		}
	}
}
