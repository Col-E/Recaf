package me.coley.recaf;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.workspace.ClasspathResource;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * More verbose cases for the assembler.
 *
 * @author Matt
 */
public class AssemblyCasesTest {
	private static final String D1 = "DEFINE static func()V\nSTART:\n";
	private static final String D2 = "\nEND:\n";

	@BeforeAll
	public static void setup() {
		// Set dummy workspace so type analysis works
		Recaf.setCurrentWorkspace(new Workspace(ClasspathResource.get()));
	}

	@Nested
	public class Examples {
		@Test
		public void testHelloWorld() {
			String s = "DEFINE public static hi()V\n" +
					"A:\n" +
					"LINE A 4\n" +
					"GETSTATIC java/lang/System.out Ljava/io/PrintStream;\n" +
					"LDC \"Hello world\"\n" +
					"INVOKEVIRTUAL java/io/PrintStream.println(Ljava/lang/String;)V\n" +
					"B:\n" +
					"LINE B 5\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}

		@Test
		public void testOnlyParameterLocals() {
			String s = "DEFINE public add(I unused, I count)V\n" +
					"START:\n" +
					"ALOAD this\n" +
					"DUP\n" +
					"GETFIELD Test.increment I\n" +
					"ILOAD count\n" + "" +
					"IADD\n" +
					"PUTFIELD Test.increment I\n" +
					"RETURN\n" +
					"END:";
			verifyPass(Parse.parse(s));
		}

		@Test
		public void testPutStatic() {
			String s = "DEFINE public static init()V\n" +
					"THROWS java/io/IOException\n" +
					"A:\n" +
					"LDC Ljava/lang/System;\n" +
					"LDC \"/logo.png\"\n" +
					"INVOKEVIRTUAL java/lang/Class.getResource(Ljava/lang/String;)Ljava/net/URL;\n" +
					"INVOKESTATIC javax/imageio/ImageIO.read(Ljava/net/URL;)Ljava/awt/Image/BufferedImage;\n" +
					"PUTSTATIC Test.logo Ljava/awt/image/BufferedImage;\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}
	}

	@Nested
	public class Strings {
		@Test
		public void testUnicode() {
			try {
				// Support unicode
				String s = "DEFINE static x()V\n" +
						"LDC \"下雨了\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("下雨了", ldc.cst);
			}catch(Exception ex) {
				fail(ex);
			}
		}

		@Test
		public void testEscapedUnicode() {
			try {
				// Unescape unicode
				String s = "DEFINE static x()V\n" +
						"LDC \"\\u4E0B\\u96E8\\u4E86\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("下雨了", ldc.cst);
			} catch(Exception ex) {
				fail(ex);
			}
		}

		@Test
		public void testEscapeNewline() {
			try {
				// Unescape newline
				String s = "DEFINE static x()V\n" +
						"LDC \"New\\nLine\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("New\nLine", ldc.cst);
			} catch(Exception ex) {
				fail(ex);
			}
		}

		@Test
		public void testDontEscapeUnintended() {
			try {
				// Don't attempt to unescape/skip over non-recognized "escapes"
				String s = "DEFINE static x()V\n" +
						"LDC \"Something\\xElse\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("Something\\xElse", ldc.cst);
			} catch(Exception ex) {
				fail(ex);
			}
		}

		@Test
		public void testDontEscapeExisting() {
			try {
				// Don't unescape existing escaped items, like tab
				String s = "DEFINE static x()V\n" +
						"LDC \"\t\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("\t", ldc.cst);
			} catch(Exception ex) {
				fail(ex);
			}
		}

		@Test
		public void testDontEscapeBadUnicodeEscape() {
			try {
				// Unicode unescape need a length of 4:
				// \u0048
				String s = "DEFINE static x()V\n" +
						"LDC \"\\u048\"\n" +
						"POP\n" +
						"RETURN";
				MethodNode mn = compile(Parse.parse(s));
				LdcInsnNode ldc = (LdcInsnNode) mn.instructions.get(0);
				assertEquals("\\u048", ldc.cst);
			} catch(Exception ex) {
				fail(ex);
			}
		}
	}

	@Nested
	public class Variables {
		@Test
		public void testRawIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE 0\n" +
						"ICONST_0\nISTORE 1\n" +
						"ICONST_0\nISTORE 2\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testNamedIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE zero\n" +
						"ICONST_0\nISTORE one\n" +
						"ICONST_0\nISTORE two\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testMixedIndices() {
			try {
				MethodNode node = compile(parse(
						"ICONST_0\nISTORE 0\n" +
						"ICONST_0\nISTORE k\n" +
						"ICONST_0\nISTORE 2\n"));
				assertEquals(0, ((VarInsnNode) node.instructions.get(2)).var);
				assertEquals(1, ((VarInsnNode) node.instructions.get(4)).var);
				assertEquals(2, ((VarInsnNode) node.instructions.get(6)).var);
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}
	}

	@Nested
	public class Verify {
		@Test
		public void testStoreObjInInt() {
			try {
				verifyFails(parse("ACONST_NULL\nISTORE 0\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testStoreIntAsObj() {
			try {
				verifyFails(parse("ICONST_0\nASTORE 0\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testVoidHasObjReturn() {
			try {
				verifyFails(parse("ACONST_NULL\nARETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testReferenceOnPrimitive() {
			try {
				verifyFails(parse("ICONST_0\nINVOKEVIRTUAL owner.name()V\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testStoreDoubleInInt() {
			try {
				verifyFails(parse("DCONST_1\nISTORE test\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testStoreDoubleInInt2() {
			try {
				verifyFails(parse("INVOKESTATIC test.get()D\nISTORE test\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testInvokeOnNull() {
			try {
				verifyFails(parse("ACONST_NULL\nICONST_0\nINVOKEVIRTUAL test.get(I)V\nRETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testPopOnEmpty() {
			try {
				verifyFails(parse("POP\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testPop2On1Size() {
			try {
				verifyFails(parse("ACONST_NULL\nPOP2\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testMissingReturn() {
			try {
				verifyFails(parse("NOP"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testStoreInWideVariableTypesReservedSpace() {
			try {
				// Double takes two spots, 0 and 1
				// Should fail if we try to save to 1
				verifyFails(parse("DCONST_0\nDSTORE 0\nICONST_0\nISTORE 1\nRETURN"));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testPutStaticObjectIntoInt() {
			try {
				String s = "LDC Ljava/lang/System;\n" +
						"LDC \"/logo.png\"\n" +
						"INVOKEVIRTUAL java/lang/Class.getResource(Ljava/lang/String;)Ljava/net/URL;\n" +
						"PUTSTATIC Test.url I\n" +
						"RETURN";
				verifyFails(parse(s));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testPutStaticIntIntoObject() {
			try {
				String s = "BIPUSH 32\n" +
						"PUTSTATIC Test.value Ljava/lang/Object;\n" +
						"RETURN";
				verifyFails(parse(s));
			} catch(AssemblerException ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}
	}

	// =============================================================== //

	private static MethodNode compile(ParseResult<RootAST> result) throws AssemblerException {
		Assembler assembler = new Assembler("Test");
		assembler.setNoVerify(true);
		return assembler.compile(result);
	}

	private static void verifyFails(ParseResult<RootAST> result) throws AssemblerException {
		Assembler assembler = new Assembler("Test");
		try {
			assembler.compile(result);
			fail("Code did not throw any verification exceptions");
		} catch(VerifierException ex) {
			System.err.println(ex.getMessage());
		}
	}

	private static void verifyPass(ParseResult<RootAST> result) {
		Assembler assembler = new Assembler("Test");
		try {
			assembler.compile(result);
		} catch(AssemblerException ex) {
			fail(ex);
		}
	}

	private static ParseResult<RootAST> parse(String code) {
		return Parse.parse(D1 + code + D2);
	}
}
