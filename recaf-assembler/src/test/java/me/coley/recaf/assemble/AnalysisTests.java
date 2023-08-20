package me.coley.recaf.assemble;

import me.coley.recaf.assemble.analysis.Analysis;
import me.coley.recaf.assemble.analysis.Analyzer;
import me.coley.recaf.assemble.analysis.Frame;
import me.coley.recaf.assemble.analysis.Value;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.transformer.JasmToUnitTransformer;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.coley.recaf.util.Types;
import me.darknet.assembler.exceptions.AssemblerException;
import me.darknet.assembler.parser.Group;
import me.darknet.assembler.parser.ParserContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class AnalysisTests extends JasmUtils {
	@Nested
	public class Blocks {
		@Test
		public void testLinear() {
			// This isn't a complete method, but the analyzer is meant to work even in
			// incomplete situations. This will show that there is a single block.
			String code = "method static linear ()V\n" +
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
			String code = "method static ifCond (Z skip)V\n" +
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
			String code = "method static switchMethod (I value)V\n" +
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
			String code = "method static tryCatch ()V\n" +
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
					assertEquals(2, results.block(8).getEdges().size());
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTryCatchWithInsideBlocks() {
			String code = "method static tryCatch (Z flag)V\n" +
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
			String code = "method static math ()V\n" +
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
			String code = "method static hello ()V\n" +
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
		public void test2DArrayMath() {
			// int[][] array = {{1, 2, 3}, {1, 2, 3}};
			// for (int i = 0; i < array.length; i++) for (int j = 0; j < array.length; j++) array[i][j] *= array[j][i];
			String code = "method static foo ()V\n" +
					"A:\n" +
					"    iconst_2\n" +
					"    anewarray [I\n" +
					"    dup\n" +
					"    iconst_0\n" +
					"    iconst_3\n" +
					"    newarray int\n" +
					"    dup\n" +
					"    iconst_0\n" +
					"    iconst_1\n" +
					"    iastore\n" +
					"    dup\n" +
					"    iconst_1\n" +
					"    iconst_2\n" +
					"    iastore\n" +
					"    dup\n" +
					"    iconst_2\n" +
					"    iconst_3\n" +
					"    iastore\n" +
					"    aastore\n" +
					"    dup\n" +
					"    iconst_1\n" +
					"    iconst_3\n" +
					"    newarray int\n" +
					"    dup\n" +
					"    iconst_0\n" +
					"    iconst_1\n" +
					"    iastore\n" +
					"    dup\n" +
					"    iconst_1\n" +
					"    iconst_2\n" +
					"    iastore\n" +
					"    dup\n" +
					"    iconst_2\n" +
					"    iconst_3\n" +
					"    iastore\n" +
					"    aastore\n" +
					"    astore array\n" +
					"B:\n" +
					"    iconst_0\n" +
					"    istore i\n" +
					"C:\n" +
					"    iload i\n" +
					"    aload array\n" +
					"    arraylength\n" +
					"    if_icmpge I\n" +
					"D:\n" +
					"    iconst_0\n" +
					"    istore j\n" +
					"E:\n" +
					"    iload j\n" +
					"    aload array\n" +
					"    arraylength\n" +
					"    if_icmpge H\n" +
					"F:\n" +
					"    aload array\n" +
					"    iload i\n" +
					"    aaload\n" +
					"    iload j\n" +
					"    dup2\n" +
					"    iaload\n" +
					"    aload array\n" +
					"    iload j\n" +
					"    aaload\n" +
					"    iload i\n" +
					"    iaload\n" +
					"    imul\n" +
					"    iastore\n" +
					"G:\n" +
					"    iinc j 1\n" +
					"    goto E\n" +
					"H:\n" +
					"    iinc i 1\n" +
					"    goto C\n" +
					"I:\n" +
					"    return\n" +
					"J:\n" +
					"end";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				try {
					Analysis results = analyzer.analyze();

					// Assert no wonky frames
					boolean wonky = false;
					List<Frame> frames = results.getFrames();
					for (int i = 0; i < frames.size(); i++) {
						Frame frame = frames.get(i);
						if (frame.isWonky()) {
							System.err.println(i + " Wonky: " + frame.getWonkyReason());
							wonky = true;
						}
					}
					assertFalse(wonky, "Should not have any wonky frames");

					// Assert array contents
					Frame last = frames.get(frames.size() - 2);
					Value.ArrayValue value = (Value.ArrayValue) last.getLocal("array");
					Value[] array = value.getArray();
					assertEquals(Types.array(Type.INT_TYPE, 2), value.getArrayType());
					assertEquals(Type.INT_TYPE, value.getElementType());
					assertEquals(2, array.length);
				} catch (AstException ex) {
					fail(ex);
				}
			});
		}

		@Test
		public void testTypeMerge() {
			// This isn't a complete method, but the analyzer is meant to work even in incomplete situations.
			String code = "method static merge (I kind)V\n" +
					"start:\n" +
					"  iload kind\n" +
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
		public void testBackwardsTypeMergeSimple() {
			// This isn't a complete method, but the analyzer is meant to work even in incomplete situations.
			String code = "method static merge ()Ljava/lang/Object;\n" +
					"start:\n" +
					"  aconst_null\n" +
					"assign:\n" +
					"  dup\n" +
					"  astore collection\n" +
					"  ifnonnull yield\n" +
					"  new java/util/Collection\n" +
					"  goto assign\n" +
					"yield:\n" +
					"  aload collection\n" +
					"  areturn\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				analyzer.setInheritanceChecker(ReflectiveInheritanceChecker.getInstance());
				try {
					Analysis results = analyzer.analyze();
					// Assert variable is collection
					Frame first = results.getFrames().get(5);
					Frame last = results.getFrames().get(results.getFrames().size() - 1);
					Value valueFirst = first.getLocal("collection");
					Value valueLast = last.getLocal("collection");
					// Check first frame where value is assigned
					if (valueFirst instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) valueFirst;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' at assign is not an object!  Is: " + valueLast);
					}
					// Check last frame where value is used
					if (valueLast instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) valueLast;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' at end is not an object!  Is: " + valueLast);
					}
					// Assert stack is also collection at 'areturn'
					Frame mergeLabel = results.getFrames().get(results.getFrames().size() - 2);
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
		public void testBackwardsTypeMerge() {
			// This isn't a complete method, but the analyzer is meant to work even in incomplete situations.
			String code = "method static merge (I kind)Ljava/lang/Object;\n" +
					"start:\n" +
					"  aconst_null\n" +
					"assign:\n" +
					"  dup\n" +
					"  astore collection\n" +
					"  ifnonnull yield\n" +
					"  iload kind\n" +
					"switch:\n" +
					"  tableswitch 0 2 a b c default d\n" +
					"a: new java/util/List \n goto assign\n" +
					"b: new java/util/Set \n goto assign\n" +
					"c: new java/util/HashSet \n goto assign\n" +
					"d: new java/util/ArrayList \n  goto assign\n" +
					"yield:\n" +
					"  aload collection\n" +
					"  areturn\n" + "\nend";
			handle(code, unit -> {
				Analyzer analyzer = new Analyzer("Test", unit);
				analyzer.setInheritanceChecker(ReflectiveInheritanceChecker.getInstance());
				try {
					Analysis results = analyzer.analyze();
					// Assert variable is collection
					Frame first = results.getFrames().get(5);
					Frame last = results.getFrames().get(results.getFrames().size() - 1);
					Value valueFirst = first.getLocal("collection");
					Value valueLast = last.getLocal("collection");
					// Check first frame where value is assigned
					if (valueFirst instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) valueFirst;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' at assign is not an object!  Is: " + valueLast);
					}
					// Check last frame where value is used
					if (valueLast instanceof Value.ObjectValue) {
						Value.ObjectValue objectValue = (Value.ObjectValue) valueLast;
						assertEquals("java/util/Collection", objectValue.getType().getInternalName());
					} else {
						fail("var 'collection' at end is not an object!  Is: " + valueLast);
					}
					// Assert stack is also collection
					Frame mergeLabel = results.getFrames().get(results.getFrames().size() - 2);
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
			String code = "method static merge (I kind)V\n" +
					"start:\n" +
					"  iload kind\n" +
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

		@Test
		public void testNoInfiniteLoopIinc() {
			// If our frame merge conditions are too loose this will infinite loop since we always eval "i++"
			String code = "method static merge (I kind)V\n" +
					"start:\n" +
					"  iconst_0\n" +
					"  istore i\n" +
					"loop:\n" +
					"  iinc i 1\n" +
					"  goto loop\n" +
					"exit:\n" +
					"  return\n" + "\nend";
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

	@Nested
	public class Correctness {
		@Test
		public void castNullToObject() {
			handle("method dummy ()V\n" +
					"a: \n" +
					"  aconst_null\n" +
					"  checkcast java/lang/String\n" +
					"b: \n" +
					"  putstatic Test.string_field Ljava/lang/String;\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void stringAndNullMergeOk() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  iload bool\n" +
					"  ifeq c\n" +
					"b: \n" +
					"  ldc \"hello\"\n" +
					"  goto d\n" +
					"c: \n" +
					"  aconst_null\n" +
					"  goto d\n" +
					"d: \n" +
					"  putstatic Test.string_field Ljava/lang/String;\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void arrayAndNullMergeOk() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  iload bool\n" +
					"  ifeq c\n" +
					"b: \n" +
					"  getstatic Test.array_field [B\n" +
					"  goto d\n" +
					"c: \n" +
					"  aconst_null\n" +
					"  goto d\n" +
					"d: \n" +
					"  putstatic Test.array_field [B\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void nullAndNullMergeOk() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  iload bool\n" +
					"  ifeq c\n" +
					"b: \n" +
					"  aconst_null\n" +
					"  goto d\n" +
					"c: \n" +
					"  aconst_null\n" +
					"  goto d\n" +
					"d: \n" +
					"  putstatic Test.object_field Ljava/lang/Object;\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void storeNullInArrayField() {
			handle("method dummy ()V\n" +
					"a: \n" +
					"  aconst_null\n" +
					"  putstatic Test.array_field [I\n" +
					"  return\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void storeNullInObjectField() {
			handle("method dummy ()V\n" +
					"a: \n" +
					"  aconst_null\n" +
					"  putstatic Test.obj_field Ljava/lang/Object;\n" +
					"  return\n" +
					"end", AnalysisTests::correct);
		}

		@Test
		public void testObjectToArrayCheckcast() {
			handle("method dummy ()I\n" +
					"a: \n" +
					"  getstatic Test.object_field Ljava/lang/Object;\n" +
					"  checkcast [B\n" +
					"  arraylength\n" +
					"  ireturn\n" +
					"end", AnalysisTests::correct);
		}
	}

	@Nested
	public class Incorrectness {
		@Test
		public void storeIntInLongField() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  iload bool\n" +
					"  putstatic Test.long_field J\n" +
					"  return\n" +
					"end", AnalysisTests::failure);
		}

		@Test
		public void storeNullInIntField() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  aconst_null\n" +
					"  putstatic Test.int_field I\n" +
					"  return\n" +
					"end", AnalysisTests::failure);
		}

		@Test
		public void getfieldOnPrimitive() {
			handle("method dummy (Z bool)V\n" +
					"a: \n" +
					"  iload bool\n" +
					"  getfield Test.int_field I\n" +
					"  pop\n" +
					"  return\n" +
					"end", AnalysisTests::failure);
		}

		@Test
		public void getfieldOnNull() {
			handle("method dummy ()V\n" +
					"a: \n" +
					"  aconst_null\n" +
					"  getfield Test.int_field I\n" +
					"  pop\n" +
					"  return\n" +
					"end", AnalysisTests::failure);
		}

		@Test
		public void unsupportedJsr() {
			handle("method dummy ()V\n" +
					"a: \n" +
					"  jsr sub\n" +
					"  goto exit\n" +
					"sub:\n" +
					"  astore address\n" +
					"  ret address\n" +
					"exit:\n" +
					"  return\n" +
					"end", AnalysisTests::failure);
		}
	}

	private static void correct(MethodDefinition unit) {
		Analyzer analyzer = new Analyzer("Test", unit);
		try {
			Analysis results = analyzer.analyze();
			List<Frame> frames = results.getFrames();
			for (int i = 0; i < frames.size(); i++) {
				Frame frame = frames.get(i);
				assertFalse(frame.isWonky(), i + ": " + frame.getWonkyReason());
			}
		} catch (AstException ex) {
			fail(ex);
		}
	}

	private static void failure(MethodDefinition unit) {
		Analyzer analyzer = new Analyzer("Test", unit);
		try {
			Analysis results = analyzer.analyze();
			for (Frame frame : results.getFrames()) {
				if (frame.isWonky()) {
					// intended to occur
					System.err.println(frame.getWonkyReason());
					return;
				}
			}
			fail("No non-wonky frames!");
		} catch (AstException ignored) {
			// intended to occur
		}
	}

	private static void handle(String original, Consumer<MethodDefinition> handler) {
		// JASM parse
		ParserContext parser = createParser(DEFAULT_KEYWORDS, original);
		try {
			Collection<Group> groups = parser.parse();
			assertNotEquals(groups.isEmpty(), true, "Parser did not find unit context with input: " + original);

			// Transform to our AST
			JasmToUnitTransformer transformer = new JasmToUnitTransformer(groups);
			Unit unit = transformer.generateUnit();

			// Handle
			handler.accept(unit.getDefinitionAsMethod());
		} catch (AssemblerException e) {
			throw new RuntimeException(e.describe(), e);
		}
	}
}
