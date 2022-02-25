package me.coley.recaf;

import me.coley.recaf.parse.bytecode.Parse;
import me.coley.recaf.parse.bytecode.ParseResult;
import me.coley.recaf.parse.bytecode.exception.ASTParseException;
import me.coley.recaf.parse.bytecode.ast.*;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.RegexUtil;
import me.coley.recaf.util.TypeUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the bytecode AST and associated systems <i>(Assembler)</i>.
 *
 * @author Matt
 */
public class AssemblyAstTest {
	@Nested
	public class Format {
		@Test
		public void testParseComment() {
			String msg = "Hello world";
			String line = "//" + msg;
			CommentAST ast = single(line);
			assertEquals(msg, ast.getComment());
			assertEquals(line, ast.print());
		}

		@Test
		public void testSignature() {
			String pre = "SIGNATURE ";
			String sig = "Ljava/util/Set<Ljava/lang/String;>;";
			SignatureAST ast = single(pre + sig);
			assertEquals(sig, ast.getSignature());
			assertEquals(pre + sig, ast.print());
		}

		@Test
		public void testAlias() {
			RootAST root = Parse.parse(
					"ALIAS hello \"\"Hello World\"\"\n" +
					"LDC ${hello}").getRoot();
			LdcInsnAST ldc = (LdcInsnAST) root.getChildren().get(1);
			assertEquals("LDC \"Hello World\"", ldc.print());
		}

		@Test
		public void testAliasInAlias() {
			RootAST root = Parse.parse(
					"ALIAS one \"World\"\n" +
					"ALIAS two \"\"Hello ${one}\"\"\n" +
					"LDC ${two}").getRoot();
			LdcInsnAST ldc = (LdcInsnAST) root.getChildren().get(2);
			assertEquals("LDC \"Hello World\"", ldc.print());
		}

		@Test
		public void testParseCommentAfterNewline() {
			String msg = "test";
			String line = "//" + msg;
			CommentAST ast = single("\n" + line);
			assertEquals(msg, ast.getComment());
			assertEquals(line, ast.print());
		}

		@Test
		public void testParseLabel() {
			String name = "LABEL";
			String line = name + ":";
			LabelAST ast = single(line);
			assertEquals(name, ast.getName().getName());
			assertEquals(line, ast.print());
		}

		@Test
		public void testParseThrows() {
			String type = "java/lang/Exception";
			String line = "THROWS " + type;
			ThrowsAST ast = single(line);
			assertEquals(type, ast.getType().getType());
			assertEquals(line, ast.print());
		}

		@Test
		public void testMethodDefine() {
			MethodDefinitionAST def = single("DEFINE public static main([Ljava/lang/String; args)V");
			assertEquals("main", def.getName().getName());
			assertEquals(2, def.getModifiers().size());
			assertEquals("public", def.getModifiers().get(0).getName());
			assertEquals("static", def.getModifiers().get(1).getName());
			assertEquals(1, def.getArguments().size());
			assertEquals("[Ljava/lang/String;", def.getArguments().get(0).getDesc().getDesc());
			assertEquals("args", def.getArguments().get(0).getVariableName().getName());
			assertEquals("V", def.getReturnType().getDesc());
		}


		@Test
		public void testFieldDefine() {
			FieldDefinitionAST def = single("DEFINE public static Ljava/util/List; myList");
			assertEquals("myList", def.getName().getName());
			assertEquals(2, def.getModifiers().size());
			assertEquals("public", def.getModifiers().get(0).getName());
			assertEquals("static", def.getModifiers().get(1).getName());
			assertEquals("Ljava/util/List;", def.getType().getDesc());
		}

		@Test
		public void testMethodDefineNoModifiers() {
			MethodDefinitionAST def = single("DEFINE func(Ljava/lang/String; s)V");
			assertEquals("func", def.getName().getName());
			assertEquals(0, def.getModifiers().size());
		}

		@Test
		public void testFieldDefineNoModifiers() {
			FieldDefinitionAST def = single("DEFINE Ljava/util/List; myList");
			assertEquals("myList", def.getName().getName());
			assertEquals("Ljava/util/List;", def.getType().getDesc());
			assertEquals(0, def.getModifiers().size());
		}

		@Test
		public void testFieldDefinePrimitive() {
			FieldDefinitionAST def = single("DEFINE J myLong");
			assertEquals("myLong", def.getName().getName());
			assertEquals("J", def.getType().getDesc());
			assertEquals(0, def.getModifiers().size());
		}

		@Test
		public void testMethodDefineNoArgs() {
			MethodDefinitionAST def = single("DEFINE func()V");
			assertEquals("func", def.getName().getName());
			assertEquals(0, def.getArguments().size());
		}

		@Test
		public void testMethodDefineMultipleArgs() {
			MethodDefinitionAST def = single("DEFINE func(I int1, I int2)V");
			assertEquals("func", def.getName().getName());
			assertEquals(2, def.getArguments().size());
			assertEquals("I", def.getArguments().get(0).getDesc().getDesc());
			assertEquals("int1", def.getArguments().get(0).getVariableName().getName());
			assertEquals("I", def.getArguments().get(1).getDesc().getDesc());
			assertEquals("int2", def.getArguments().get(1).getVariableName().getName());
		}


		@Test
		public void testTryCatch() {
			TryCatchAST def = single("TRY start end CATCH(java/lang/Exception) handler");
			assertEquals("start", def.getLblStart().getName());
			assertEquals("end", def.getLblEnd().getName());
			assertEquals("handler", def.getLblHandler().getName());
			assertEquals("java/lang/Exception", def.getType().getType());
		}


		@Test
		public void testPrevNextLinkage() {
			String line = "//a\n//b\n//c";
			RootAST root = Parse.parse(line).getRoot();
			assertEquals(root.getChildren().get(0), root.getChildren().get(1).getPrev());
			assertEquals(root.getChildren().get(1), root.getChildren().get(2).getPrev());
			assertEquals(root.getChildren().get(1), root.getChildren().get(0).getNext());
			assertEquals(root.getChildren().get(2), root.getChildren().get(1).getNext());
			assertEquals(line, root.print());
		}

		@Test
		public void testInsn() {
			RootAST root = Parse.parse("ACONST_NULL\nARETURN").getRoot();
			assertEquals("ACONST_NULL", root.getChildren().get(0).print());
			assertEquals("ARETURN", root.getChildren().get(1).print());
		}

		@Test
		public void testIntInsn() {
			String text = "BIPUSH 5";
			IntInsnAST iiAst = single(text);
			assertEquals(text, iiAst.print());
			assertEquals("BIPUSH", iiAst.getOpcode().print());
			assertEquals(5, iiAst.getValue().getIntValue());
		}

		@Test
		public void testVarInsn() {
			String text = "ILOAD i";
			VarInsnAST iiAst = single(text);
			assertEquals(text, iiAst.print());
			assertEquals("ILOAD", iiAst.getOpcode().print());
			assertEquals("i", iiAst.getVariableName().getName());
		}

		@Test
		public void testTypeInsn() {
			String text = "NEW java/lang/String";
			TypeInsnAST tiAST = single(text);
			assertEquals(text, tiAST.print());
			assertEquals("NEW", tiAST.getOpcode().print());
			assertEquals("java/lang/String", tiAST.getType().getType());
		}

		@Test
		public void testIincInsn() {
			String text = "IINC i 2";
			IincInsnAST tiAST = single(text);
			assertEquals(text, tiAST.print());
			assertEquals("IINC", tiAST.getOpcode().print());
			assertEquals("i", tiAST.getVariableName().getName());
			assertEquals(2, tiAST.getIncrement().getIntValue());
		}

		@Test
		public void testLineInsn() {
			String text = "LINE lbl 2";
			LineInsnAST lineAST = single(text);
			assertEquals(text, lineAST.print());
			assertEquals("LINE", lineAST.getOpcode().print());
			assertEquals("lbl", lineAST.getLabel().getName());
			assertEquals(2, lineAST.getLineNumber().getIntValue());
		}

		@Test
		public void testNewArray() {
			String text = "NEWARRAY I";
			IntInsnAST arrayAST = single(text);
			assertEquals(text, arrayAST.print());
			assertEquals(TypeUtil.typeToNewArrayArg(Type.INT_TYPE), arrayAST.getValue().getIntValue());
		}

		@Test
		public void testMultiANewArrayInsn() {
			String text = "MULTIANEWARRAY [[Ljava/lang/String; 2";
			MultiArrayInsnAST arrayAST = single(text);
			assertEquals(text, arrayAST.print());
			assertEquals("MULTIANEWARRAY", arrayAST.getOpcode().print());
			assertEquals("[[Ljava/lang/String;", arrayAST.getDesc().getDesc());
			assertEquals(2, arrayAST.getDimensions().getIntValue());
		}

		@Test
		public void testFieldInsn() {
			String text = "GETSTATIC java/lang/System.out Ljava/io/PrintStream;";
			FieldInsnAST fieldAST = single(text);
			assertEquals(text, fieldAST.print());
			assertEquals("GETSTATIC", fieldAST.getOpcode().print());
			assertEquals("java/lang/System", fieldAST.getOwner().getType());
			assertEquals("out", fieldAST.getName().getName());
			assertEquals("Ljava/io/PrintStream;", fieldAST.getDesc().getDesc());
		}

		@Test
		public void testMethodInsn() {
			String text = "INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V";
			MethodInsnAST methodAST = single(text);
			assertEquals(text, methodAST.print());
			assertEquals("INVOKEVIRTUAL", methodAST.getOpcode().print());
			assertEquals("java/io/PrintStream", methodAST.getOwner().getType());
			assertEquals("println", methodAST.getName().getName());
			assertEquals("(Ljava/lang/String;)V", methodAST.getDesc().getDesc());
		}

		@Test
		public void testMethodInsnArrayReturn() {
			String text = "INVOKEVIRTUAL java/lang/String.getBytes()[B";
			MethodInsnAST methodAST = single(text);
			assertEquals(text, methodAST.print());
			assertEquals("INVOKEVIRTUAL", methodAST.getOpcode().print());
			assertEquals("java/lang/String", methodAST.getOwner().getType());
			assertEquals("getBytes", methodAST.getName().getName());
			assertEquals("()[B", methodAST.getDesc().getDesc());
		}

		@Test
		public void testLdcInsn() {
			// int
			String text = "LDC 1";
			LdcInsnAST ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("LDC", ldc.getOpcode().print());
			assertEquals(1, ((NumberAST) ldc.getContent()).getIntValue());
			// negative int
			text = "LDC -10";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(-10, ((NumberAST) ldc.getContent()).getIntValue());
			// long
			text = "LDC 10000000000L";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(10000000000L, ((NumberAST) ldc.getContent()).getLongValue());
			// negative long
			text = "LDC -10000000000L";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(-10000000000L, ((NumberAST) ldc.getContent()).getLongValue());
			// double
			text = "LDC 2.5";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(2.5, ((NumberAST) ldc.getContent()).getDoubleValue());
			// negative double
			text = "LDC -2.5";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(-2.5, ((NumberAST) ldc.getContent()).getDoubleValue());
			// float
			text = "LDC 2.6F";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(2.6F, ((NumberAST) ldc.getContent()).getFloatValue());
			// negative float
			text = "LDC -2.6F";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals(-2.6F, ((NumberAST) ldc.getContent()).getFloatValue());
			// string
			text = "LDC \"text\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("text", ((StringAST) ldc.getContent()).getValue());
			// empty string
			text = "LDC \"\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("", ((StringAST) ldc.getContent()).getValue());
			// newline string
			text = "LDC \"" + EscapeUtil.escape("\n") + "\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("\\n", ((StringAST) ldc.getContent()).getValue());
			assertEquals("\n", ((StringAST) ldc.getContent()).getUnescapedValue());
			// tab string
			text = "LDC \"" + EscapeUtil.escape("\t") + "\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("\\t", ((StringAST) ldc.getContent()).getValue());
			assertEquals("\t", ((StringAST) ldc.getContent()).getUnescapedValue());
			text = "LDC \"\t\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("\t", ((StringAST) ldc.getContent()).getValue());
			assertEquals("\t", ((StringAST) ldc.getContent()).getUnescapedValue());
			// Null terminator string, because people (obfuscators) are mean
			text = "LDC \"\\u0000\"";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("\\u0000", ((StringAST) ldc.getContent()).getUnescapedValue());
			// Unicode - escaped
			text = "LDC \"\\u4E0B\\u96E8\\u4E86\"";
			ldc = single(text);
			assertEquals("\\u4E0B\\u96E8\\u4E86", ((StringAST) ldc.getContent()).getValue());
			assertEquals("下雨了", ((StringAST) ldc.getContent()).getUnescapedValue());
			assertEquals(text, ldc.print());
			// Unicode - unescaped
			text = "LDC \"下雨了\"";
			ldc = single(text);
			assertEquals("下雨了", ((StringAST) ldc.getContent()).getValue());
			assertEquals("下雨了", ((StringAST) ldc.getContent()).getUnescapedValue());
			assertEquals(text, ldc.print());
			// Unicode - but not actually unicode. Its close but not exact so it shouldn't match.
			text = "LDC \"\\u048\"";
			ldc = single(text);
			assertEquals("\\u048", ((StringAST) ldc.getContent()).getValue());
			assertEquals("\\u048", ((StringAST) ldc.getContent()).getUnescapedValue());
			assertEquals(text, ldc.print());
			// Windows path url
			text = "LDC \"" + EscapeUtil.escapeCommon("C:\\example\\recaf.jar") + "\"";
			ldc = single(text);
			assertEquals("C:\\\\example\\\\recaf.jar", ((StringAST) ldc.getContent()).getValue());
			assertEquals("C:\\example\\recaf.jar", ((StringAST) ldc.getContent()).getUnescapedValue());
			assertEquals(text, ldc.print());
			// type
			text = "LDC Ljava/lang/String;";
			ldc = single(text);
			assertEquals(text, ldc.print());
			assertEquals("Ljava/lang/String;", ((DescAST) ldc.getContent()).getDesc());
		}

		@Test
		public void testTableSwitchInsn() {
			String text = "TABLESWITCH range[0:2] offsets[A, B, C] default[D]";
			TableSwitchInsnAST tbl = single(text);
			assertEquals(text, tbl.print());
			assertEquals("TABLESWITCH", tbl.getOpcode().print());
			assertEquals("D", tbl.getDfltLabel().print());
			assertEquals(0, tbl.getRangeMin().getIntValue());
			assertEquals(2, tbl.getRangeMax().getIntValue());
			assertEquals(3, tbl.getLabels().size());
		}

		@Test
		public void testLookupSwitchInsn() {
			String text = "LOOKUPSWITCH mapping[0=A, 1=B, 2=C] default[D]";
			LookupSwitchInsnAST tbl = single(text);
			assertEquals(text, tbl.print());
			assertEquals("LOOKUPSWITCH", tbl.getOpcode().print());
			assertEquals("D", tbl.getDfltLabel().print());
			assertEquals(3, tbl.getMapping().size());
		}

		@Test
		public void testEmptyLookupSwitchInsn() {
			String text = "LOOKUPSWITCH mapping[] default[D]";
			LookupSwitchInsnAST tbl = single(text);
			assertEquals(text, tbl.print());
			assertEquals("LOOKUPSWITCH", tbl.getOpcode().print());
			assertEquals("D", tbl.getDfltLabel().print());
			assertEquals(0, tbl.getMapping().size());
		}

		@Test
		public void testInvokeDynamic() {
			String text = "INVOKEDYNAMIC handle (Lgame/SnakeController;)Ljavafx/event/EventHandler; "
					+ H_META + " args[handle[H_INVOKESTATIC game/FxMain" +
					".lambda$start$0(Lgame/SnakeController;Ljavafx/scene/input/KeyEvent;)V], " +
					"(Ljavafx/event/Event;)V, (Ljavafx/scene/input/KeyEvent;)V]";
			InvokeDynamicAST indy = single(text);
			assertEquals(text, indy.print());
			assertEquals("INVOKEDYNAMIC", indy.getOpcode().print());
			assertEquals("handle", indy.getName().getName());
			assertEquals("(Lgame/SnakeController;)Ljavafx/event/EventHandler;", indy.getDesc().getDesc());

			 text = "INVOKEDYNAMIC handle (Lgame/SnakeController;)Ljavafx/event/EventHandler; "
					+ H_META + " args[-1]";
			 indy = single(text);
			assertEquals(text, indy.print());
		}

		@Test
		public void testInvokeDynamicHandleWithArrType() {
			String text = "INVOKEDYNAMIC apply ([LString;)" +
					"LIntFunction; handle[H_INVOKESTATIC Meta.factory(LLookup;LString;LMethodType;LMethodType;LMethodHandle;LMethodType;)LCallSite;] " +
					"args[" +
					"handle[H_INVOKESTATIC TextBlockLiteralExpr.stripIndent([LString;I)LPair;]" +
					"]";
			InvokeDynamicAST indy = single(text);
			assertEquals(text, indy.print());
			HandleAST handle = (HandleAST) indy.getArgs().get(0);
			assertEquals("H_INVOKESTATIC", handle.getTag().getName());
			assertEquals("TextBlockLiteralExpr", handle.getOwner().getType());
			assertEquals("stripIndent", handle.getName().getName());
			assertEquals("([LString;I)LPair;", handle.getDesc().getDesc());
		}

		@Test
		public void testInvokeDynamicNoArgs() {
			String text = "INVOKEDYNAMIC y (IJ)Ljava/lang/String; handle[H_INVOKESTATIC " +
					"com/example.call(Ljava/lang/invoke/MethodHandles$Lookup;" +
					"Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;] " +
					"args[]";
			InvokeDynamicAST indy = single(text);
			assertEquals(text, indy.print());
		}

		@Test
		public void testInvokeDynamicStringWithCommaArgs() {
			String text = "INVOKEDYNAMIC y (IJ)Ljava/lang/String; handle[H_INVOKESTATIC " +
					"com/example.call(Ljava/lang/invoke/MethodHandles$Lookup;" +
					"Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;] " +
					"args[\",aaaa\", \"bb,bb\", \"cccc,\", 0]";
			InvokeDynamicAST indy = single(text);
			assertEquals(text, indy.print());
		}
	}

	@Nested
	public class Suggestions {
		@Test
		public void testTypeInsnSuggest() {
			List<String> suggestions = suggest(null, "NEW java/lang/Stri");
			assertTrue(suggestions.contains("java/lang/String"));
		}

		@Test
		public void testMultiANewArrayInsnSuggest() {
			List<String> suggestions = suggest(null, "MULTIANEWARRAY [Ljava/lang/Stri");
			assertTrue(suggestions.contains("[Ljava/lang/String;"));
		}

		@Test
		public void testTypeSuggestFromField() {
			List<String> suggestions = suggest(null, "GETSTATIC java/lang/Syst");
			assertTrue(suggestions.contains("java/lang/System"));
		}

		@Test
		public void testMemberSuggestFromField() {
			List<String> suggestions = suggest(null, "GETSTATIC java/lang/System.ou");
			assertTrue(suggestions.contains("out Ljava/io/PrintStream;"));
		}

		@Test
		public void testTypeSuggestFromMethod() {
			List<String> suggestions = suggest(null, "INVOKESTATIC java/lang/Syst");
			assertTrue(suggestions.contains("java/lang/System"));
		}

		@Test
		public void testMemberSuggestFromMethod() {
			List<String> suggestions = suggest(null, "INVOKESTATIC java/io/PrintStream.printl");
			assertTrue(suggestions.contains("println(Ljava/lang/String;)V"));
		}

		@Test
		public void testVariableSuggestFromInsns() {
			ParseResult<RootAST> ast = Parse.parse("ISTORE example\nISTORE other\nISTORE also");
			List<String> suggestions = suggest(ast, "ILOAD ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testVariableSuggestFromDefinition() {
			ParseResult<RootAST> ast = Parse.parse("DEFINE public static main([Ljava/lang/String; args)V");
			List<String> suggestions = suggest(ast, "ALOAD a");
			assertTrue(suggestions.contains("args"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testLabelSuggestJump() {
			ParseResult<RootAST> ast = Parse.parse("example:\nother:");
			List<String> suggestions = suggest(ast, "GOTO ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testLabelSuggestSwitch() {
			ParseResult<RootAST> ast = Parse.parse("example:\nother:");
			List<String> suggestions = suggest(ast, "TABLESWITCH range[0-2] offsets[A, B, C] default[ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
			//
			suggestions = suggest(ast, "TABLESWITCH range[0-2] offsets[A, B, ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testLookupSwitchInsn() {
			ParseResult<RootAST> ast = Parse.parse("example:\nother:");
			List<String> suggestions = suggest(ast, "LOOKUPSWITCH mapping[0=A, 1=B, 2=C] default[ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
			//
			ast = Parse.parse("example:\nother:");
			suggestions = suggest(ast, "LOOKUPSWITCH mapping[0=A, 1=B, 2=ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testCatchType() {
			List<String> suggestions = suggest(null, "TRY start end CATCH(java/lang/Exce");
			assertTrue(suggestions.contains("java/lang/Exception"));
		}

		@Test
		public void testLabelSuggestLine() {
			ParseResult<RootAST> ast = Parse.parse("example:\nother:");
			List<String> suggestions = suggest(ast, "LINE ex");
			assertTrue(suggestions.contains("example"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testIndySuggestHandleTag() {
			List<String> suggestions = suggest(null, "INVOKEDYNAMIC name ()V handle[H_GET");
			assertTrue(suggestions.contains("H_GETSTATIC"));
			assertTrue(suggestions.contains("H_GETFIELD"));
			assertEquals(2, suggestions.size());
		}

		@Test
		public void testIndySuggestHandleField() {
			List<String> suggestions = suggest(null, "INVOKEDYNAMIC name ()V handle[H_GETSTATIC java/lang/System.ou");
			assertTrue(suggestions.contains("out Ljava/io/PrintStream;"));
			assertEquals(1, suggestions.size());
		}

		@Test
		public void testIndySuggestHandleMethod() {
			List<String> suggestions = suggest(null, "INVOKEDYNAMIC name ()V handle[H_INVOKEVIRTUAL java/io/PrintStream.printl");
			assertTrue(suggestions.contains("println(Ljava/lang/String;)V"));
		}


		@Test
		public void testDoesNotSuggestAlreadyTyped() {
			ParseResult<RootAST> ast = Parse.parse("example:\nother:");
			List<String> suggestions = suggest(ast, "GOTO example");
			assertFalse(suggestions.contains("example"));
			assertTrue(suggestions.isEmpty());
		}
	}

	@Nested
	public class Errors {
		@Test
		public void testBadInsn() {
			// Not real opcode name: "ACONST_OOF"
			assertErrors(() -> Parse.parse("ACONST_OOF"));
			assertErrors(() -> Parse.parse("IRETURN\nACONST_OOF\nIRETURN "));
		}

		@Test
		public void testParseBadThrows() {
			assertErrors(() -> Parse.parse("THROWS"));
			assertErrors(() -> Parse.parse("THROWS "));
			assertErrors(() -> Parse.parse("THROWS  "));
			assertErrors(() -> Parse.parse("THROWS\t"));
		}

		@Test
		public void testParseBadType() {
			assertErrors(() -> Parse.parse("THROWS no spaces allowed"));
			assertErrors(() -> Parse.parse("THROWS no_;_allowed"));
			assertErrors(() -> Parse.parse("THROWS Ljava/lang/String;"));
		}

		@Test
		public void testParseBadDefine() {
			assertErrors(() -> Parse.parse("DEFINE "));
			assertErrors(() -> Parse.parse("DEFINE ()V"));
			assertErrors(() -> Parse.parse("DEFINE (I arg)V"));
			assertErrors(() -> Parse.parse("DEFINE name()"));
			assertErrors(() -> Parse.parse("DEFINE notstatic name()V"));
			assertErrors(() -> Parse.parse("DEFINE name(NotDesc arg)V"));
			assertErrors(() -> Parse.parse("DEFINE name(LDesc;)V"));
		}

		@Test
		public void testBadTryCatch() {
			// Missing catch type
			assertErrors(() -> Parse.parse("TRY start end CATCH() handler"));
			assertErrors(() -> Parse.parse("TRY start end CATCH handler"));
			// Missing
			assertErrors(() -> Parse.parse("TRY start end"));
			assertErrors(() -> Parse.parse("TRY start"));
			assertErrors(() -> Parse.parse("TRY"));
		}

		@Test
		public void testBadInt() {
			// Long
			assertErrors(() -> Parse.parse("BIPUSH 100000000000000"));
			// Float
			assertErrors(() -> Parse.parse("BIPUSH 120F"));
			// Double
			assertErrors(() -> Parse.parse("BIPUSH 120.0"));
		}

		@Test
		public void testBadField() {
			// Invalid because no name could be matched
			assertErrors(() -> Parse.parse("GETFIELD Dummy Ljava/lang/Stri"));
			// Invalid because field descriptor is not complete
			assertErrors(() -> Parse.parse("GETFIELD Dummy.in Ljava/lang/Stri"));
			// Invalid because field descriptor is not complete
			assertErrors(() -> Parse.parse("GETFIELD Dummy.in [[Ljava/lang/Stri"));
			// Invalid because field only specifies owner
			assertErrors(() -> Parse.parse("GETFIELD Dummy"));
		}

		@Test
		public void testBadMethod() {
			// missing variable name, but has desc
			assertErrors(() -> Parse.parse("INVOKESTATIC Dummy.(I)V"));
			// missing return type
			assertErrors(() -> Parse.parse("INVOKESTATIC Dummy.call(I)"));
			// descriptor is incomplete
			assertErrors(() -> Parse.parse("INVOKESTATIC Dummy.call(I"));
			assertErrors(() -> Parse.parse("INVOKESTATIC Dummy.call("));
			// descriptor is missing
			assertErrors(() -> Parse.parse("INVOKESTATIC Dummy.call"));
		}
	}

	// ================================================================ //
	// ============================= UTILS ============================ //
	// ================================================================ //

	private static List<String> suggest(ParseResult<RootAST> ast, String line) {
		// Don't suggest opcodes, not the point here
		if (!line.contains(" "))
			return Collections.emptyList();
		// Create dummy AST if needed
		if (ast == null)
			ast = Parse.parse("");
		try {
			String firstToken = Objects.requireNonNull(RegexUtil.getFirstWord(line));
			String lastToken = Objects.requireNonNull(RegexUtil.getLastWord(line));
			return Parse.getParser(-1, firstToken).suggest(ast, line).stream()
					.filter(option -> !lastToken.equals(option))
					.collect(Collectors.toList());
		} catch(Exception ex) {
			fail(ex);
			return null;
		}
	}

	private static void assertErrors(Supplier<ParseResult<?>> resultSupplier) {
		List<ASTParseException> problems = resultSupplier.get().getProblems();
		// problems.forEach(e -> System.err.println(e.getMessage()));
		assertFalse(problems.isEmpty());
	}

	@SuppressWarnings("unchecked")
	private static <T extends AST> T single(String line) {
		try {
			ParseResult<RootAST> res = Parse.parse(line);
			if (!res.isSuccess())
				fail(res.getProblems().get(0));
			return (T) res.getRoot().getChildren().get(0);
		} catch(ClassCastException ex) {
			fail(ex);
			return (T) new RootAST();
		}
	}

	// Hiding the ugly constant down here
	private static final String H_META = "handle[H_INVOKESTATIC java/lang/invoke/LambdaMetafactory." +
			"metafactory(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;" +
			"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;" +
			"Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)" +
			"Ljava/lang/invoke/CallSite;]";
}
