package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.AbstractInsnNode;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.util.AsmInsnUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.analysis.ReInterpreter;
import software.coley.recaf.util.analysis.value.ReValue;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An assortment of deobfuscation tests, specifically to ensure weird bugs that occurred at one point do not come back.
 */
public class RegressionDeobfuscationTest extends BaseDeobfuscationTest {
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
				        iconst_0
				        istore xyz
				    B:
				        iconst_1
				        istore xyz
				    C:
				        goto E
				    D:
				        // handler
				        pop
				        iload xyz
				        ireturn
				    E:
				        iload xyz
				        ireturn
				    F:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class), dis -> {
			assertFalse(dis.contains("xyz"), "Should have folded all variable stores/reads");
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
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class));
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
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class));
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
				            max: 3,
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
				            max: 1,
				            cases: { U },
				            default: H
				        }
				    M:
				        aload v1
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 1,
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
				            max: 1,
				            cases: { W },
				            default: D
				        }
				    S:
				        iload i0
				        tableswitch {
				            min: 0,
				            max: 1,
				            cases: { F },
				            default: N
				        }
				    T:
				        aload v1
				        invokestatic java/lang/Integer.parseInt (Ljava/lang/String;)I
				        tableswitch {
				            min: 0,
				            max: 1,
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
				            max: 1,
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
				            max: 1,
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
	void perf() {
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
}
