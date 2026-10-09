package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import software.coley.recaf.services.deobfuscation.transform.generic.CallResultInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableTableNormalizingTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationParameters;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An assortment of deobfuscation tests, specifically to ensure weird bugs that occurred at one point do not come back.
 */
public class RegressionDeobfuscationTest extends TransformerTestBase {
	private static final String COUNTER_SOURCE = """
			class Counter {
			    static int count;
			    static int next() { return ++count; }
			}
			""";

	private static final String SIDE_EFFECT_SEQUENCES = """
			.super java/lang/Object
			.class public super Example {
			    .field static keys [I
			    .field static sink I

			    .method static <clinit> ()V {
			        code: {
			        A:
			            iconst_1
			            newarray int
			            putstatic Example.keys [I
			            return
			        B:
			        }
			    }

			    .method static record (I)V {
			        parameters: { value },
			        code: {
			        A:
			            iload value
			            putstatic Example.sink I
			            return
			        B:
			        }
			    }

			    .method static arrayStore ()I {
			        code: {
			        A:
			            iconst_1
			            getstatic Example.keys [I
			            iconst_0
			            bipush 7
			            iastore
			            iconst_2
			            iadd
			            pop
			            getstatic Example.keys [I
			            iconst_0
			            iaload
			            ireturn
			        B:
			        }
			    }

			    .method static fieldStore ()I {
			        code: {
			        A:
			            iconst_1
			            iconst_2
			            bipush 7
			            putstatic Example.sink I
			            iadd
			            pop
			            getstatic Example.sink I
			            ireturn
			        B:
			        }
			    }

			    .method static argumentCall ()I {
			        code: {
			        A:
			            iconst_1
			            iconst_2
			            bipush 7
			            invokestatic Example.record (I)V
			            iadd
			            pop
			            getstatic Example.sink I
			            ireturn
			        B:
			        }
			    }
			}
			""";

	@Test
	void gotoInlining1a() {
		String asm = """
				.method static example ([I)V {
				    parameters: { array },
				    code: {
				    A:
				        goto P
				    B:
				        goto D
				    C:
				        goto Q
				    D:
				        goto F
				    E:
				        goto R
				    F:
				        goto H
				    G:
				        goto S
				    H:
				        goto J
				    I:
				        goto T
				    J:
				        goto L
				    K:
				        goto U
				    L:
				        aload array
				        iload i
				        iaload
				        invokevirtual java/lang/StringBuilder.append (I)Ljava/lang/StringBuilder;
				        ldc " "
				        invokevirtual java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
				        invokevirtual java/lang/StringBuilder.toString ()Ljava/lang/String;
				        invokevirtual java/io/PrintStream.print (Ljava/lang/String;)V
				    M:
				        iinc i 1
				        goto W
				    N:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        invokevirtual java/io/PrintStream.println ()V
				    O:
				        return
				    P:
				        goto C
				    Q:
				        goto E
				    R:
				        goto G
				    S:
				        goto I
				    T:
				        goto K
				    U:
				        aload array
				        arraylength
				        istore length
				    V:
				        iconst_0
				        istore i
				    W:
				        iload i
				        iload length
				        if_icmpge N
				    X:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        new java/lang/StringBuilder
				        dup
				        invokespecial java/lang/StringBuilder.<init> ()V
				        goto B
				    Y:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(GotoInliningTransformer.class, DeadCodeRemovingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("goto", dis), "Only one goto should remain (the while loop handler)");
		});
	}

	/** The same as {@link #gotoInlining1a()} but starting from an easier step. */
	@Test
	void gotoInlining1b() {
		String asm = """
				.method static example ([I)V {
				    parameters: { array },
				    code: {
						  A:
				              goto C
				          B:
				              getstatic java/lang/System.out Ljava/io/PrintStream;
				              invokevirtual java/io/PrintStream.println ()V
				              return
				          C:
				              aload array
				              arraylength
				              istore length
				              iconst_0
				              istore i
				          D:
				              iload i1
				              iload i2
				              if_icmpge B
				              getstatic java/lang/System.out Ljava/io/PrintStream;
				              new java/lang/StringBuilder
				              dup
				              invokespecial java/lang/StringBuilder.<init> ()V
				              aload array
				              iload i1
				              iaload
				              invokevirtual java/lang/StringBuilder.append (I)Ljava/lang/StringBuilder;
				              ldc " "
				              invokevirtual java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
				              invokevirtual java/lang/StringBuilder.toString ()Ljava/lang/String;
				              invokevirtual java/io/PrintStream.print (Ljava/lang/String;)V
				              iinc i1 1
				              goto D
				          E:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(GotoInliningTransformer.class, DeadCodeRemovingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("goto", dis), "Only one goto should remain (the while loop handler)");
		});
	}

	@Test
	void gotoInlining2() {
		String asm = """
				.method public static example ([Ljava/lang/String;)V {
				    parameters: { args },
				    code: {
				    A:
				        getstatic sample/math/BinarySearch.l Z
				        istore i6
				        goto C
				    B:
				        return
				    C:
				        iload i6
				        ifne B
				    D:
				        iload i6
				        ifne B
				        new sample/math/BinarySearch
				        dup
				        invokespecial sample/math/BinarySearch.<init> ()V
				        astore ob
				        iload i6
				        ifne B
				    E:
				        iload i6
				        ifne B
				        iconst_5
				        newarray int
				        dup
				        iconst_0
				        iconst_2
				        iastore
				        dup
				        iconst_1
				        iconst_3
				        iastore
				        dup
				        iconst_2
				        iconst_4
				        iastore
				        dup
				        iconst_3
				        bipush 10
				        iastore
				        dup
				        iconst_4
				        bipush 40
				        iastore
				        astore arr
				        iload i6
				        ifne B
				    F:
				        iload i6
				        ifne B
				        aload arr
				        arraylength
				        istore n
				        iload i6
				        ifne B
				    G:
				        iload i6
				        ifne B
				        bipush 10
				        istore x
				        iload i6
				        ifne B
				    H:
				        iload i6
				        ifne B
				        aload ob
				        aload arr
				        iconst_0
				        iload n
				        iconst_1
				        isub
				        iload x
				        invokevirtual sample/math/BinarySearch.binarySearch ([IIII)I
				        istore result
				        iload i6
				        ifne B
				    I:
				        iload i6
				        ifne B
				        iload result
				        iconst_m1
				        if_icmpne J
				        iload i6
				        ifne B
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        ldc "Element not present"
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        iload i6
				        ifne B
				        goto K
				    J:
				        iload i6
				        ifne B
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        new java/lang/StringBuilder
				        dup
				        invokespecial java/lang/StringBuilder.<init> ()V
				        ldc "Element found at index "
				        invokevirtual java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
				        iload result
				        invokevirtual java/lang/StringBuilder.append (I)Ljava/lang/StringBuilder;
				        invokevirtual java/lang/StringBuilder.toString ()Ljava/lang/String;
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        iload i6
				        ifne B
				    K:
				        // This block should NEVER be inlined because if it were, then we'd have no
				        // terminating instruction at the end of the method.
				        iload i6
				        ifne B
				        return
				        // This dead code can be found as a result of some obfuscators.
				        // It can fool with our analysis of "can we remove this block and not have dangling code?"
				        // so having it here to make sure this still passes is important.
				        // See the internal comments in the goto-inlining transformer for more details.
				        nop
				        athrow
				    L:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(GotoInliningTransformer.class, DeadCodeRemovingTransformer.class), dis -> {
			// Just needs to pass, dead-code remover's analysis process
			// failing would indicate this has failed with an exception
		});
	}

	/**
	 * Bug from improper label visitation tracking in {@link GotoInliningTransformer} due to improper catch block
	 * tracking.
	 */
	@Test
	void gotoInlining3() {
		String asm = """
				.method public execute (Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/client/ResponseHandler;Lorg/apache/http/protocol/HttpContext;)Ljava/lang/Object; {
				    parameters: { this, host, request, handler, context },
				    exceptions: {
				        { B, C, D, Lorg/apache/http/client/ClientProtocolException; },
				        { E, F, G, Ljava/lang/Exception; },
				        { B, C, I, * },
				        { D, J, I, * }
				     },
				    code: {
				    A:
				        aload this
				        aload host
				        aload request
				        aload context
				        invokevirtual org/apache/http/impl/client/CloseableHttpClient.execute (Lorg/apache/http/HttpHost;Lorg/apache/http/HttpRequest;Lorg/apache/http/protocol/HttpContext;)Lorg/apache/http/client/methods/CloseableHttpResponse;
				        astore response
				    B:
				        // try-start:   range=[B-C] handler=D:org/apache/http/client/ClientProtocolException
				        // try-start:   range=[B-C] handler=I:*
				        aload handler
				        aload response
				        invokeinterface org/apache/http/client/ResponseHandler.handleResponse (Lorg/apache/http/HttpResponse;)Ljava/lang/Object;
				        astore protoError
				        aload response
				        invokeinterface org/apache/http/client/methods/CloseableHttpResponse.getEntity ()Lorg/apache/http/HttpEntity;
				        astore entity
				        aload entity
				        invokestatic org/apache/http/util/EntityUtils.consume (Lorg/apache/http/HttpEntity;)V
				        aload protoError
				        astore ex
				    C:
				        // try-end:     range=[B-C] handler=D:org/apache/http/client/ClientProtocolException
				        // try-end:     range=[B-C] handler=I:*
				        aload response
				        invokeinterface org/apache/http/client/methods/CloseableHttpResponse.close ()V
				        aload ex
				        areturn
				    D:
				        // try-handler: range=[B-C] handler=D:org/apache/http/client/ClientProtocolException
				        // try-start:   range=[D-J] handler=I:*
				        astore protoError
				        aload response
				        invokeinterface org/apache/http/client/methods/CloseableHttpResponse.getEntity ()Lorg/apache/http/HttpEntity;
				        astore entity
				    E:
				        // try-start:   range=[E-F] handler=G:java/lang/Exception
				        aload entity
				        invokestatic org/apache/http/util/EntityUtils.consume (Lorg/apache/http/HttpEntity;)V
				    F:
				        // try-end:     range=[E-F] handler=G:java/lang/Exception
				        goto H
				    G:
				        // try-handler: range=[E-F] handler=G:java/lang/Exception
				        astore ex
				    H:
				        aload protoError
				        athrow
				    I:
				        // try-handler: range=[B-C] handler=I:*
				        // try-handler: range=[D-J] handler=I:*
				        astore t
				    J:
				        // try-end:     range=[D-J] handler=I:*
				        aload t
				        athrow
				    K:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(GotoInliningTransformer.class));
	}

	/**
	 * Bug from {@link VariableFoldingTransformer} where {@code IINC} on a variable initially assigned to {@code 0}
	 * wouldn't properly change the tracked state of the variable at the given slot. It usually indicates something
	 * like a for loop counter. We would want to change the state to "unknown" since a loop counter can have more than
	 * a single value during its lifespan in the relevant bytecode ranges.
	 * <br>
	 * If we properly track {@code IINC} handling, this should see no transformations.
	 */
	@Test
	void variableFoldingWithIinc() {
		String asm = """
				.method public equals (Ljava/lang/Object;)Z {
				    parameters: { this, object },
				    code: {
				    A:
				        aload object
				        aload this
				        if_acmpne D
				    B:
				        iconst_1
				        ireturn
				    D:
				        aload object
				        checkcast Example
				        astore that
				    E:
				        aload this
				        invokevirtual Example.size ()I
				        istore size
				    F:
				        aload that
				        invokevirtual Example.size ()I
				        iload size
				        if_icmpeq H
				    G:
				        iconst_0
				        ireturn
				    H:
				        iconst_0
				        istore i
				    I:
				        iload i
				        iload size
				        if_icmpge M
				    J:
				        aload this
				        getfield Example.array [I
				        aload this
				        getfield Example.start I
				        iload i
				        iadd
				        iaload
				        aload that
				        getfield Example.array [I
				        aload that
				        getfield Example.start I
				        iload i
				        iadd
				        iaload
				        if_icmpeq L
				    K:
				        iconst_0
				        ireturn
				    L:
				        iinc i 1
				        goto I
				    M:
				        iconst_1
				        ireturn
				    N:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class));
	}

	/**
	 * Prior transformer behavior would try to fold the first part, but also break the second part by changing
	 * {@code iload} to {@code aload} if the slot was the same. We want to fold the first part without breaking
	 * the second part.
	 */
	@Test
	void variableFoldingKeepsDifferentTypedSlotReuse() {
		String asm = """
				.method public static example (Ljava/lang/Object;)I {
				    parameters: { source },
				    code: {
				    A:
				        aload source
				        astore copy // transformer wants to fold this redundant store to use just 'source'
				        aload copy
				        pop
				    B:
				        // Rewrite 'copy' to hold an int from this point onward.
				        invokestatic Example.getValue ()I
				        istore copy
				    C:
				        // Transformer should keep this as iload as the value of 'copy' is a clearly different type.
				        iload copy
				        ireturn
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertTrue(RegexUtil.matchesAny("iload \\w+\\s+ireturn", dis), "The integer reuse of the copy slot must remain an integer load");
			assertFalse(RegexUtil.matchesAny("aload source\\s+ireturn", dis), "The reference source must not replace the integer result");
		});
	}

	/**
	 * Variant of the above test, main thing we are testing here is that the {@code iinc} doesn't trip up the
	 * tracking of the foldable reference portion at the start.
	 */
	@Test
	void variableFoldingKeepsIincForDifferentTypedSlotReuse() {
		String asm = """
				.method public static example (Ljava/lang/Object;)I {
				    parameters: { source },
				    code: {
				    A:
				        aload source
				        astore copy
				        aload copy
				        pop
				    B:
				        invokestatic Example.getValue ()I
				        istore copy
				    C:
				        iinc copy 1
				        iload copy
				        ireturn
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertTrue(RegexUtil.matchesAny("istore (\\w+)\\s+iinc \\1 1\\s+iload \\1", dis), "The integer increment must remain on the reused slot");
			assertFalse(RegexUtil.matchesAny("iinc i0 1", dis), "The reference source must not receive the integer increment");
		});
	}

	/**
	 * Another variant that allows the first part to be folded, without getting confused by the slot re-assignment later.
	 */
	@Test
	void variableFoldingResetsAfterSlotDomination() {
		String asm = """
				.method public static example (Ljava/lang/Object;)Ljava/lang/Object; {
				    parameters: { source },
				    code: {
				    A:
				        aload source
				        astore copy
				        aload copy // <-- Can be 'source'
				        pop
				    B:
				        invokestatic Example.getValue ()Ljava/lang/Object;
				        astore copy
				    C:
				        aload copy // <-- Must remain as 'copy'
				        areturn
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			// The first copy is redundant and should be replaced with direct source loads.
			assertTrue(RegexUtil.matchesAny("aload source\\s+pop\\s+aload source\\s+pop", dis), "The copy should fold before the later overwrite");

			// The later definition must remain the value read by the return path.
			assertTrue(RegexUtil.matchesAny("invokestatic Example.getValue \\(\\)Ljava/lang/Object;\\s+astore \\w+\\s+aload \\w+\\s+areturn", dis), "The later slot definition and read must remain");
			assertFalse(RegexUtil.matchesAny("invokestatic Example.getValue \\(\\)Ljava/lang/Object;\\s+astore \\w+\\s+aload source\\s+areturn", dis), "The later slot definition must not be replaced with the original source");
		});
	}

	/**
	 * Illegal code that writes to wide-reserved slots shouldn't trigger transformations.
	 */
	@Test
	void variableFoldingRejectsOverlappingWideSourceWrite() {
		putClass(CLASS_NAME, node -> {
			// Pseudocode: long example(long p0)
			//    long v2 = p0;
			//    int i1 = getValue(); // Illegal store to reserved slot 1 (overlaps with reserved slot of p0)
			//    i1;
			//    return v2;
			MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "example", "(J)J", null, null);
			method.instructions.add(new VarInsnNode(Opcodes.LLOAD, 0));
			method.instructions.add(new VarInsnNode(Opcodes.LSTORE, 2));
			method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CLASS_NAME, "getValue", "()I", false));
			method.instructions.add(new VarInsnNode(Opcodes.ISTORE, 1));
			method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
			method.instructions.add(new InsnNode(Opcodes.POP));
			method.instructions.add(new VarInsnNode(Opcodes.LLOAD, 2));
			method.instructions.add(new InsnNode(Opcodes.LRETURN));
			method.maxStack = 2;
			method.maxLocals = 4;
			node.methods.add(method);
		});
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier().transformJvm(List.of(VariableFoldingTransformer.class)));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertTrue(result.getTransformedClasses().isEmpty(), "The overlapping source write must prevent copy folding");
	}

	@Test
	void callInliningOfUnhandledCoreJdkClassesDoesNotFail() {
		// Unknown values like 'Thread.currentThread()' shouldn't throw exceptions.
		// Instead, nothing should happen at all here.
		String asm = """
				.method public test ()I {
				    parameters: { this },
				    code: {
				    A:
				        invokestatic java/lang/Thread.currentThread ()Ljava/lang/Thread;
				        invokevirtual java/lang/Object.hashCode ()I
				        ireturn
				    B:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(CallResultInliningTransformer.class));
	}

	/**
	 * This case has "xyz" always be "1" when read, so it should fold.
	 */
	@Test
	void variableFoldingWithCatchThatHasNoThrowableContents() {
		String asm = """
				.method public example ()I {
				    parameters: { this },
				    exceptions: { { B, C, D, Ljava/lang/Throwable; } },
				    code: {
				    A:
				        // Redundant first assignment
				        iconst_0
				        istore xyz
				    B:
				        // Will always overwrite, throwing behavior impossible
				        iconst_1
				        istore xyz
				    C:
				        goto E
				    D:
				        // Handler, not clear what should be folded here since the try block can't throw anything
				        // making it impossible to actually end up here.
				        pop
				        iload xyz
				        ireturn
				    E:
				        // Should always be "1" here since the try block can't throw anything
				        iload xyz
				        ireturn
				    F:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertFalse(RegexUtil.matchesAny("iconst_0\\s+istore", dis), "Redundant var store should be folded");
			assertTrue(RegexUtil.matchesAny("iconst_1\\s+istore", dis), "Final var store should be kept");
			assertTrue(RegexUtil.matchesAny("E:\\s+iconst_1\\s+ireturn", dis), "E block should fold to iconst_1");
			assertTrue(RegexUtil.matchesAny("D:\\s+//.+\\s+pop\\s+iload xyz\\s+ireturn", dis), "D block shouldn't fold since it's technically possible to end up here, even if it can't actually happen in practice");
		});
	}

	/**
	 * This case has "xyz" change inside a "try" block, with a possibly throwing
	 * method before the new value is assigned. Depending on if the called method
	 * throws or not, the "xyz" value will be changed.
	 */
	@Test
	void variableFoldingWithCatchThatCanThrow() {
		String asm = """
				.method public example ()I {
				    parameters: { this },
				    exceptions: { { B, C, D, Ljava/lang/Throwable; } },
				    code: {
				    A:
				        iconst_0
				        istore xyz
				    B:
				        // Method can throw, so "xyz" later on can be either "0" or "1"
				        invokestatic Example.throwError ()V
				        iconst_1
				        istore xyz
				    C:
				        goto E
				    D:
				        // handler, pop exception off stack
				        pop
				        // In this basic case, this should always be "0"
				        iload xyz
				        ireturn
				    E:
				        // In this basic case, this should always be "1"
				        iload xyz
				        ireturn
				    F:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertTrue(RegexUtil.matchesAny("iconst_0\\s+istore", dis), "Both var stores should be kept");
			assertTrue(RegexUtil.matchesAny("iconst_1\\s+istore", dis), "Both var stores should be kept");
			assertTrue(RegexUtil.matchesAny("E:\\s+iconst_1\\s+ireturn", dis), "E block should fold to iconst_1");
			assertTrue(RegexUtil.matchesAny("D:\\s+//.+\\s+pop\\s+iload xyz\\s+ireturn", dis), "D block shouldn't fold since it can be either 0 or 1");
		});
	}

	/**
	 * Advanced case of {@link #variableFoldingWithCatchThatCanThrow()}.
	 * <br>
	 * Multiple calls to throwing methods, with variable assignments after.
	 */
	@Test
	void variableFoldingWithCatchThatCanThrowAdvanced() {
		String asm = """
				.method public example ()I {
				    parameters: { this },
				    exceptions: { { B, C, D, Ljava/lang/Throwable; } },
				    code: {
				    A:
				        iconst_0
				        istore xyz
				    B:
				        // Method can throw, so "xyz" later on can be any "0-3" value.
				        invokestatic Example.throwError1 ()V
				        iconst_1
				        istore xyz
				        invokestatic Example.throwError2 ()V
				        iconst_2
				        istore xyz
				        invokestatic Example.throwError3 ()V
				        iconst_3
				        istore xyz
				    C:
				        goto E
				    D:
				        // handler, pop exception off stack
				        pop
				        // In this advanced case, this should be "0", "1", or "2"
				        iload xyz
				        ireturn
				    E:
				        // In this advanced case, this should always be "3"
				        iload xyz
				        ireturn
				    F:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertTrue(RegexUtil.matchesAny("iconst_0\\s+istore", dis), "All var stores should be kept");
			assertTrue(RegexUtil.matchesAny("iconst_1\\s+istore", dis), "All var stores should be kept");
			assertTrue(RegexUtil.matchesAny("iconst_2\\s+istore", dis), "All var stores should be kept");
			assertTrue(RegexUtil.matchesAny("iconst_3\\s+istore", dis), "All var stores should be kept");
			assertTrue(RegexUtil.matchesAny("E:\\s+iconst_3\\s+ireturn", dis), "E block should fold to iconst_3");
			assertTrue(RegexUtil.matchesAny("D:\\s+//.+\\s+pop\\s+iload xyz\\s+ireturn", dis), "D block shouldn't fold since it can be 0, 1, or 2");
		});
	}

	/**
	 * If {@link ReInterpreter#merge(ReValue, ReValue)} is implemented incorrectly, some transformers relying on
	 * frame analysis with {@link ReValue} contents may incorrectly assume they should make optimizations.
	 * <p>
	 * This example shows an if-else control flow based on some value stored in a local variable. That switch key
	 * is unknown at the start, so any branch taken can modify the local variable state. This means we should
	 * end up at the if-else control flow with an unknown value <i>(Or at the very least, not a single known value)</i>.
	 * Because of this, no specific path in this if-else should be optimized away.
	 * <p>
	 * If we see any transforms take place here, something is broken.
	 */
	@Test
	void frameMergeIncorrectlyLeadsToImproperOptimization() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        getstatic Example.key I
				        lookupswitch {
						    1: B,
						    2: C,
						    3: D,
						    default: E
						}
				    B:
				        iconst_0
				        istore foo
				        goto F
				    C:
				        iconst_1
				        istore foo
				        goto F
				    D:
				        iconst_2
				        istore foo
				        goto F
				    E:
				        iconst_3
				        istore foo
				        goto F
				    F:
				        iload foo
				        ifeq G
				        invokestatic Example.nonzero ()V
				        return
				    G:
				        invokestatic Example.zero ()V
				        return
				    Z:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class, OpaquePredicateFoldingTransformer.class));
	}

	@Test
	void multiStepInteractionsOfVariousFoldingTransformers() {
		String asm = """
				.method public example ()Ljava/lang/String; {
				    parameters: { this },
				    exceptions: {
				        { E, F, G, Ljava/lang/RuntimeException; },
				        { G, H, G, Ljava/lang/RuntimeException; }
				     },
				    code: {
				    A:
				        sipush 6197
				        sipush 1729
				        iand
				        tableswitch {
				            min: 0,
				            max: 2,
				            cases: { C, D, B },
				            default: B
				        }
				    B:
				        aconst_null
				        athrow
				    C:
				        aconst_null
				        athrow
				    D:
				        goto E
				    E:
				        ldc 191946085
				        ldc 191943797
				        iand
				        aconst_null
				        ifnull I
				        aconst_null
				        athrow
				    F:
				        nop
				        nop
				        athrow
				    G:
				        dup
				        invokevirtual java/lang/RuntimeException.printStackTrace ()V
				        checkcast java/lang/Throwable
				        athrow
				    H:
				        nop
				        nop
				        athrow
				    I:
				        lookupswitch {
				            191943781: K,
				            1241362125: J,
				            default: J
				        }
				    J:
				        aconst_null
				        athrow
				    K:
				        goto L
				    L:
				        aload this
				        getfield Foo.value Ljava/lang/String;
				        areturn
				    M:
				    }
				}
				""";
		// This sample does not work in a single pass, it needs multiple to properly fold everything.
		// Any breakage in one transformer will affect the others.
		validateAfterRepeatedAssembly(asm, List.of(
				RedundantTryCatchRemovingTransformer.class,
				OpaqueConstantFoldingTransformer.class,
				GotoInliningTransformer.class,
				OpaquePredicateFoldingTransformer.class
		), dis -> {
			assertEquals(0, StringUtil.count("goto", dis), "All goto instructions should be inlined");
			assertEquals(0, StringUtil.count("switch", dis), "All switch instructions should be inlined");
			assertEquals(0, StringUtil.count("athrow", dis), "All athrow instructions should removed");
			assertEquals(0, StringUtil.count("aconst_null", dis), "All athrow instructions should removed");
			assertEquals(0, StringUtil.count("ldc", dis), "All ldc instructions should removed");
		});
	}

	@Test
	void singleStackNoLocalsDoesNotAIOOBEConstantFolding() {
		String asm = """
				.method public static example ()Ljava/lang/Object; {
				    code: {
				    A:
				        getstatic Foo.foo I
				        newarray int
				        areturn
				    B:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(OpaqueConstantFoldingTransformer.class));
	}

	/**
	 * The first pass of the constant folding transformer should let us know what the final value is.
	 * Everything else should facilitate cleanup. The arrays are referenced a lot due to how arrays are
	 * populated in Java. It's not like Dalvik where you can just push a fully constructed array in one go.
	 * Because of this, we need to detect that these reads/writes are not actually useful and clean up where
	 * necessary.
	 *
	 * @see #constFoldingAndVariableFoldingWillNotAggressivelyRemoveArrayConstructionsForArraysUsedElsewhere()
	 */
	@Test
	void constFoldingAndVariableFoldingWillEventuallyCleanUpNoLongerUsedArrays() {
		String asm = """
				.method public static example ()I {
				     code: {
				     A:
				         iconst_5
				         newarray int
				         astore a
				     B:
				         aload a
				         iconst_0
				         iconst_1
				         iastore
				     C:
				         aload a
				         iconst_1
				         bipush 10
				         iastore
				     D:
				         aload a
				         iconst_2
				         bipush 100
				         iastore
				     E:
				         aload a
				         iconst_3
				         sipush 1000
				         iastore
				     F:
				         aload a
				         iconst_4
				         sipush 10000
				         iastore
				     G:
				         iconst_5
				         newarray int
				         astore b
				     H:
				         iconst_2
				         aload a
				         aload b
				         arraylength
				         iconst_2
				         iushr
				         iaload
				         aload a
				         arraylength
				         idiv
				         imul
				         newarray int
				         astore c
				     I:
				         aload c
				         arraylength
				         iconst_1
				         iushr
				         newarray int
				         astore b
				     J:
				         aload b
				         aload c
				         iconst_0
				         iaload
				         aload a
				         iconst_3
				         iaload
				         aload c
				         arraylength
				         iushr
				         iastore
				     K:
				         aload b
				         aload a
				         iconst_0
				         iaload
				         aload b
				         arraylength
				         iconst_2
				         ishl
				         aload b
				         aload a
				         iconst_0
				         iaload
				         iconst_1
				         iushr
				         iaload
				         iadd
				         aload c
				         arraylength
				         ishl
				         iconst_2
				         ishr
				         iastore
				     L:
				         aload c
				         aload a
				         iconst_1
				         iaload
				         bipush 10
				         newarray int
				         arraylength
				         idiv
				         aload a
				         iconst_4
				         iaload
				         iastore
				     M:
				         aload c
				         iconst_0
				         aload c
				         iconst_1
				         iaload
				         iconst_4
				         ishr
				         iastore
				     N:
				         aload c
				         iconst_3
				         aload a
				         iconst_2
				         newarray int
				         arraylength
				         iaload
				         bipush 10
				         imul
				         iastore
				     O:
				         aload c
				         iconst_2
				         aload c
				         iconst_3
				         iaload
				         iconst_1
				         ishr
				         iastore
				     P:
				         aload a
				         iconst_0
				         iaload
				         istore a0
				     Q:
				         aload a
				         iconst_1
				         iaload
				         istore a1
				     R:
				         aload a
				         iconst_2
				         iaload
				         istore a2
				     S:
				         aload a
				         iconst_3
				         iaload
				         istore a3
				     T:
				         aload a
				         iconst_4
				         iaload
				         istore a4
				     U:
				         aload b
				         iconst_0
				         iaload
				         istore b0
				     V:
				         aload b
				         iconst_1
				         iaload
				         istore b1
				     W:
				         aload c
				         iconst_0
				         iaload
				         istore c0
				     X:
				         aload c
				         iconst_1
				         iaload
				         istore c1
				     Y:
				         aload c
				         iconst_2
				         iaload
				         istore c2
				     Z:
				         aload c
				         iconst_3
				         iaload
				         istore c3
				     AA:
				         bipush 11
				         newarray int
				         dup
				         iconst_0
				         iload a0
				         iastore
				         dup
				         iconst_1
				         iload a1
				         iastore
				         dup
				         iconst_2
				         iload a2
				         iastore
				         dup
				         iconst_3
				         iload a3
				         iastore
				         dup
				         iconst_4
				         iload a4
				         iastore
				         dup
				         iconst_5
				         iload b0
				         iastore
				         dup
				         bipush 6
				         iload b1
				         iastore
				         dup
				         bipush 7
				         iload c0
				         iastore
				         dup
				         bipush 8
				         iload c1
				         iastore
				         dup
				         bipush 9
				         iload c2
				         iastore
				         dup
				         bipush 10
				         iload c3
				         iastore
				         astore d
				     AB:
				         aload d
				         invokestatic java/util/Arrays.hashCode ([I)I
				         istore h
				     AC:
				         iload h
				     AD:
				         // -1239995153
				         ireturn
				     AE:
				     }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class, VariableFoldingTransformer.class), dis -> {
			// Final result
			assertTrue(dis.contains("ldc -1239995153"), "Failed to fold sequence");
			// All we should have is just the final value and a return
			assertFalse(dis.contains("iload"), "Failed to fold sequence");
			assertFalse(dis.contains("istore"), "Failed to fold sequence");
			assertFalse(dis.contains("iastore"), "Failed to fold sequence");
			assertFalse(dis.contains("iaload"), "Failed to fold sequence");
			assertFalse(dis.contains("invoke"), "Failed to fold sequence");
			assertFalse(dis.contains("dup"), "Failed to fold sequence");
			assertFalse(dis.contains("pop"), "Failed to fold sequence");
		});
	}

	/**
	 * Unlike {@link #constFoldingAndVariableFoldingWillEventuallyCleanUpNoLongerUsedArrays()} the array built here
	 * is passed to some other method, so we need to keep the whole thing around.
	 */
	@Test
	void constFoldingAndVariableFoldingWillNotAggressivelyRemoveArrayConstructionsForArraysUsedElsewhere() {
		String asm = """
				.method public static example ()I {
				     code: {
				     A:
				         iconst_2
				         newarray int
				         astore a
				     B:
				         aload a
				         iconst_0
				         iconst_5
				         iastore
				     C:
				         aload a
				         iconst_1
				         iconst_5
				         iastore
				     D:
				         // This should be folded to 10
				         aload a
				         dup
				         iconst_0
				         iaload
				         aload a
				         iconst_1
				         iaload
				         iadd
				     E:
				         // This takes '10' and 'int[] a'
				         // The array should not be deleted since it is used as a parameter here.
				         invokestatic Example.consume (I[I)I
				         ireturn
				     F:
				     }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class, VariableFoldingTransformer.class), dis -> {
			// The foldable part
			assertTrue(dis.contains("bipush 10"), "Failed to fold sequence");
			assertFalse(dis.contains("iaload"), "Failed to fold sequence");

			// Keep the array construction parts
			assertTrue(dis.contains("iconst_5"), "Should keep array value assignments");
			assertTrue(dis.contains("iastore"), "Should keep array value assignments");
		});
	}

	/**
	 * This got addressed by not using {@link AsmInsnUtil#getSizeConsumed(AbstractInsnNode)} and
	 * {@link AsmInsnUtil#getSizeProduced(AbstractInsnNode)} by default for computing foldable sequences
	 * in {@link OpaqueConstantFoldingTransformer}. Instead, we do a simple 'this' and 'next' stack size
	 * diff via {@link org.objectweb.asm.tree.analysis.Frame#getStackSize()}.
	 */
	@Test
	void i2lConfusesConstantFoldingStackBalanceAndSkipsFoldableSequence() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        invokestatic Example.foo ()V
				        iconst_0
				        // Begin foldable
				        ldc -114812231
				        i2l
				        ldc -8456834448885618340L
				        lxor
				        // End foldable
				        invokestatic Example.bar (IJ)I
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertFalse(dis.contains("ldc -114812231"), "Failed to fold sequence");
			assertFalse(dis.contains("i2l"), "Failed to fold sequence");
			assertFalse(dis.contains("ldc -8456834448885618340L"), "Failed to fold sequence");
			assertFalse(dis.contains("lxor"), "Failed to fold sequence");
			assertTrue(dis.contains("ldc 8456834448930567141L"), "Failed to fold sequence into expected value");
		});
	}

	/**
	 * This example shows how <i>"Get the value from the next frame"</i> can fail.
	 * This was fixed by using a fallback computation when the next frame's value is unknown for a given
	 * sequence of foldable instructions.
	 */
	@Test
	void backwardsJumpConfusesConstantFoldingKnownStackReplacement() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        bipush 50
				        goto C
				    B:
				        // Begin foldable --> 30
				        bipush 15
				        bipush 15
				        iadd
				        // End foldable
				    C:
				        // Stack top is either 50 or 30 depending on where we came from
				        //
				        // Begin foldable --> 10
				        iconst_5
				        iconst_5
				        iadd
				        iadd
				        // End foldable
				        //
				        // This backwards jump to C creates a scenario where we revisit C.
				        // - A will jump to C with 50 on the stack.
				        // - B will naturally flow into C with 30 on the stack.
				        // - This jumps back to B so the control flow over C has two possible stack top values
				        lookupswitch {
						    55: B,
						    default: D
						}
					D:
				        return
				    E:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertFalse(dis.contains("iconst_5"), "Failed to fold easy sequence"); // The easy case
			assertFalse(dis.contains("bipush 15"), "Failed to fold edge sequence"); // The edge case
			assertTrue(dis.contains("bipush 10"), "Failed to fold easy-case sequence into expected value");
			assertTrue(dis.contains("bipush 30"), "Failed to fold edge-case sequence into expected value");
		});
	}

	/**
	 * A backwards jump can merge an unknown value into a foldable sequence's successor frame,
	 * triggering the sequence fallback evaluation. When the instruction immediately before the
	 * sequence consumes a stack value, the sequence entry frame must be taken from the sequence's
	 * first instruction, not the instruction before it. Otherwise, the fallback evaluation starts
	 * from an inflated stack and overflows the method's max stack.
	 */
	@Test
	void constantFoldingSequencePrecededByPopDoesNotOverflowMaxStack() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_1
				        goto C
				    B:
				        pop
				        bipush 15
				        bipush 15
				        iadd
				    C:
				        dup
				        lookupswitch {
				            1: B,
				            default: D
				        }
				    D:
				        pop
				        return
				    E:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertFalse(dis.contains("bipush 15"), "Failed to fold edge sequence");
			assertTrue(dis.contains("bipush 30"), "Failed to fold edge sequence into expected value");
		});
	}

	/**
	 * A snippet of DashO obfuscation. Includes a series of opaque jumps with some loop-backs in dead code.
	 * There's also sequences that set up the stack before those jumps which also have variable state side effects.
	 * These two issues have caused issues with opaque folding transformers in the past.
	 */
	@Test
	void dashFlow() {
		String asm = """
				.method private static example ()Ljava/lang/String; {
				    code: {
				    A:
				        ldc "example-text-to-decrypt"
				        bipush 15
				        goto P
				    B:
				        goto C
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    C:
				        iadd
				        idiv
				        iinc i0 14
				        ldc "22"
				        astore v1
				        goto AB
				    D:
				        goto E
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    E:
				        iconst_4
				        dup
				        iconst_0
				        istore i0
				        ldc "0"
				        astore v1
				        goto M
				    F:
				        goto G
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    G:
				        iinc i0 6
				        iconst_0
				        iconst_0
				        goto T
				    H:
				        goto I
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    I:
				        iinc i0 15
				        pop
				        iconst_1
				        goto R
				    J:
				        goto K
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    K:
				        iadd
				        iadd
				        iinc i0 8
				        ldc "22"
				        astore v1
				        goto S
				    L:
				        aload v1
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { U },
				            default: H
				        }
				    M:
				        aload v1
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { J },
				            default: AA
				        }
				    N:
				        goto O
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    O:
				        bipush 15
				        dup
				        iconst_0
				        istore i0
				        ldc "0"
				        astore v1
				        goto T
				    P:
				        goto Q
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    Q:
				        iconst_m1
				        istore i0
				        ldc "0"
				        iinc i0 1
				        astore v1
				        goto L
				    R:
				        iload i0
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { W },
				            default: D
				        }
				    S:
				        iload i0
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { F },
				            default: N
				        }
				    T:
				        aload v1
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { B },
				            default: Y
				        }
				    U:
				        goto V
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    V:
				        bipush 11
				        imul
				        iinc i0 5
				        ldc "22"
				        astore v1
				        goto R
				    W:
				        goto X
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    X:
				        iinc i0 9
				        iconst_1
				        iconst_1
				        goto M
				    Y:
				        goto Z
				        athrow
				        nop
				        nop
				        athrow
				        athrow
				    Z:
				        iinc i0 13
				        pop
				        pop
				        goto AB
				    AA:
				        iinc i0 8
				        pop
				        pop
				        goto S
				    AB:
				        invokestatic Example.decrypt (Ljava/lang/String;I)Ljava/lang/String;
				        nop
				        areturn
				    AC:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(
				GotoInliningTransformer.class,
				VariableFoldingTransformer.class,
				OpaqueConstantFoldingTransformer.class,
				OpaquePredicateFoldingTransformer.class
		), dis -> {
			// Flow should be removed
			assertFalse(dis.contains("goto"));
			assertFalse(dis.contains("switch"));
			assertFalse(dis.contains("if"));

			// Variable reads/writes should be removed
			assertFalse(dis.contains("iinc"));
			assertFalse(dis.contains("store"));
			assertFalse(dis.contains("load"));

			// It should be just:
			//   return decrypt(const-text, const-key);
			assertFalse(dis.contains("C:"), "Method should be simplified enough to fit between two labels [A-B]");
		});
	}

	/**
	 * A mocked up sample that has been hand-crafted to mimic DashO.
	 * The block of code in 'A' before the switch modifies state that is required later for proper folding.
	 * This test ensures transformers don't overly optimize earlier code that can prevent later sections
	 * from being properly folded.
	 */
	@Test
	void mockDashFlow() {
		String asm = """
				.method private static example (I)Ljava/lang/String; {
					parameters: { foo },
				    code: {
				    A:
				        iconst_0
				        istore i
				        ldc "0"
				        iconst_1
				        istore foo
				        iinc i 1
				        astore s
				        aload s
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { B },
				            default: X
				        }
				    B:
				        iload foo
				        ifeq X
				    C:
				        iload i
				        tableswitch {
				            min: 0,
				            max: 0,
				            cases: { X },
				            default: D
				        }
				    D:
				        ldc "win"
				        areturn
				    X:
				        iinc foo 1
				        iload foo
				        ifne B
				        ldc "fail"
				        areturn
				    Z:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(
				GotoInliningTransformer.class,
				VariableFoldingTransformer.class,
				OpaqueConstantFoldingTransformer.class,
				OpaquePredicateFoldingTransformer.class
		), dis -> {
			assertFalse(dis.contains("switch"));
			assertFalse(dis.contains("iinc"));
		});
	}

	@Test
	void analyzerRequeuesReachableBranchTargets() {
		// When we use ReAnalyzer/ReFrame we need to ensure that the first branching behavior to some destination frame
		// does not prevent a later reachable branch from re-analyzing that destination frame. In the past when this
		// bug was present, the first branch would mark 'TARGET' as unreachable, and any transformer would ignore
		// the contents of the TARGET block.
		String asm = """
				.super java/lang/Object
				.class Example {
					.method static <clinit> ()V {
					    code: {
					    ENTRY:
					        // This first branch is opaque, and indicate TARGET is unreachable from ENTRY.
					        iconst_1
					        iconst_0
					        if_icmpgt PATH_A // GOTO PATH_A
					    TARGET:
					        invokestatic Example.value ()Ljava/lang/String;
					        pop
					        return
					    PATH_A:
					        // This second branch is also opaque, but indicates TARGET is reachable from PATH_A.
					        iconst_1
					        iconst_0
					        if_icmpgt TARGET // GOTO TARGET
					        return
					    }
					}
					.method static value ()Ljava/lang/String; {
					    code: {
					    A:
					        ldc "reachable"
					        areturn
					    B:
					    }
					}
				}
				""";
		assemble(asm, true);

		TransformationParameters parameters = new TransformationParameters(Map.of(
				CallResultInliningTransformer.KEY_EVALUATE_CLASS_INITIALIZERS, true));
		JvmTransformResult result = assertDoesNotThrow(() ->
				newApplier().transformJvm(List.of(CallResultInliningTransformer.class), parameters));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");

		String disassembly = disassembleTransformed(result, true);
		assertEquals(0, StringUtil.count("invokestatic Example.value", disassembly),
				"A later reachable edge must requeue the previously pruned target");
		assertEquals(2, StringUtil.count("ldc \"reachable\"", disassembly),
				"The reachable initializer call must be replaced with its exact value");
	}

	/**
	 * Ensures the const-folder doesn't take too long to run.
	 */
	@Test
	@Timeout(value = 2)
	@DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
	void constFolderDoesNotCatestrophicallyBacktrack() {
		String asm = """
				.method public static example (I)I {
					parameters: { foo },
				    code: {
				    A:
				        iload foo
				\0
				        ireturn
				    E:
				    }
				}
				""".replace("\0", "        iconst_1\n        iadd\n".repeat(20_000));
		validateNoTransformation(asm, List.of(OpaqueConstantFoldingTransformer.class));
	}

	/**
	 * Folding a sequence that contains a variable store should retain side effects.
	 * Previously we incorrectly read the operation's top stack operand instead,
	 * which rewrote {@code tmp} from {@code 7} to {@code 3}.
	 */
	@Test
	void foldSequenceWithVarStorePreservesStoredValue() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_1
				        bipush 7
				        istore tmp // tmp = 7
				        iconst_3
				        iadd // 1 + 3
				        iload tmp
				        iadd // 7 + 4
				        ireturn
				    B:
				    }
				}
				""";

		// tmp must remain 7, keeping the method's return value at 1 + 3 + 7 = 11.
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "tmp = 7", "tmp = 7");
	}

	/**
	 * We previously read the local from the operation frame and double-applied the increment, turning {@code x += 5} into 10.
	 */
	@Test
	void foldSequenceWithIincPreservesIncrementedValue() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_0
				        istore x // x = 0
				        iconst_1
				        iconst_2
				        iadd // 1 + 2
				        iinc x 5 // x += 5
				        iload x
				        iadd // 5 + 3
				        iload x
				        iadd // 5 + 8
				        ireturn
				    B:
				    }
				}
				""";
		// x must be 5 after folding, keeping the method's return value at 1 + 2 + 5 + 5 = 13.
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "x += 5", "x = 5");
	}

	/**
	 * When a two-argument predicate compares values created by {@code dup2}, only the {@code dup2}
	 * itself is removed. The two originals it duplicated remain on the stack for the branch body.
	 */
	@Test
	void foldOpaqueDup2Predicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        dup2
				        if_icmpne C
				    B:
				        pop2
				        return
				    C:
				        pop2
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			// The "0 != 1" predicate folds into an unconditional goto.
			assertEquals(0, StringUtil.count("if_icmpne", dis), "Expected to remove if_icmpne");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace if_icmpne <target> with goto <target>");

			// Only the dup2 is removed; the two originals it duplicated remain for the branch body.
			assertEquals(0, StringUtil.count("dup2", dis), "Expected to remove dup2");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep the first original value");
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to keep the second original value");

			// The unreachable fall-through branch is pruned, leaving a single pop2.
			assertEquals(1, StringUtil.count("pop2", dis), "Expected to prune the dead fall-through branch");
		});
	}

	/**
	 * Object comparisons backed by {@code dup2} keep the two duplicated originals as well.
	 */
	@Test
	void foldOpaqueDup2AcmpPredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        aconst_null
				        dup2
				        if_acmpeq C
				    B:
				        pop2
				        return
				    C:
				        pop2
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_acmpeq", dis), "Expected to remove if_acmpeq");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace if_acmpeq <target> with goto <target>");
			assertEquals(0, StringUtil.count("dup2", dis), "Expected to remove dup2");
			assertEquals(2, StringUtil.count("aconst_null", dis), "Expected to keep the two duplicated originals");
			assertEquals(1, StringUtil.count("pop2", dis), "Expected to prune the dead fall-through branch");
		});
	}

	/**
	 * A single-value predicate above {@code dup2} keeps the {@code dup2} and pops the copy the jump
	 * no longer consumes, so the two originals plus the surviving copy remain for the branch body.
	 */
	@Test
	void foldOpaqueDup2SingleValuePredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        dup2
				        ifne C
				    B:
				        pop
				        pop
				        pop
				        return
				    C:
				        pop
				        pop
				        pop
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifne", dis), "Expected to remove ifne");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace ifne <target> with goto <target>");
			assertEquals(1, StringUtil.count("dup2", dis), "Expected to keep the dup2");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep the first original value");
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to keep the second original value");
			assertEquals(4, StringUtil.count("pop", dis), "Expected the surviving copy to be popped");
		});
	}

	/**
	 * Object single-value predicates backed by {@code dup2} keep the two duplicated originals and
	 * pop the surviving copy, mirroring the primitive case.
	 */
	@Test
	void foldOpaqueDup2NullSingleValuePredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        aconst_null
				        dup2
				        ifnull C
				    B:
				        pop
				        pop
				        pop
				        return
				    C:
				        pop
				        pop
				        pop
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifnull", dis), "Expected to remove ifnull");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace ifnull <target> with goto <target>");
			assertEquals(1, StringUtil.count("dup2", dis), "Expected to keep the dup2");
			assertEquals(2, StringUtil.count("aconst_null", dis), "Expected to keep the two duplicated originals");
			assertEquals(4, StringUtil.count("pop", dis), "Expected the surviving copy to be popped");
		});
	}

	/**
	 * A switch selector produced by {@code dup2} keeps the {@code dup2} and pops the selector copy
	 * the switch no longer consumes.
	 */
	@Test
	void foldOpaqueDup2SwitchPredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        dup2
				        lookupswitch {
				            0: B,
				            1: C,
				            default: B
				        }
				    B:
				        pop
				        pop
				        pop
				        return
				    C:
				        pop
				        pop
				        pop
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("lookupswitch", dis), "Expected to remove lookupswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace lookupswitch <target> with goto <target>");
			assertEquals(1, StringUtil.count("dup2", dis), "Expected to keep the dup2");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep the first original value");
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to keep the second original value");
			assertEquals(4, StringUtil.count("pop", dis), "Expected the surviving copy to be popped");
		});
	}

	/**
	 * An effective-goto switch with a {@code dup2} selector keeps the {@code dup2} and pops the
	 * selector copy the goto no longer consumes.
	 */
	@Test
	void foldOpaqueDup2EffectiveGotoSwitchPredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        dup2
				        lookupswitch {
				            0: B,
				            1: B,
				            default: B
				        }
				    B:
				        pop
				        pop
				        pop
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("lookupswitch", dis), "Expected to remove lookupswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace lookupswitch <target> with goto <target>");
			assertEquals(1, StringUtil.count("dup2", dis), "Expected to keep the dup2");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep the first original value");
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to keep the second original value");
			assertEquals(4, StringUtil.count("pop", dis), "Expected the surviving copy to be popped");
		});
	}

	/**
	 * A two-argument predicate whose lower value comes from a {@code dup2} keeps the {@code dup2}
	 * and pops the consumed copy, removing only the top producer.
	 */
	@Test
	void foldOpaqueDup2BelowProducerPredicate() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        dup2
				        iconst_2
				        if_icmpne C
				    B:
				        pop
				        pop
				        pop
				        return
				    C:
				        pop
				        pop
				        pop
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmpne", dis), "Expected to remove if_icmpne");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace if_icmpne <target> with goto <target>");
			assertEquals(1, StringUtil.count("dup2", dis), "Expected to keep the dup2");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep the first original value");
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to keep the second original value");
			assertEquals(0, StringUtil.count("iconst_2", dis), "Expected to remove the top producer");
			assertEquals(4, StringUtil.count("pop", dis), "Expected the surviving copy to be popped");
		});
	}

	/**
	 * A discarded call result must not delete the call, as the call may have side effects.
	 */
	@Test
	void constantFoldingKeepsDiscardedStaticCall() {
		compileFull("Example", COUNTER_SOURCE + """
				class Example {
				    static int run() {
				        Counter.next();
				        return Counter.count;
				    }
				}
				""");
		assertEquals(1, invokeStatic("Example", "run", "Example", "Counter"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(1, invokeStatic("Example", "run", "Example", "Counter"));
	}

	/**
	 * A call whose result is duplicated must still only be called once.
	 */
	@Test
	void constantFoldingKeepsDuplicatedStaticCallSingle() {
		compileFull("Example", COUNTER_SOURCE + """
				class Example {
				    static int run() {
				        int x, y;
				        x = y = Counter.next();
				        return Counter.count * 100 + x * 10 + y;
				    }
				}
				""");
		assertEquals(111, invokeStatic("Example", "run", "Example", "Counter"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(111, invokeStatic("Example", "run", "Example", "Counter"));
	}

	/**
	 * An unused local assigned from a call must not delete the call, as the call may have side effects.
	 */
	@Test
	void variableFoldingKeepsDeadStoreStaticCall() {
		compileFull("Example", COUNTER_SOURCE + """
				class Example {
				    static int run() {
				        int unused = Counter.next();
				        return Counter.count;
				    }
				}
				""");
		assertEquals(1, invokeStatic("Example", "run", "Example", "Counter"));
		transformAndApply(List.of(VariableFoldingTransformer.class));
		assertEquals(1, invokeStatic("Example", "run", "Example", "Counter"));
	}

	/**
	 * A side effecting call between folded operands must be kept even though the arithmetic folds.
	 */
	@Test
	void constantFoldingKeepsVoidStaticCallBetweenOperands() {
		assemble("""
			.super java/lang/Object
			.class public super Example {
			    .field static hits I

			    .method static setupXorKeys ()V {
			        code: {
			        A:
			            getstatic Example.hits I
			            iconst_1
			            iadd
			            putstatic Example.hits I
			            return
			        B:
			        }
			    }

			    .method static run ()I {
			        code: {
			        A:
			            iconst_1
			            iconst_1
			            invokestatic Example.setupXorKeys ()V
			            iadd
			            getstatic Example.hits I
			            iadd
			            ireturn
			        B:
			        }
			    }
			}
			""", true);
		assertEquals(3, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(3, invokeStatic("Example", "run", "Example"));
	}

	/**
	 * A value returning call whose result is discarded between folded operands must be kept.
	 */
	@Test
	void constantFoldingKeepsPoppedStaticCallBetweenOperands() {
		assemble("""
			.super java/lang/Object
			.class public super Example {
			    .field static count I

			    .method static next ()I {
			        code: {
			        A:
			            getstatic Example.count I
			            iconst_1
			            iadd
			            dup
			            putstatic Example.count I
			            ireturn
			        B:
			        }
			    }

			    .method static run ()I {
			        code: {
			        A:
			            iconst_1
			            invokestatic Example.next ()I
			            pop
			            iconst_1
			            iadd
			            getstatic Example.count I
			            iadd
			            ireturn
			        B:
			        }
			    }
			}
			""", true);
		assertEquals(3, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(3, invokeStatic("Example", "run", "Example"));
	}

	/**
	 * A cast that looks redundant for the known runtime value must be kept when the verifier
	 * only knows the less specific static type of the operand.
	 */
	@Test
	void constantFoldingKeepsCastRequiredByVerifier() {
		compileFull("Example", """
				class Example {
				    static int run() {
				        Object[] holder = new Object[4];
				        holder[3] = new Object[5];
				        ((Object[]) holder[3])[0] = "x";
				        return ((Object[]) holder[3])[0] == null ? 0 : 1;
				    }
				}
				""");
		assertEquals(1, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(1, invokeStatic("Example", "run", "Example"));
	}

	/**
	 * Removing a dead block removes its stack map frame, which later delta frames <i>(chop/append/same)</i>
	 * may be relative to. Frames must be recomputed so the output still verifies.
	 */
	@Test
	void deadCodeRemovalRecomputesFramesAfterDeadFrame() {
		putDeadFrameClass();
		assertEquals(1, invokeStatic("DeadFrame", "run", "DeadFrame"));
		transformAndApply(List.of(DeadCodeRemovingTransformer.class));
		assertEquals(1, invokeStatic("DeadFrame", "run", "DeadFrame"));
	}

	/**
	 * Opaque predicate folding runs dead code removal as a dependency, so it must also recompute frames.
	 */
	@Test
	void opaquePredicateFoldingRecomputesFramesAfterDeadFrame() {
		putDeadFrameClass();
		assertEquals(1, invokeStatic("DeadFrame", "run", "DeadFrame"));
		transformAndApply(List.of(OpaquePredicateFoldingTransformer.class));
		assertEquals(1, invokeStatic("DeadFrame", "run", "DeadFrame"));
	}

	/**
	 * Removing a dead store leaves a later stack map frame claiming the local is assigned.
	 * Frames must be recomputed so the output still verifies.
	 */
	@Test
	void variableFoldingRecomputesFramesAfterDeadStoreRemoval() {
		compileFull("Example", """
				class Example {
				    static int field = 7;
				    static int run() {
				        int unused = field;
				        if (field > 5) return 9;
				        return 3;
				    }
				}
				""");
		assertEquals(9, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(VariableFoldingTransformer.class));
		assertEquals(9, invokeStatic("Example", "run", "Example"));
	}

	/**
	 * Compacting local slots and shrinking max locals leaves frames naming slots that no longer exist.
	 * Frames must be recomputed so the output can be written and still verifies.
	 */
	@Test
	void variableTableNormalizingRecomputesFramesAfterSlotCompaction() {
		putClass("SlotGap", node -> {
			MethodVisitor mv = node.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
			Label join = new Label();
			mv.visitCode();
			mv.visitIntInsn(Opcodes.BIPUSH, 42);
			mv.visitVarInsn(Opcodes.ISTORE, 2);
			mv.visitInsn(Opcodes.ICONST_0);
			mv.visitJumpInsn(Opcodes.IFEQ, join);
			mv.visitIincInsn(2, 1);

			// The only used local is in slot 2, with slots 0 and 1 unused.
			mv.visitLabel(join);
			mv.visitFrame(Opcodes.F_FULL, 3, new Object[]{Opcodes.TOP, Opcodes.TOP, Opcodes.INTEGER}, 0, new Object[0]);
			mv.visitVarInsn(Opcodes.ILOAD, 2);
			mv.visitInsn(Opcodes.IRETURN);
			mv.visitMaxs(1, 3);
			mv.visitEnd();
		});
		assertEquals(42, invokeStatic("SlotGap", "run", "SlotGap"));
		transformAndApply(List.of(VariableTableNormalizingTransformer.class));
		assertEquals(42, invokeStatic("SlotGap", "run", "SlotGap"));
	}

	/**
	 * Folding a sequence must not delete array element stores it contains.
	 */
	@Test
	void constantFoldingKeepsArrayStoreInFoldedSequence() {
		assemble(SIDE_EFFECT_SEQUENCES, true);
		assertEquals(7, invokeStatic("Example", "arrayStore", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(7, invokeStatic("Example", "arrayStore", "Example"));
	}

	/**
	 * Folding a sequence must not delete field stores it contains.
	 */
	@Test
	void constantFoldingKeepsFieldStoreInFoldedSequence() {
		assemble(SIDE_EFFECT_SEQUENCES, true);
		assertEquals(7, invokeStatic("Example", "fieldStore", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(7, invokeStatic("Example", "fieldStore", "Example"));
	}

	/**
	 * Folding a sequence must not delete calls with arguments it contains.
	 */
	@Test
	void constantFoldingKeepsArgumentCallInFoldedSequence() {
		assemble(SIDE_EFFECT_SEQUENCES, true);
		assertEquals(7, invokeStatic("Example", "argumentCall", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(7, invokeStatic("Example", "argumentCall", "Example"));
	}

	/**
	 * A folded sequence whose result is known regardless of a side-effecting call operand must keep the call's
	 * value discarded at its position, without disturbing the stack around the folded constant.
	 */
	@Test
	void constantFoldingKeepsCallOperandOfKnownResultOnOneBranch() {
		compileFull("Example", """
				class Example {
				    static int get() { return 5; }
				    static int run() {
				        int r = 1;
				        if (get() > 4) { r = 0 * get(); }
				        return r + 2;
				    }
				}
				""");
		assertEquals(2, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(OpaqueConstantFoldingTransformer.class));
		assertEquals(2, invokeStatic("Example", "run", "Example"));
	}

	private void putDeadFrameClass() {
		putClass("DeadFrame", node -> {
			MethodVisitor mv = node.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()I", null, null);
			Label dead = new Label();
			Label live = new Label();
			mv.visitCode();
			mv.visitInsn(Opcodes.ICONST_1);
			mv.visitVarInsn(Opcodes.ISTORE, 0);
			mv.visitJumpInsn(Opcodes.GOTO, live);

			// Unreachable, but its frame appends two ints to the initial frame and is the base for the next frame.
			mv.visitLabel(dead);
			mv.visitFrame(Opcodes.F_APPEND, 2, new Object[]{Opcodes.INTEGER, Opcodes.INTEGER}, 0, null);
			mv.visitIincInsn(1, 1);
			mv.visitJumpInsn(Opcodes.GOTO, live);

			// Reachable, and chops one local relative to the dead frame.
			mv.visitLabel(live);
			mv.visitFrame(Opcodes.F_CHOP, 1, null, 0, null);
			mv.visitVarInsn(Opcodes.ILOAD, 0);
			mv.visitInsn(Opcodes.IRETURN);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		});
	}
}
