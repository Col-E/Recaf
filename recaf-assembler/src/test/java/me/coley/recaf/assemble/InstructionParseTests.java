package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.insn.*;
import me.coley.recaf.assemble.parser.BytecodeParser;
import me.coley.recaf.assemble.parser.BytecodeVisitorImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Type;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests strictly for parsing into the AST nodes.
 */
public class InstructionParseTests extends TestUtil {
	@Nested
	public class Field {
		@ParameterizedTest
		@ValueSource(strings = {
				"GETSTATIC java/lang/System.out Ljava/io/PrintStream;",
				"\tGETSTATIC\tjava/lang/System\t.out\tLjava/io/PrintStream;\t",
				" GETSTATIC \n java/lang/System .  out \n\t\t   Ljava/io/PrintStream;"
		})
		public void testWithWhitespace(String input) {
			handle(input, field -> {
				assertEquals("java/lang/System", field.getOwner());
				assertEquals("out", field.getName());
				assertEquals("Ljava/io/PrintStream;", field.getDesc());
			});
		}

		@Test
		public void testDefaultPackage() {
			handle("GETSTATIC DefaultPackage.integer I", field -> {
				assertEquals("DefaultPackage", field.getOwner());
				assertEquals("integer", field.getName());
				assertEquals("I", field.getDesc());
			});
		}

		@Test
		public void testOwnerNameShadowsPrimitiveClass() {
			handle("GETSTATIC I.Z F", field -> {
				assertEquals("I", field.getOwner());
				assertEquals("Z", field.getName());
				assertEquals("F", field.getDesc());
			});
			handle("GETSTATIC C.B [I", field -> {
				assertEquals("C", field.getOwner());
				assertEquals("B", field.getName());
				assertEquals("[I", field.getDesc());
			});
		}

		@Test
		public void testUnicodeMatch() {
			handle("GETSTATIC 下.雨 L了;", field -> {
				assertEquals("下", field.getOwner());
				assertEquals("雨", field.getName());
				assertEquals("L了;", field.getDesc());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("GETSTATIC \\u4E0B.\\u96E8 L\\u4E86;", field -> {
				assertEquals("\\u4E0B", field.getOwner());
				assertEquals("\\u96E8", field.getName());
				assertEquals("L\\u4E86;", field.getDesc());
			});
		}

		private void handle(String original, Consumer<FieldInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnFieldContext fieldCtx = parser.insnField();
			assertNotNull(fieldCtx, "Parser did not find field context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			FieldInstruction field = (FieldInstruction) visitor.visitInsnField(fieldCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + field.print());

			handler.accept(field);
		}
	}

	@Nested
	public class Method {
		@Test
		public void testNormal() {
			handle("INVOKESTATIC java/lang/System.getProperties()Ljava/lang/Properties;", method -> {
				assertEquals("java/lang/System", method.getOwner());
				assertEquals("getProperties", method.getName());
				assertEquals("()Ljava/lang/Properties;", method.getDesc());
			});
		}

		@Test
		public void testDefaultPackage() {
			handle("INVOKESTATIC DefaultPackage.getInt()I", method -> {
				assertEquals("DefaultPackage", method.getOwner());
				assertEquals("getInt", method.getName());
				assertEquals("()I", method.getDesc());
			});
		}

		@Test
		public void testNameShadowsPrimitiveClass() {
			handle("INVOKESTATIC I.Z()F", method -> {
				assertEquals("I", method.getOwner());
				assertEquals("Z", method.getName());
				assertEquals("()F", method.getDesc());
			});
			handle("INVOKESTATIC C.B([I)[Z", method -> {
				assertEquals("C", method.getOwner());
				assertEquals("B", method.getName());
				assertEquals("([I)[Z", method.getDesc());
			});
		}

		@Test
		public void testNameShadowsKeyword() {
			handle("INVOKESTATIC public.STATIC(LSTORE;)F", method -> {
				assertEquals("public", method.getOwner());
				assertEquals("STATIC", method.getName());
				assertEquals("(LSTORE;)F", method.getDesc());
			});
		}

		@Test
		public void testUnicodeMatch() {
			handle("INVOKESTATIC 下.雨(L下;)L了;", method -> {
				assertEquals("下", method.getOwner());
				assertEquals("雨", method.getName());
				assertEquals("(L下;)L了;", method.getDesc());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("INVOKESTATIC \\u4E0B.\\u96E8(L\\u4E0B;)L\\u4E86;", method -> {
				assertEquals("\\u4E0B", method.getOwner());
				assertEquals("\\u96E8", method.getName());
				assertEquals("(L\\u4E0B;)L\\u4E86;", method.getDesc());
			});
		}

		private void handle(String original, Consumer<MethodInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnMethodContext methodCtx = parser.insnMethod();
			assertNotNull(methodCtx, "Parser did not find method context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			MethodInstruction method = (MethodInstruction) visitor.visitInsnMethod(methodCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + method.print());

			handler.accept(method);
		}
	}

	@Nested
	public class InvokeDynamic {
		@Test
		public void testNormal() {
			handle("INVOKEDYNAMIC getText ()Ljava/lang/String; " +
					"handle(H_GETFIELD java/lang/Example.foo Ljava/lang/String;) " +
					"args(1, \"Hello\", java/lang/String)", indy -> {
				assertEquals("getText", indy.getName());
				assertEquals("()Ljava/lang/String;", indy.getDesc());

				HandleInfo handleInfo = indy.getBsmHandle();
				assertEquals("H_GETFIELD", handleInfo.getTag());
				assertEquals("java/lang/Example", handleInfo.getOwner());
				assertEquals("foo", handleInfo.getName());
				assertEquals("Ljava/lang/String;", handleInfo.getDesc());

				assertEquals(1, indy.getBsmArguments().get(0).getValue());
				assertEquals("Hello", indy.getBsmArguments().get(1).getValue());
				assertEquals(Type.getObjectType("java/lang/String"), indy.getBsmArguments().get(2).getValue());
			});
		}

		@Test
		public void testNoArgs() {
			handle("INVOKEDYNAMIC getText ()Ljava/lang/String; " +
					"handle(H_GETFIELD java/lang/Example.foo Ljava/lang/String;) ", indy -> {
				assertEquals("getText", indy.getName());
				assertEquals("()Ljava/lang/String;", indy.getDesc());

				HandleInfo handleInfo = indy.getBsmHandle();
				assertEquals("H_GETFIELD", handleInfo.getTag());
				assertEquals("java/lang/Example", handleInfo.getOwner());
				assertEquals("foo", handleInfo.getName());
				assertEquals("Ljava/lang/String;", handleInfo.getDesc());

				assertEquals(0, indy.getBsmArguments().size());
			});
		}

		@Test
		public void testUnicodeMatch() {
			handle("INVOKEDYNAMIC 今 ()L天; " +
					"handle(H_GETFIELD 下.大 L天;) " +
					"args(1, \"今天下大雨\", 雨)", indy -> {
				assertEquals("今", indy.getName());
				assertEquals("()L天;", indy.getDesc());

				HandleInfo handleInfo = indy.getBsmHandle();
				assertEquals("H_GETFIELD", handleInfo.getTag());
				assertEquals("下", handleInfo.getOwner());
				assertEquals("大", handleInfo.getName());
				assertEquals("L天;", handleInfo.getDesc());

				assertEquals(1, indy.getBsmArguments().get(0).getValue());
				assertEquals("今天下大雨", indy.getBsmArguments().get(1).getValue());
				assertEquals(Type.getObjectType("雨"), indy.getBsmArguments().get(2).getValue());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			// I know, the unicode escape being LITERAL in descriptors is illegal,
			// we're just doing basic parse-matching tests here. It'll be escaped later.
			handle("INVOKEDYNAMIC \\u4E0B ()L\\u4E86; " +
					"handle(H_GETFIELD \\u96E8.\\u4E0B L\\u4E86;) " +
					"args(1, \"Hello\", \\u4E86)", indy -> {
				assertEquals("\\u4E0B", indy.getName());
				assertEquals("()L\\u4E86;", indy.getDesc());

				HandleInfo handleInfo = indy.getBsmHandle();
				assertEquals("H_GETFIELD", handleInfo.getTag());
				assertEquals("\\u96E8", handleInfo.getOwner());
				assertEquals("\\u4E0B", handleInfo.getName());
				assertEquals("L\\u4E86;", handleInfo.getDesc());

				assertEquals(1, indy.getBsmArguments().get(0).getValue());
				assertEquals("Hello", indy.getBsmArguments().get(1).getValue());
				assertEquals(Type.getObjectType("\\u4E86"), indy.getBsmArguments().get(2).getValue());
			});
		}

		private void handle(String original, Consumer<IndyInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnDynamicContext dynamic = parser.insnDynamic();
			assertNotNull(dynamic, "Parser did not find invoke-dynamic context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			IndyInstruction indy = (IndyInstruction) visitor.visitInsnDynamic(dynamic);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + indy.print());

			handler.accept(indy);
		}
	}

	@Nested
	public class LDC {
		@Test
		public void testInt() {
			handle("LDC 100", ldc -> assertEquals(100, ldc.getValue()));
			handle("LDC -100", ldc -> assertEquals(-100, ldc.getValue()));
			handle("LDC 0x0", ldc -> assertEquals(0, ldc.getValue()));
			handle("LDC 0xA", ldc -> assertEquals(0xA, ldc.getValue()));
			handle("LDC 0x7FFFFFFF", ldc -> assertEquals(0x7FFFFFFF, ldc.getValue()));
		}

		@Test
		public void testLong() {
			handle("LDC 0L", ldc -> assertEquals(0L, ldc.getValue()));
			handle("LDC 100000000000L", ldc -> assertEquals(100000000000L, ldc.getValue()));
			handle("LDC -100000000000L", ldc -> assertEquals(-100000000000L, ldc.getValue()));
			handle("LDC 0x0L", ldc -> assertEquals(0L, ldc.getValue()));
			handle("LDC 0xAL", ldc -> assertEquals(0xAL, ldc.getValue()));
			handle("LDC 0x7FFFFFFFL", ldc -> assertEquals(0x7FFFFFFFL, ldc.getValue()));
			handle("LDC 0x900000000L", ldc -> assertEquals(0x900000000L, ldc.getValue()));
			handle("LDC 0x1234567891011L", ldc -> assertEquals(0x1234567891011L, ldc.getValue()));
		}

		@Test
		public void testDouble() {
			handle("LDC 0.0", ldc -> assertEquals(0.0, ldc.getValue()));
			handle("LDC 5.5", ldc -> assertEquals(5.5, ldc.getValue()));
			handle("LDC 127.", ldc -> assertEquals(127., ldc.getValue()));
			handle("LDC 10000000000000.6", ldc -> assertEquals(10000000000000.6, ldc.getValue()));
			handle("LDC -25.65", ldc -> assertEquals(-25.65, ldc.getValue()));
		}

		@Test
		public void testFloat() {
			handle("LDC 0.0F", ldc -> assertEquals(0.0F, ldc.getValue()));
			handle("LDC 5.5F", ldc -> assertEquals(5.5F, ldc.getValue()));
			handle("LDC 127.F", ldc -> assertEquals(127.F, ldc.getValue()));
			handle("LDC 10000000000000.6F", ldc -> assertEquals(10000000000000.6F, ldc.getValue()));
			handle("LDC -25.65F", ldc -> assertEquals(-25.65F, ldc.getValue()));
		}

		@Test
		public void testType() {
			handle("LDC java/lang/Object", ldc -> assertEquals(Type.getObjectType("java/lang/Object"), ldc.getValue()));
			handle("LDC DefaultPackage", ldc -> assertEquals(Type.getObjectType("DefaultPackage"), ldc.getValue()));
			handle("LDC I", ldc -> assertEquals(Type.getObjectType("I"), ldc.getValue()));
			handle("LDC 下雨了", ldc -> assertEquals(Type.getObjectType("下雨了"), ldc.getValue()));
			handle("LDC \\u4E0B\\u96E8\\u4E86", ldc -> assertEquals(Type.getObjectType("\\u4E0B\\u96E8\\u4E86"), ldc.getValue()));
		}

		@Test
		public void testString() {
			// By default, the content of LDC strings is NOT unescaped when parsed.
			handle("LDC \"Hello world!\"", ldc -> assertEquals("Hello world!", ldc.getValue()));
			handle("LDC \"C:\\\\example\\\\recaf.jar\"", ldc -> assertEquals("C:\\\\example\\\\recaf.jar", ldc.getValue()));
			handle("LDC \"C:\\example\\recaf.jar\"", ldc -> assertEquals("C:\\example\\recaf.jar", ldc.getValue()));
			handle("LDC \"\"", ldc -> assertEquals("", ldc.getValue()));
			handle("LDC \"\\u0000\"", ldc -> assertEquals("\\u0000", ldc.getValue()));
			handle("LDC \"\\n\"", ldc -> assertEquals("\\n", ldc.getValue()));
			handle("LDC \"\"\"", ldc -> assertEquals("\"", ldc.getValue()));
			handle("LDC \"下雨了\"", ldc -> assertEquals("下雨了", ldc.getValue()));
			handle("LDC \"\\u4E0B\\u96E8\\u4E86\"", ldc -> assertEquals("\\u4E0B\\u96E8\\u4E86", ldc.getValue()));
		}

		private void handle(String original, Consumer<LdcInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnLdcContext ldcCtx = parser.insnLdc();
			assertNotNull(ldcCtx, "Parser did not find LDC context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			LdcInstruction ldc = (LdcInstruction) visitor.visitInsnLdc(ldcCtx);

			System.out.println("LDC Value type: " + ldc.getValueType().name());
			System.out.println("Original: " + original);
			System.out.println("Printed:  " + ldc.print());

			handler.accept(ldc);
		}
	}

	@Nested
	public class IINC {
		@Test
		public void testValues() {
			handle("IINC v 100", iinc -> assertEquals(100, iinc.getIncrement()));
			handle("IINC v -100", iinc -> assertEquals(-100, iinc.getIncrement()));
			handle("IINC v 0xF", iinc -> assertEquals(0xF, iinc.getIncrement()));
		}

		@Test
		public void testNames() {
			handle("IINC v 1", iinc -> assertEquals("v", iinc.getIdentifier()));
			handle("IINC I 1", iinc -> assertEquals("I", iinc.getIdentifier()));
			handle("IINC 雨 1", iinc -> assertEquals("雨", iinc.getIdentifier()));
			handle("IINC \\u96E8 1", iinc -> assertEquals("\\u96E8", iinc.getIdentifier()));
		}

		private void handle(String original, Consumer<IincInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnIincContext iincCtx = parser.insnIinc();
			assertNotNull(iincCtx, "Parser did not find IINC context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			IincInstruction insn = (IincInstruction) visitor.visitInsnIinc(iincCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + insn.print());

			handler.accept(insn);
		}
	}

	@Nested
	public class Integers {
		@Test
		public void testValues() {
			handle("SIPUSH 100", i -> assertEquals(100, i.getValue()));
			handle("SIPUSH -100", i -> assertEquals(-100, i.getValue()));
			handle("SIPUSH 0xF", i -> assertEquals(0xF, i.getValue()));
		}

		private void handle(String original, Consumer<IntInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnIntContext intCtx = parser.insnInt();
			assertNotNull(intCtx, "Parser did not find INT context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			IntInstruction insn = (IntInstruction) visitor.visitInsnInt(intCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + insn.print());

			handler.accept(insn);
		}
	}

	@Nested
	public class Jump {
		@Test
		public void testValues() {
			handle("IFEQ A", jump -> assertEquals("A", jump.getLabel()));
			handle("IFEQ 雨", jump -> assertEquals("雨", jump.getLabel()));
			handle("IFEQ \\u96E8", jump -> assertEquals("\\u96E8", jump.getLabel()));
		}

		private void handle(String original, Consumer<JumpInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnJumpContext jumpCtx = parser.insnJump();
			assertNotNull(jumpCtx, "Parser did not find JUMP context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			JumpInstruction jump = (JumpInstruction) visitor.visitInsnJump(jumpCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + jump.print());

			handler.accept(jump);
		}
	}

	@Nested
	public class Line {
		@Test
		public void testLabels() {
			handle("LINE A 0", line -> assertEquals("A", line.getLabel()));
			handle("LINE 雨 0", line -> assertEquals("雨", line.getLabel()));
			handle("LINE \\u96E8 0", line -> assertEquals("\\u96E8", line.getLabel()));
		}

		@Test
		public void testValues() {
			handle("LINE A 0", line -> assertEquals(0, line.getLineNo()));
			handle("LINE A 100", line -> assertEquals(100, line.getLineNo()));
			handle("LINE A -100", line -> assertEquals(-100, line.getLineNo()));
		}

		private void handle(String original, Consumer<LineInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnLineContext lineCtx = parser.insnLine();
			assertNotNull(lineCtx, "Parser did not find LINE context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			LineInstruction line = (LineInstruction) visitor.visitInsnLine(lineCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + line.print());

			handler.accept(line);
		}
	}

	@Nested
	public class Variable {
		@Test
		public void testNames() {
			handle("ALOAD 1", v -> assertEquals("1", v.getIdentifier()));
			handle("ALOAD A", v -> assertEquals("A", v.getIdentifier()));
			handle("ALOAD 雨", v -> assertEquals("雨", v.getIdentifier()));
			handle("ALOAD \\u96E8", v -> assertEquals("\\u96E8", v.getIdentifier()));
		}

		private void handle(String original, Consumer<VarInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnVarContext varCtx = parser.insnVar();
			assertNotNull(varCtx, "Parser did not find VAR context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			VarInstruction v = (VarInstruction) visitor.visitInsnVar(varCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + v.print());

			handler.accept(v);
		}
	}

	@Nested
	public class Types {
		@Test
		public void testNames() {
			handle("NEW A", type -> assertEquals("A", type.getType()));
			handle("NEW 雨", type -> assertEquals("雨", type.getType()));
			handle("NEW \\u96E8", type -> assertEquals("\\u96E8", type.getType()));
		}

		private void handle(String original, Consumer<TypeInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnTypeContext typeCtx = parser.insnType();
			assertNotNull(typeCtx, "Parser did not find TYPE context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			TypeInstruction type = (TypeInstruction) visitor.visitInsnType(typeCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + type.print());

			handler.accept(type);
		}
	}

	@Nested
	public class MultiArray {
		@Test
		public void testDims() {
			handle("MULTIANEWARRAY LFoo; 1", array -> assertEquals(1, array.getDimensions()));
			handle("MULTIANEWARRAY LFoo; -1", array -> assertEquals(-1, array.getDimensions()));
		}

		@Test
		public void testType() {
			handle("MULTIANEWARRAY Ljava/lang/String; 1", array -> assertEquals("Ljava/lang/String;", array.getDesc()));
			handle("MULTIANEWARRAY L雨; 1", array -> assertEquals("L雨;", array.getDesc()));
			handle("MULTIANEWARRAY L\\u96E8; 1", array -> assertEquals("L\\u96E8;", array.getDesc()));
		}

		private void handle(String original, Consumer<MultiArrayInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnMultiAContext multiCtx = parser.insnMultiA();
			assertNotNull(multiCtx, "Parser did not find MULTI=ARR context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			MultiArrayInstruction array = (MultiArrayInstruction) visitor.visitInsnMultiA(multiCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + array.print());

			handler.accept(array);
		}
	}

	@Nested
	public class NewArray {
		@ValueSource(strings = {"Z", "C", "F", "D", "B", "S", "I", "J"})
		@ParameterizedTest
		public void test(String value) {
			char c = value.charAt(0);
			handle("NEWARRAY '" + c + "'", array -> assertEquals(c, array.getArrayType()));
		}

		private void handle(String original, Consumer<NewArrayInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnNewArrayContext arrayCtx = parser.insnNewArray();
			assertNotNull(arrayCtx, "Parser did not find NEWARRAY context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			NewArrayInstruction array = (NewArrayInstruction) visitor.visitInsnNewArray(arrayCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + array.print());

			handler.accept(array);
		}
	}

	@Nested
	public class LookupSwitch {
		@Test
		public void testNormal() {
			handle("LOOKUPSWITCH mapping(A=0, B=1, C=2) default(D)", swtch -> {
				assertEquals("D", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("A", swtch.getEntries().get(0).getIdentifier());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("B", swtch.getEntries().get(1).getIdentifier());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("C", swtch.getEntries().get(2).getIdentifier());
			});
		}

		@Test
		public void testUnicodeMatch() {
			// 下雨了
			handle("LOOKUPSWITCH mapping(下=0, 雨=1, 了=2) default(雨)", swtch -> {
				assertEquals("雨", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("下", swtch.getEntries().get(0).getIdentifier());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("雨", swtch.getEntries().get(1).getIdentifier());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("了", swtch.getEntries().get(2).getIdentifier());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("LOOKUPSWITCH mapping(\\u4E0B=0, \\u96E8=1, \\u4E86=2) default(\\u96E8)", swtch -> {
				assertEquals("\\u96E8", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("\\u4E0B", swtch.getEntries().get(0).getIdentifier());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("\\u96E8", swtch.getEntries().get(1).getIdentifier());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("\\u4E86", swtch.getEntries().get(2).getIdentifier());
			});
		}

		private void handle(String original, Consumer<LookupSwitchInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnLookupContext switchCtx = parser.insnLookup();
			assertNotNull(switchCtx, "Parser did not find LOOKUP-SWITCH context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			LookupSwitchInstruction swtch = (LookupSwitchInstruction) visitor.visitInsnLookup(switchCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + swtch.print());

			handler.accept(swtch);
		}
	}

	@Nested
	public class TableSwitch {
		@Test
		public void testNormal() {
			handle("TABLESWITCH range(0:2) offsets(A, B, C) default(D)", swtch -> {
				assertEquals(0, swtch.getMin());
				assertEquals(2, swtch.getMax());
				assertEquals(3, swtch.getLabels().size());
				assertEquals("A", swtch.getLabels().get(0));
				assertEquals("B", swtch.getLabels().get(1));
				assertEquals("C", swtch.getLabels().get(2));
				assertEquals("D", swtch.getDefaultIdentifier());
			});
		}

		private void handle(String original, Consumer<TableSwitchInstruction> handler) {
			BytecodeParser parser = parser(original);

			BytecodeParser.InsnTableContext switchCtx = parser.insnTable();
			assertNotNull(switchCtx, "Parser did not find TABLE-SWITCH context with input: " + original);

			BytecodeVisitorImpl visitor = new BytecodeVisitorImpl();
			TableSwitchInstruction swtch = (TableSwitchInstruction) visitor.visitInsnTable(switchCtx);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + swtch.print());

			handler.accept(swtch);
		}
	}
}
