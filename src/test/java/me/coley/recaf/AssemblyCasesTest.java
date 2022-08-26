package me.coley.recaf;

import me.coley.recaf.parse.bytecode.*;
import me.coley.analysis.value.VirtualValue;
import me.coley.analysis.value.AbstractValue;
import me.coley.recaf.parse.bytecode.ast.RootAST;
import me.coley.recaf.parse.bytecode.exception.AssemblerException;
import me.coley.recaf.parse.bytecode.exception.VerifierException;
import me.coley.recaf.workspace.LazyClasspathResource;
import org.junit.jupiter.api.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Frame;

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
		Recaf.setController(setupController(LazyClasspathResource.get()));
	}

	@AfterEach
	public void shutdown() {
		removeController();
	}

	@Nested
	public class VerifyPassCases {
		@Test
		public void testSharedExceptionHandlerOfDifferentTypes() {
			String s = "DEFINE PRIVATE example()Z\n" +
					"TRY EX_START EX_END CATCH(java/lang/InterruptedException) EX_HANDLER\n" +
					"TRY EX_START EX_END CATCH(java/util/concurrent/TimeoutException) EX_HANDLER\n" +
					"EX_START:\n" +
					"NOP\n" +
					"EX_END:\n" +
					"GOTO EXIT\n" +
					"EX_HANDLER:\n" +
					"ASTORE 1\n" +
					"ICONST_0\n" +
					"IRETURN\n" +
					"EXIT:\n" +
					"ICONST_1\n" +
					"IRETURN";
			verifyPass(Parse.parse(s));
		}

		@Test
		public void testScopedVariableDiffs() {
			String s = "DEFINE public static hi()V\n" +
					"A:\n" +
					"LCONST_1\n" +
					"LSTORE 0\n" +
					"B:\n" +
					"GOTO C\n" +
					"C:\n" +
					"ICONST_1\n" +
					"ISTORE 1\n" +
					"D:\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}

		@Test
		public void testScopedVariableDiffsAlt() {
			String s = "DEFINE public static hi()V\n" +
					"A:\n" +
					"ICONST_1\n" +
					"ISTORE 1\n" +
					"B:\n" +
					"GOTO C\n" +
					"C:\n" +
					"LCONST_1\n" +
					"LSTORE 0\n" +
					"D:\n" +
					"RETURN";
			verifyPass(Parse.parse(s));
		}

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

		@Test
		public void testReservedVariableIndicesFreeAfterScopeChange1() {
			try {
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

		@Test
		public void testReservedVariableIndicesFreeAfterScopeChange2() {
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
			} catch(Exception ex) {
				// Catches "assembler.compile"
				fail(ex);
			}
		}

		@Test
		public void testTypeDiscoveryInTryCatch() {
			verifyPass(parseLit(
					"DEFINE STATIC dump(LMyStream; stream)[B\n"+
					"TRY EX_START EX_END CATCH(java/lang/Throwable) EX_HANDLER\n"+
					"START:\n"+
					// out = null
					"ACONST_NULL\n"+
					"ASTORE out\n"+
					// out=stream.getValue()
					// stream.close()
					"EX_START:\n"+
					"ALOAD stream\n"+
					"INVOKEVIRTUAL MyStream.getValue()[B;\n"+
					"ASTORE out\n"+
					"ALOAD stream\n"+
					"INVOKEVIRTUAL MyStream.close()V\n"+
					"EX_END:\n"+
					"GOTO RET_ADDR\n"+
					// Error
					"EX_HANDLER:\n"+
					"ACONST_NULL\n"+
					"INVOKEVIRTUAL java/lang/Throwable.addSuppressed(Ljava/lang/Throwable;)V\n"+
					"ACONST_NULL\n"+
					"ASTORE out\n"+
					// Load and ret
					"RET_ADDR:\n"+
					"ALOAD out\n"+
					"ARETURN\n"+
					"THE_END:"
			));
		}

		@Test
		public void testTypeDiscoveryInIfStatement() {
			// The "myType" variable should be "Type"
			// But its initially null, so we only assume "Object"
			// It is set to a "Type" in a branch. We want to make sure this knowledge is used.
			String s = "DEFINE static from(LType; var0, LType; var1)LType;\n" +
					"A:\n" +
					"ACONST_NULL\n" +
					"ASTORE myType\n" +
					"ICONST_0\n" +
					"IFEQ B\n" +
					"NEW Type\n" +
					"ASTORE myType\n" +
					"B:\n" +
					"ALOAD myType\n" +
					"ARETURN\n" +
					"C:";
			verifyPass(parseLit(s));
		}
	}

	@Nested
	public class VerifyFailingCases {
		@Test
		public void testScopedVariableDiffs() {
			// This is the same as the passing test case, but without the jump.
			// With no jump, there is no ability for a scope change.
			String s = "DEFINE public static hi()V\n" +
					"A:\n" +
					"LCONST_1\n" +
					"LSTORE 0\n" +
					"B:\n" +
					"ICONST_1\n" +
					"ISTORE 1\n" +
					"C:\n" +
					"RETURN";
			try {
				verifyFails(Parse.parse(s));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

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
				verifyFails(parse(
						"DCONST_1\n" +
						"ISTORE test\n" +
						"RETURN"));
				verifyFails(parse(
						"INVOKESTATIC test.get()D\n" +
						"ISTORE test\n" +
						"RETURN"));
			} catch(AssemblerException ex) {
				fail(ex);
			}
		}

		@Test
		public void testStoreDoubleInInt2() {
			try {
				verifyFails(parse(
						"DCONST_1\n" +
						"ISTORE 0\n" +
						"RETURN"));
				verifyFails(parse(
						"INVOKESTATIC test.get()D\n" +
						"ISTORE 0\n" +
						"RETURN"));
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
	}

	@Nested
	public class InvokeSimulation {
		@Test
		public void testStaticMathCall() {
			int large = 20;
			int small = 5;
			Frame<AbstractValue>[] frames = getFrames(parse(
					"A:\n" +
					"BIPUSH " + large + "\n" +
					"BIPUSH " + small + "\n" +
					"INVOKESTATIC java/lang/Math.min(II)I\n" +
					"ISTORE 0\n" +
					"B:\n" +
					"RETURN"));
			assertEquals(small, frames[frames.length - 2].getLocal(0).getValue());
		}

		@Test
		public void testStringLength() {
			String str = "1234567";
			Frame<AbstractValue>[] frames = getFrames(parse(
					"A:\n" +
					"LDC \"" + str + "\"\n" +
					"INVOKEVIRTUAL java/lang/String.length()I\n" +
					"ISTORE 0\n" +
					"B:\n" +
					"RETURN"));
			assertEquals(str, frames[3].getStack(0).getValue());
			assertEquals(str.length(), frames[4].getStack(0).getValue());
		}

		@Test
		public void testCompilerGeneratedStringBuilder() {
			String part1 = "Hello";
			String part2 = "World";
			Frame<AbstractValue>[] frames = getFrames(parse(
					"A:\n" +
					"LDC \""+ part1 + "\"\n" +
					"ASTORE s\n" +
					"NEW java/lang/StringBuilder\n" +
					"DUP\n" +
					"INVOKESPECIAL java/lang/StringBuilder.<init>()V\n" +
					"ALOAD s\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"LDC \""+ part2 + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.toString()Ljava/lang/String;\n" +
					"ASTORE s\n" +
					"RETURN"));
			assertEquals(part1 + part2, frames[frames.length - 2].getStack(0).getValue());
		}

		@Test
		public void testManualStringBuilder() {
			String part1 = "Hello";
			String part2 = "World";
			Frame<AbstractValue>[] frames = getFrames(parse(
					"A:\n" +
					"NEW java/lang/StringBuilder\n" +
					"DUP\n" +
					"INVOKESPECIAL java/lang/StringBuilder.<init>()V\n" +
					"ASTORE sb\n" +
					"B:\n" +
					"ALOAD sb\n" +
					"LDC \""+ part1 + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"LDC \""+ part2 + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"POP\n" +
					"ALOAD sb\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.toString()Ljava/lang/String;\n" +
					"ASTORE str\n" +
					"RETURN"));
			VirtualValue retFrameLocal = (VirtualValue) frames[frames.length - 2].getLocal(1);
			assertEquals(part1 + part2, retFrameLocal.getValue());
		}

		@Test
		public void testDontMergeWhenScopeChanges() {
			String initial = "INIT";
			String one = "PATH_A";
			String two = "PATH_B";
			Frame<AbstractValue>[] frames = getFrames(parse(
					"NEW java/lang/StringBuilder\n" +
					"DUP\n" +
					"LDC \""+ initial + "\"\n" +
					"INVOKESPECIAL java/lang/StringBuilder.<init>(Ljava/lang/String;)V\n" +
					"ASTORE sb\n" +
					"B:\n" +
					"INVOKESTATIC MyClass.someBool()Z\n" +
					"IFEQ D\n" +
					"C:\n" +
					"ALOAD sb\n" +
					"LDC \""+ one + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"GOTO E\n" +
					"D:\n" +
					"ALOAD sb\n" +
					"LDC \""+ two + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"E:\n" +
					"ALOAD sb\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.toString()Ljava/lang/String;\n" +
					"INVOKESTATIC Logger.print(Ljava/lang/String;)V\n" +
					"RETURN"));
			VirtualValue retFrameLocal = (VirtualValue) frames[frames.length - 2].getLocal(0);
			assertFalse(retFrameLocal.isNull());
			assertFalse(retFrameLocal.isValueUnresolved());
			assertNotEquals(initial, retFrameLocal.getValue());
			assertNotEquals(one, retFrameLocal.getValue());
			assertNotEquals(two, retFrameLocal.getValue());
		}

		@Test
		@Disabled
		// TODO: Manage opaque predicates
		public void testMergeWithOpaquePredicate() {
			String initial = "INIT";
			String one = "PATH_A";
			String two = "PATH_B";
			Frame<AbstractValue>[] frames = getFrames(parse(
					"NEW java/lang/StringBuilder\n" +
					"DUP\n" +
					"LDC \""+ initial + "\"\n" +
					"INVOKESPECIAL java/lang/StringBuilder.<init>(Ljava/lang/String;)V\n" +
					"ASTORE sb\n" +
					"B:\n" +
					"ICONST_0\n" +
					"IFEQ D\n" +
					"C:\n" +
					"ALOAD sb\n" +
					"LDC \""+ one + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"GOTO E\n" +
					"D:\n" +
					"ALOAD sb\n" +
					"LDC \""+ two + "\"\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.append(Ljava/lang/String;)Ljava/lang/StringBuilder;\n" +
					"E:\n" +
					"ALOAD sb\n" +
					"INVOKEVIRTUAL java/lang/StringBuilder.toString()Ljava/lang/String;\n" +
					"INVOKESTATIC Logger.print(Ljava/lang/String;)V\n" +
					"RETURN"));
			VirtualValue retFrameLocal = (VirtualValue) frames[frames.length - 2].getLocal(0);
			assertFalse(retFrameLocal.isNull());
			assertFalse(retFrameLocal.isValueUnresolved());
			assertNotEquals(initial, retFrameLocal.getValue());
			assertNotEquals(one, retFrameLocal.getValue());
			assertEquals(two, retFrameLocal.getValue());
		}

		@Test
		public void testInterfaceMethodRef() {
			try {
				MethodNode method = compile(parse(
						"INVOKESTATIC java/util/function/Function.identity()Ljava/util/function/Function; itf\n" +
							"POP\n" +
							"RETURN"));
				AbstractInsnNode invokeStaticInsn = method.instructions.get(1);
				assertEquals(Opcodes.INVOKESTATIC, invokeStaticInsn.getOpcode());
				assertTrue(((MethodInsnNode) invokeStaticInsn).itf, "INVOKESTATIC instruction must have itf=true");
			} catch (AssemblerException ex) {
				fail(ex);
			}
		}
	}

	// =============================================================== //

	private static MethodNode compile(ParseResult<RootAST> result) throws AssemblerException {
		Recaf.getController().config().assembler().verify = false;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController());
		return assembler.compile(result);
	}

	private static void verifyFails(ParseResult<RootAST> result) throws AssemblerException {
		Recaf.getController().config().assembler().verify = true;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController());
		try {
			assembler.compile(result);
			fail("Code did not throw any verification exceptions");
		} catch(VerifierException ex) {
			System.err.println(ex.getMessage());
		}
	}

	private static void verifyPass(ParseResult<RootAST> result) {
		Recaf.getController().config().assembler().verify = true;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController());
		try {
			assembler.compile(result);
		} catch(AssemblerException ex) {
			fail(ex);
		}
	}

	private static Frame<AbstractValue>[] getFrames(ParseResult<RootAST> result) {
		Recaf.getController().config().assembler().verify = true;
		MethodAssembler assembler = new MethodAssembler("Test", Recaf.getController());
		try {
			assembler.compile(result);
			return assembler.getFrames();
		} catch(AssemblerException ex) {
			fail(ex);
			throw new IllegalStateException(ex);
		}
	}

	private static ParseResult<RootAST> parse(String code) {
		return Parse.parse(D1 + code + D2);
	}

	private static ParseResult<RootAST> parseLit(String code) {
		return Parse.parse(code);
	}
}
