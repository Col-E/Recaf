package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
