package me.coley.recaf.assemble;

import me.coley.recaf.assemble.ast.CodeEntry;
import me.coley.recaf.assemble.ast.HandleInfo;
import me.coley.recaf.assemble.ast.PrintContext;
import me.coley.recaf.assemble.ast.Unit;
import me.coley.recaf.assemble.ast.arch.MethodDefinition;
import me.coley.recaf.assemble.ast.insn.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.Type;
import org.opentest4j.AssertionFailedError;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests strictly for parsing into the AST nodes.
 */
public class InstructionParseTests extends JasmUtils {
	@Nested
	public class Field {

		@Test
		public void testDefaultPackage() {
			handle("getstatic DefaultPackage.integer I", field -> {
				assertEquals("DefaultPackage", field.getOwner());
				assertEquals("integer", field.getName());
				assertEquals("I", field.getDesc());
			});
		}

		@Test
		public void testOwnerNameShadowsPrimitiveClass() {
			handle("getstatic I.Z F", field -> {
				assertEquals("I", field.getOwner());
				assertEquals("Z", field.getName());
				assertEquals("F", field.getDesc());
			});
			handle("getstatic C.B [I", field -> {
				assertEquals("C", field.getOwner());
				assertEquals("B", field.getName());
				assertEquals("[I", field.getDesc());
			});
		}

		@Test
		public void testUnicodeMatch() {
			handle("getstatic 下.雨 L了;", field -> {
				assertEquals("下", field.getOwner());
				assertEquals("雨", field.getName());
				assertEquals("L了;", field.getDesc());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("getstatic \\u4E0B.\\u96E8 L\\u4E86;", field -> {
				assertEquals("\\u4E0B", field.getOwner());
				assertEquals("\\u96E8", field.getName());
				assertEquals("L\\u4E86;", field.getDesc());
			});
		}

		@Test
		public void testNumericIntMatch() {
			handle("getstatic 1.5 I", field -> {
				assertEquals("1", field.getOwner());
				assertEquals("5", field.getName());
				assertEquals("I", field.getDesc());
			});
		}

		@Test
		public void testNumericFloatMatch() {
			handle("getstatic 1.5f I", field -> {
				assertEquals("1", field.getOwner());
				assertEquals("5f", field.getName());
				assertEquals("I", field.getDesc());
			});
		}

		private void handle(String original, Consumer<FieldInstruction> handler) {
			FieldInstruction instruction = (FieldInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + instruction.print(PrintContext.DEFAULT_CTX));

			handler.accept(instruction);
		}
	}

	@Nested
	public class Method {
		@Test
		public void testNormal() {
			handle("invokestatic java/lang/System.getProperties ()Ljava/lang/Properties;", method -> {
				assertEquals("java/lang/System", method.getOwner());
				assertEquals("getProperties", method.getName());
				assertEquals("()Ljava/lang/Properties;", method.getDesc());
			});
		}

		@Test
		public void testNormalPrimAndObjParams() {
			handle("invokestatic com/example/FooBar.foo (ILjava/lang/String;)I", method -> {
				assertEquals("com/example/FooBar", method.getOwner());
				assertEquals("foo", method.getName());
				assertEquals("(ILjava/lang/String;)I", method.getDesc());
			});
		}

		@Test
		public void testNormalObjAndPrimParams() {
			handle("invokestatic com/example/FooBar.foo (Ljava/lang/String;I)I", method -> {
				assertEquals("com/example/FooBar", method.getOwner());
				assertEquals("foo", method.getName());
				assertEquals("(Ljava/lang/String;I)I", method.getDesc());
			});
		}

		@Test
		public void testDefaultPackage() {
			handle("invokestatic DefaultPackage.getInt ()I", method -> {
				assertEquals("DefaultPackage", method.getOwner());
				assertEquals("getInt", method.getName());
				assertEquals("()I", method.getDesc());
			});
		}

		@Test
		public void testNameShadowsPrimitiveClass() {
			handle("invokestatic I.Z ()F", method -> {
				assertEquals("I", method.getOwner());
				assertEquals("Z", method.getName());
				assertEquals("()F", method.getDesc());
			});
			handle("invokestatic C.B ([I)[Z", method -> {
				assertEquals("C", method.getOwner());
				assertEquals("B", method.getName());
				assertEquals("([I)[Z", method.getDesc());
			});
		}

		@Test
		public void testNameShadowsKeyword() {
			handle("invokestatic public.STATIC (LSTORE;)F", method -> {
				assertEquals("public", method.getOwner());
				assertEquals("STATIC", method.getName());
				assertEquals("(LSTORE;)F", method.getDesc());
			});
		}

		@Test
		public void testUnicodeMatch() {
			handle("invokestatic 下.雨 (L下;)L了;", method -> {
				assertEquals("下", method.getOwner());
				assertEquals("雨", method.getName());
				assertEquals("(L下;)L了;", method.getDesc());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("invokestatic \\u4E0B.\\u96E8 (L\\u4E0B;)L\\u4E86;", method -> {
				assertEquals("\\u4E0B", method.getOwner());
				assertEquals("\\u96E8", method.getName());
				assertEquals("(L\\u4E0B;)L\\u4E86;", method.getDesc());
			});
		}

		private void handle(String original, Consumer<MethodInstruction> handler) {
			MethodInstruction method = (MethodInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + method.print(PrintContext.DEFAULT_CTX));

			handler.accept(method);
		}
	}

	@Nested
	public class InvokeDynamic {
		@Test
		public void testNormal() {
			handle("invokedynamic getText ()Ljava/lang/String; " +
					"handle H_GETFIELD java/lang/Example.foo Ljava/lang/String; " +
					"args 1 \"Hello\" type java/lang/String end", indy -> {
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
			handle("invokedynamic getText ()Ljava/lang/String; " +
					"handle H_GETFIELD java/lang/Example.foo Ljava/lang/String; args end", indy -> {
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
			handle("invokedynamic 今 ()L天; " +
					"handle H_GETFIELD 下.大 L天; " +
					"args 1 \"今天下大雨\" type 雨 end", indy -> {
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

		private void handle(String original, Consumer<IndyInstruction> handler) {
			IndyInstruction indy = (IndyInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + indy.print(PrintContext.DEFAULT_CTX));

			handler.accept(indy);
		}
	}

	@Nested
	public class LDC {
		@Test
		public void testInt() {
			handle("ldc 100", ldc -> assertEquals(100, ldc.getValue()));
			handle("ldc -100", ldc -> assertEquals(-100, ldc.getValue()));
			handle("ldc 0x0", ldc -> assertEquals(0, ldc.getValue()));
			handle("ldc 0xA", ldc -> assertEquals(0xA, ldc.getValue()));
			handle("ldc 0x7FFFFFFF", ldc -> assertEquals(0x7FFFFFFF, ldc.getValue()));
		}

		@Test
		public void testLong() {
			handle("ldc 0L", ldc -> assertEquals(0L, ldc.getValue()));
			handle("ldc 100000000000L", ldc -> assertEquals(100000000000L, ldc.getValue()));
			handle("ldc -100000000000L", ldc -> assertEquals(-100000000000L, ldc.getValue()));
			handle("ldc 0x0L", ldc -> assertEquals(0L, ldc.getValue()));
			handle("ldc 0xAL", ldc -> assertEquals(0xAL, ldc.getValue()));
			handle("ldc 0x7FFFFFFFL", ldc -> assertEquals(0x7FFFFFFFL, ldc.getValue()));
			handle("ldc 0x900000000L", ldc -> assertEquals(0x900000000L, ldc.getValue()));
			handle("ldc 0x1234567891011L", ldc -> assertEquals(0x1234567891011L, ldc.getValue()));
		}

		@Test
		public void testDouble() {
			handle("ldc 0.0", ldc -> assertEquals(0.0, ldc.getValue()));
			handle("ldc 5.5", ldc -> assertEquals(5.5, ldc.getValue()));
			handle("ldc 127.", ldc -> assertEquals(127., ldc.getValue()));
			handle("ldc 10000000000000.6", ldc -> assertEquals(10000000000000.6, ldc.getValue()));
			handle("ldc -25.65", ldc -> assertEquals(-25.65, ldc.getValue()));
		}

		@Test
		public void testFloat() {
			handle("ldc 0.0F", ldc -> assertEquals(0.0F, ldc.getValue()));
			handle("ldc 5.5F", ldc -> assertEquals(5.5F, ldc.getValue()));
			handle("ldc 127.F", ldc -> assertEquals(127.F, ldc.getValue()));
			handle("ldc 10000000000000.6F", ldc -> assertEquals(10000000000000.6F, ldc.getValue()));
			handle("ldc -25.65F", ldc -> assertEquals(-25.65F, ldc.getValue()));
		}

		@Test
		public void testChar() {
			handle("ldc 'A'", ldc -> assertEquals('A', ldc.getValue()));
		}

		@Test
		public void testType() {
			handle("ldc type java/lang/Object", ldc -> assertEquals(Type.getObjectType("java/lang/Object"), ldc.getValue()));
			handle("ldc type DefaultPackage", ldc -> assertEquals(Type.getObjectType("DefaultPackage"), ldc.getValue()));
			handle("ldc type I", ldc -> assertEquals(Type.getObjectType("I"), ldc.getValue()));
			handle("ldc type 下雨了", ldc -> assertEquals(Type.getObjectType("下雨了"), ldc.getValue()));
			handle("ldc type \\u4E0B\\u96E8\\u4E86", ldc -> assertEquals(Type.getObjectType("\\u4E0B\\u96E8\\u4E86"), ldc.getValue()));
		}

		@ParameterizedTest
		@ValueSource(strings = {
				"Hello world!",
				"\\u4E0B\\u96E8\\u4E86",
				"下雨了",
				"\\n",
				"\\u0000",
				"\\\"",
				"",
				"C:\\example\\recaf.jar",
				"C:\\\\example\\\\recaf.jar",
		})
		public void testString(String arg) {
			// By default, the content of ldc strings is NOT unescaped when parsed.
			handle("ldc \"" + arg + "\"", ldc -> assertEquals(arg, ldc.getValue()));
		}

		private void handle(String original, Consumer<LdcInstruction> handler) {
			LdcInstruction ldc = (LdcInstruction) staticHandle(original);

			System.out.println("ldc Value type: " + ldc.getValueType().name());
			System.out.println("Original: " + original);
			System.out.println("Printed:  " + ldc.print(PrintContext.DEFAULT_CTX));

			handler.accept(ldc);
		}
	}

	@Nested
	public class IINC {
		@Test
		public void testValues() {
			handle("iinc v 100", iinc -> assertEquals(100, iinc.getIncrement()));
			handle("iinc v -100", iinc -> assertEquals(-100, iinc.getIncrement()));
			handle("iinc v 0xF", iinc -> assertEquals(0xF, iinc.getIncrement()));
		}

		@Test
		public void testNames() {
			handle("iinc v 1", iinc -> assertEquals("v", iinc.getVariableIdentifier()));
			handle("iinc I 1", iinc -> assertEquals("I", iinc.getVariableIdentifier()));
			handle("iinc 雨 1", iinc -> assertEquals("雨", iinc.getVariableIdentifier()));
			handle("iinc \\u96E8 1", iinc -> assertEquals("\\u96E8", iinc.getVariableIdentifier()));
		}

		private void handle(String original, Consumer<IincInstruction> handler) {
			IincInstruction insn = (IincInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + insn.print(PrintContext.DEFAULT_CTX));

			handler.accept(insn);
		}
	}

	@Nested
	public class Integers {
		@Test
		public void testValues() {
			handle("sipush 100", i -> assertEquals(100, i.getValue()));
			handle("sipush -100", i -> assertEquals(-100, i.getValue()));
			handle("sipush 0xF", i -> assertEquals(0xF, i.getValue()));
		}

		@Test
		public void testOutOfBounds() {
			assertThrows(AssertionFailedError.class, () -> handle("sipush 32768", i -> {}));
			assertThrows(AssertionFailedError.class, () -> handle("sipush -32769", i -> {}));
			assertThrows(AssertionFailedError.class, () -> handle("sipush 0x10000", i -> {}));
			assertThrows(AssertionFailedError.class, () -> handle("bipush 128", i -> {}));
			assertThrows(AssertionFailedError.class, () -> handle("bipush -129", i -> {}));
		}

		private void handle(String original, Consumer<IntInstruction> handler) {
			IntInstruction insn = (IntInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + insn.print(PrintContext.DEFAULT_CTX));

			handler.accept(insn);
		}

	}

	@Nested
	public class Jump {
		@Test
		public void testValues() {
			handle("ifeq A", jump -> assertEquals("A", jump.getLabel()));
			handle("ifeq 雨", jump -> assertEquals("雨", jump.getLabel()));
			handle("ifeq \\u96E8", jump -> assertEquals("\\u96E8", jump.getLabel()));
		}

		private void handle(String original, Consumer<JumpInstruction> handler) {
			JumpInstruction jump = (JumpInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + jump.print(PrintContext.DEFAULT_CTX));

			handler.accept(jump);
		}
	}

	@Nested
	public class Line {
		@Test
		public void testLabels() {
			handle("line A 0", line -> assertEquals("A", line.getLabel()));
			handle("line 雨 0", line -> assertEquals("雨", line.getLabel()));
			handle("line \\u96E8 0", line -> assertEquals("\\u96E8", line.getLabel()));
		}

		@Test
		public void testValues() {
			handle("line A 0", line -> assertEquals(0, line.getLineNo()));
			handle("line A 100", line -> assertEquals(100, line.getLineNo()));
			handle("line A -100", line -> assertEquals(-100, line.getLineNo()));
		}

		private void handle(String original, Consumer<LineInstruction> handler) {
			LineInstruction line = (LineInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + line.print(PrintContext.DEFAULT_CTX));

			handler.accept(line);
		}
	}

	@Nested
	public class Variable {
		@Test
		public void testNames() {
			handle("aload 1", v -> assertEquals("1", v.getVariableIdentifier()));
			handle("aload A", v -> assertEquals("A", v.getVariableIdentifier()));
			handle("aload 雨", v -> assertEquals("雨", v.getVariableIdentifier()));
			handle("aload \\\\u96E8", v -> assertEquals("\\u96E8", v.getVariableIdentifier()));
			handle("aload \\e", v -> assertEquals("", v.getVariableIdentifier()));
		}

		private void handle(String original, Consumer<VarInstruction> handler) {
			VarInstruction v = (VarInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + v.print(PrintContext.DEFAULT_CTX));

			handler.accept(v);
		}
	}

	@Nested
	public class Types {
		@Test
		public void testNames() {
			handle("new A", type -> assertEquals("A", type.getType()));
			handle("new 雨", type -> assertEquals("雨", type.getType()));
			handle("new \\\\u96E8", type -> assertEquals("\\u96E8", type.getType()));
		}

		private void handle(String original, Consumer<TypeInstruction> handler) {
			TypeInstruction type = (TypeInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + type.print(PrintContext.DEFAULT_CTX));

			handler.accept(type);
		}
	}

	@Nested
	public class MultiArray {
		@Test
		public void testDims() {
			handle("multianewarray LFoo; 1", array -> assertEquals(1, array.getDimensions()));
			handle("multianewarray LFoo; -1", array -> assertEquals(-1, array.getDimensions()));
		}

		@Test
		public void testType() {
			handle("multianewarray Ljava/lang/String; 1", array -> assertEquals("Ljava/lang/String;", array.getDesc()));
			handle("multianewarray L雨; 1", array -> assertEquals("L雨;", array.getDesc()));
			handle("multianewarray L\\u96E8; 1", array -> assertEquals("L\\u96E8;", array.getDesc()));
		}

		private void handle(String original, Consumer<MultiArrayInstruction> handler) {
			MultiArrayInstruction array = (MultiArrayInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + array.print(PrintContext.DEFAULT_CTX));

			handler.accept(array);
		}
	}

	@Nested
	public class NewArray {
		@ValueSource(strings = {"boolean", "short", "float", "double", "byte", "char", "int", "long"})
		@ParameterizedTest
		public void test(String value) {
			handle("newarray " + value, array -> assertEquals(value, array.getArrayType()));
		}

		private void handle(String original, Consumer<NewArrayInstruction> handler) {
			NewArrayInstruction array = (NewArrayInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + array.print(PrintContext.DEFAULT_CTX));

			handler.accept(array);
		}
	}

	@Nested
	public class LookupSwitch {
		@Test
		public void testNormal() {
			handle("lookupswitch case 0 A case 1 B case 2 C default D", swtch -> {
				assertEquals("D", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("A", swtch.getEntries().get(0).getName());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("B", swtch.getEntries().get(1).getName());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("C", swtch.getEntries().get(2).getName());
			});
		}

		@Test
		public void testUnicodeMatch() {
			// 下雨了
			handle("lookupswitch case 0 下 case 1 雨 case 2 了 default 雨", swtch -> {
				assertEquals("雨", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("下", swtch.getEntries().get(0).getName());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("雨", swtch.getEntries().get(1).getName());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("了", swtch.getEntries().get(2).getName());
			});
		}

		@Test
		public void testUnicodeEscapedMatch() {
			handle("lookupswitch case 0 \\u4E0B case 1 \\u96E8 case 2 \\u4E86 default \\u96E8", swtch -> {
				assertEquals("\\u96E8", swtch.getDefaultIdentifier());
				assertEquals(3, swtch.getEntries().size());

				assertEquals(0, swtch.getEntries().get(0).getKey());
				assertEquals("\\u4E0B", swtch.getEntries().get(0).getName());

				assertEquals(1, swtch.getEntries().get(1).getKey());
				assertEquals("\\u96E8", swtch.getEntries().get(1).getName());

				assertEquals(2, swtch.getEntries().get(2).getKey());
				assertEquals("\\u4E86", swtch.getEntries().get(2).getName());
			});
		}

		private void handle(String original, Consumer<LookupSwitchInstruction> handler) {
			LookupSwitchInstruction swtch = (LookupSwitchInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + swtch.print(PrintContext.DEFAULT_CTX));

			handler.accept(swtch);
		}
	}

	@Nested
	public class TableSwitch {
		@Test
		public void testNormal() {
			handle("tableswitch 0 2 A B C default D", swtch -> {
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
			TableSwitchInstruction swtch = (TableSwitchInstruction) staticHandle(original);

			System.out.println("Original: " + original);
			System.out.println("Printed:  " + swtch.print(PrintContext.DEFAULT_CTX));

			handler.accept(swtch);
		}
	}

	private static CodeEntry staticHandle(String code) {
		String wrapped = "method somethind ()V\n" + code + "\nend";
		Unit unit = createSilentUnit(DEFAULT_KEYWORDS, wrapped);
		assertNotNull(unit);
		MethodDefinition method = unit.getDefinitionAsMethod();
		assertEquals(1, method.getCode().getEntries().size());
		return method.getCode().getEntries().get(0);
	}
}
