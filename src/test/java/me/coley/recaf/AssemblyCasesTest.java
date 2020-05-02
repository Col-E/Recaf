package me.coley.recaf;

import me.coley.recaf.parse.bytecode.*;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.parse.bytecode.exception.VerifierException;
import me.coley.recaf.workspace.LazyClasspathResource;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static me.coley.recaf.util.TestUtils.*;

/**
 * More verbose cases for the assembler.
 *
 * @author Matt
 */
public class AssemblyCasesTest {
	private static final String D1 = "DEFINE static func()V\nSTART:\n";
	private static final String D2 = "\nEND:\n";

	@BeforeEach
	public void setup() throws IOException {
		// Set dummy controller/workspace so type analysis works
		setupController(LazyClasspathResource.get());
	}

	@AfterEach
	public void shutdown() {
		removeController();
	}

	@Nested
	public class VerifyPassCases {

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
					"INVOKESTATIC javax/imageio/ImageIO.read(Ljava/net/URL;)Ljava/awt/image/BufferedImage;\n" +
					"PUTSTATIC Test.logo Ljava/awt/image/BufferedImage;\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}

		@Test
		public void testStoreBoolInInt() {
			verifyPass(parse("ICONST_0\nPUTSTATIC Test.boolVal Z\nRETURN"));
		}

		@Test
		public void testPassIntAsBool() {
			verifyPass(parse("ICONST_0\nINVOKESTATIC Test.func(Z)V\nRETURN"));
		}

		@Test
		public void testPrimitiveLongUnknownValue() {
			// Unknown long value from, 'nanoTime()'
			//  - Attempt to do math with value should not fail...
			//  - Instead, "we know the type, but not the value"
			String s = "DEFINE static public time()F\n" +
					"A:\n" +
					"INVOKESTATIC java/lang/System.nanoTime()J\n" +
					"LDC 1000000L\n" +
					"LDIV\n" +
					"L2F\n" +
					"LDC 1000.0F\n" +
					"FDIV\n" +
					"FRETURN";
			verifyPass(parseLit(s));
		}

		@Test
		public void testNullVarDiscoversTypeInControlFlowEdge() {
			verifyPass(parseLit(
					"DEFINE STATIC call()I\n" +
					"START:\n" +
					// Eventual return value
					"ICONST_0\n" +
					// string = null
					"ACONST_NULL\n" +
					"ASTORE string\n" +
					// if (whatever.number != 0) string = targetString
					"GETSTATIC whatever.number I\n" +
					"IFEQ SKIP\n" +
					"GETSTATIC whatever.targetString Ljava/lang/String;\n" +
					// new Result(string)
					"ASTORE string\n" +
					"SKIP:\n" +
					"ALOAD string\n" +
					"PUTSTATIC whatever.output Ljava/lang/String;\n" +
					"IRETURN\n" +
					"END:"
			));
		}
	}

	@Nested
	public class VerifyFailingCases {
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

		@Test
		public void testStaticArgsAndTwoWordStorage() {
			String s = "DEFINE static from(LType; var0, LType; var1)LType;\n" +
					"A:\n" +
					"LCONST_0\n" +
					"LSTORE var2\n" +
					"B:\n" +
					"ACONST_NULL\n" +
					"ARETURN";
			verifyPass(parseLit(s));
		}

		@Test
		public void testReservedVariableIndicesFreeAfterScopeChange() {
			try {
				/*
				 if (String.value != null) {
				     $0 = 0.0D; // also reserves $1
				     $2 = 0.0D; // also reserves $3
				 }
				 $1 = 0;
				 $3 = 0;
				 */
				verifyPass(parse("" +
						"A:\n" +
						"GETSTATIC java/lang/String.value Ljava/lang/String;\n" +
						"IFNULL B\n" +
						"DCONST_0\n" +
						"DSTORE 0\n" +
						"DCONST_0\n" +
						"DSTORE 2\n" +
						"B:\n" +
						"ICONST_1\n" +
						"ISTORE 1\n" +
						"ICONST_3\n" +
						"ISTORE 3\n" +
						"C:\n" +
						"RETURN"));
				/*
				 try {
				     $0 = 0.0D; // also reserves $1
				     $2 = 0.0D; // also reserves $3
				 } catch (Throwable t) {}
				 $1 = 0;
				 $3 = 0;
				 */
				verifyPass(parse("" +
						"TRY EX_START EX_END CATCH(java/lang/Throwable) EX_HANDLER\n" +
						"EX_START:\n" +
						"DCONST_0\n" +
						"DSTORE 0\n" +
						"DCONST_0\n" +
						"DSTORE 2\n" +
						"GOTO EXIT_TRY\n" +
						"EX_END:\n" +
						"EX_HANDLER:\n" +
						"POP\n" +
						"GOTO EXIT_TRY\n" +
						"EXIT_TRY:\n" +
						"ICONST_1\n" +
						"ISTORE 1\n" +
						"ICONST_3\n" +
						"ISTORE 3\n" +
						"C:\n" +
						"RETURN"));
			} catch(Exception ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}
	}

	// =============================================================== //

	private static MethodNode compile(ParseResult<RootAST> result) throws AssemblerException {
		Recaf.getController().config().assembler().verify = false;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController().config().assembler());
		return assembler.compile(result);
	}

	private static void verifyFails(ParseResult<RootAST> result) throws AssemblerException {
		Recaf.getController().config().assembler().verify = true;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController().config().assembler());
		try {
			assembler.compile(result);
			fail("Code did not throw any verification exceptions");
		} catch(VerifierException ex) {
			System.err.println(ex.getMessage());
		}
	}

	private static void verifyPass(ParseResult<RootAST> result) {
		Recaf.getController().config().assembler().verify = true;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController().config().assembler());
		try {
			assembler.compile(result);
		} catch(AssemblerException ex) {
			fail(ex);
		}
	}

	private static ParseResult<RootAST> parse(String code) {
		return Parse.parse(D1 + code + D2);
	}

	private static ParseResult<RootAST> parseLit(String code) {
		return Parse.parse(code);
	}
}
