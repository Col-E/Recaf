package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link StaticValueCollectionTransformer} / {@link StaticValueInliningTransformer}.
 */
public class StaticValueInliningTest extends TransformerTestBase {
	@Test
	void effectiveFinalAssignmentInClinit() {
		String asm = """
				.field private static foo I
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo I
					    invokevirtual java/io/PrintStream.println (I)V
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        iconst_5
				        putstatic Example.foo I
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(5);");

		// With strings
		asm = """
				.field private static foo Ljava/lang/String;
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo Ljava/lang/String;
					    invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        ldc "Hello"
				        putstatic Example.foo Ljava/lang/String;
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(\"Hello\");");
	}

	@Test
	void effectiveFinalAssignmentDisqualified() {
		String asm = """
				.field private static foo I
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo I
					    invokevirtual java/io/PrintStream.println (I)V
				        return
				    B:
				    }
				}
				
				.method static disqualification ()V {
				    code: {
				    A:
				        iconst_1
				        putstatic Example.foo I
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        iconst_5
				        putstatic Example.foo I
				        return
				    B:
				    }
				}
				""";
		validateNoInlining(asm);
	}

	@Test
	void constAssignmentInClinit() {
		String asm = """
				.field private static final foo I
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo I
					    invokevirtual java/io/PrintStream.println (I)V
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        iconst_5
				        putstatic Example.foo I
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(5);");
	}

	@Test
	void constAssignmentInField() {
		String asm = """
				.field private static final foo I { value: 5 }
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo I
					    invokevirtual java/io/PrintStream.println (I)V
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(5);");
	}

	@Test
	void simpleMathComputedAssignment() {
		String asm = """
				.field private static final foo I
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo I
					    invokevirtual java/io/PrintStream.println (I)V
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        // 50 * 5 = 250
				        bipush 50
				        bipush 5
				        imul
				        // 250 / 10 = 25
				        bipush 10
				        idiv
				        putstatic Example.foo I
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(25);");
	}

	@Test
	void stringBase64Decode() {
		String asm = """
				.field private static final foo Ljava/lang/String;
				
				.method public static example ()V {
				    code: {
				    A:
					    getstatic java/lang/System.out Ljava/io/PrintStream;
					    getstatic Example.foo Ljava/lang/String;
					    invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    B:
				    }
				}
				
				.method static <clinit> ()V {
				    code: {
				    A:
				        new java/lang/String
				        dup
				        invokestatic java/util/Base64.getDecoder ()Ljava/util/Base64$Decoder;
				        ldc "SGVsbG8="
				        invokevirtual java/util/Base64$Decoder.decode (Ljava/lang/String;)[B
				        invokespecial java/lang/String.<init> ([B)V
				        putstatic Example.foo Ljava/lang/String;
				        return
				    B:
				    }
				}
				""";
		validateInlining(asm, "println(foo);", "println(\"Hello\");");
	}

	@Test
	void stringBase64DecodeInDelegatedHelper() {
		// PseudoCode:
		//
		// String[] encoded;
		// String foo;
		//
		// static {
		//    encoded = new String[] { "SGVsbG8=" };
		//    initializeFoo();
		// }
		//
		// private static void initializeFoo() {
		//    foo = new String(Base64.getDecoder().decode(encoded[0]));
		// }
		String asm = """
				.super java/lang/Object
				.class Example {
					.field private static encoded [Ljava/lang/String;
					.field private static foo Ljava/lang/String;
				
					.method public static example ()V {
					    code: {
					    A:
					        getstatic java/lang/System.out Ljava/io/PrintStream;
					        getstatic Example.foo Ljava/lang/String;
					        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
					        return
					    B:
					    }
					}
				
					.method private static initializeFoo ()V {
					    code: {
					    A:
					        new java/lang/String
					        dup
					        invokestatic java/util/Base64.getDecoder ()Ljava/util/Base64$Decoder;
					        getstatic Example.encoded [Ljava/lang/String;
					        iconst_0
					        aaload
					        invokevirtual java/util/Base64$Decoder.decode (Ljava/lang/String;)[B
					        invokespecial java/lang/String.<init> ([B)V
					        putstatic Example.foo Ljava/lang/String;
					        return
					    B:
					    }
					}
				
					.method static <clinit> ()V {
					    code: {
					    A:
					        iconst_1
					        anewarray java/lang/String
					        dup
					        iconst_0
					        ldc "SGVsbG8="
					        aastore
					        putstatic Example.encoded [Ljava/lang/String;
					        invokestatic Example.initializeFoo ()V
					        return
					    B:
					    }
					}
				}
				""";
		validateInlining(asm, "println(foo);", "println(\"Hello\");");
	}

	@Test
	void partialArrayContentsCanBeInlined() {
		// If the static initializer calls multiple methods, but we only evaluate one of them,
		// the array contents of what was evaluated should still be inlined.
		//
		// Just because we didn't fully populate the String[] doesn't mean we can't inline the values that were populated.
		// Additionally, if the array has a value we can't evaluate, that also shouldn't prevent inlining known values.
		String asm = """
				.super java/lang/Object
				.class Example {
					.field private static final values [Ljava/lang/String;
				
					.method public static example ()Ljava/lang/String; {
					    code: {
					    A:
					        // We read index 0, which is initialized in the static initializer.
					        // It should be 'Hello' after inlining.
					        getstatic Example.values [Ljava/lang/String;
					        iconst_0
					        aaload
					        areturn
					    B:
					    }
					}
				
					.method private static allocateArray ()V {
					    code: {
					    A:
					        iconst_5
							anewarray java/lang/String
					        putstatic Example.values [Ljava/lang/String;
					        return
					    B:
					    }
					}
				
					.method private static skippedInitializer ()V {
					    code: {
					    A:
					        getstatic Example.values [Ljava/lang/String;
					        dup
					        iconst_2
					        ldc "Not"
					        aastore
					        dup
					        return
					    B:
					    }
					}
				
					.method private static poison ()V {
					    code: {
					    A:
					        getstatic Example.values [Ljava/lang/String;
					        dup
					        iconst_3
					        getstatic Unknown.POISON Ljava/lang/String;
					        aastore
					        return
					    B:
					    }
					}
				
					.method private static initializeValues ()V {
					    code: {
					    A:
					        getstatic Example.values [Ljava/lang/String;
					        dup
					        iconst_0
					        ldc "Hello"
					        aastore
					        dup
					        iconst_1
					        ldc "World"
					        aastore
					        return
					    B:
					    }
					}
				
					.method static <clinit> ()V {
					    code: {
					    A:
					        invokestatic Example.allocateArray ()V
					        iconst_0
					        ifeq B
					        invokestatic Example.skippedInitializer ()V
					    B:
					        invokestatic Example.initializeValues ()V
					        invokestatic Example.poison ()V
					        return
					    C:
					    }
					}
				}
				""";
		validateAfterAssembly(asm, List.of(StaticValueInliningTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("aaload", dis));
			assertEquals(2, StringUtil.count("ldc \"Hello\"", dis));
		});
	}

	@Test
	void arrayContentsSurviveFrameMerge() {
		// Control flow that results in a frame merge should still facilitate the inlining of the array contents.
		String asm = """
				.super java/lang/Object
				.class Example {
					.field private static final values [I
				
					.method public static example (Z)I {
					    parameters: { condition },
					    code: {
					    A:
					        getstatic Example.values [I
					        astore array
					        iload condition
					        ifeq B
					        aload array
					        goto C
					    B:
					        aload array
					    C:
					        iconst_1
					        iaload
					        ireturn
					    D:
					    }
					}
				
					.method static <clinit> ()V {
					    code: {
					    A:
					        iconst_2
					        newarray int
					        dup
					        iconst_0
					        iconst_5
					        iastore
					        dup
					        iconst_1
					        bipush 9
					        iastore
					        putstatic Example.values [I
					        return
					    B:
					    }
					}
				}
				""";
		validateAfterAssembly(asm, List.of(StaticValueInliningTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iaload", dis));
			assertEquals(1, StringUtil.count("pop2", dis));
			assertTrue(StringUtil.count("bipush 9", dis) >= 1, "Expected the merged array to retain its value");
		});
	}

	@Test
	void readInInitializerHelperIsNotInlined() {
		compileFull("Example", """
				class Example {
				    static final int VALUE;
				    static int early;
				    static {
				        early = report();
				        VALUE = 40 + two();
				    }
				    static int report() { return VALUE; }
				    static int two() { return 2; }
				    static int run() { return early; }
				}
				""");

		// The static initializer calls report() before VALUE is assigned, so it should see the default value of 0.
		// We shouldn't inline the final value of VALUE into report() because that would change what it observes.
		assertEquals(0, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(StaticValueInliningTransformer.class));
		assertEquals(0, invokeStatic("Example", "run", "Example"));
	}

	@Test
	void elementOfMutatedFinalArrayIsNotInlined() {
		compileFull("Example", """
				class Example {
				    private static final int[] TABLE = {1, 2, 3};
				    static void mutate() { TABLE[0] = 9; }
				    static int run() {
				        mutate();
				        return TABLE[0];
				    }
				}
				""");

		// Same idea but with array contents.
		assertEquals(9, invokeStatic("Example", "run", "Example"));
		transformAndApply(List.of(StaticValueInliningTransformer.class));
		assertEquals(9, invokeStatic("Example", "run", "Example"));
	}

	@Test
	void elementOfFinalArrayMutatedByOtherClassIsNotInlined() {
		compileFull("Example", """
				class Other {
				    static void mutate() { Example.TABLE[0] = 9; }
				}
				class Example {
				    static final int[] TABLE = {1, 2, 3};
				    static int run() {
				        Other.mutate();
				        return TABLE[0];
				    }
				}
				""");

		// Same idea but the array mutation is done by another class.
		assertEquals(9, invokeStatic("Example", "run", "Example", "Other"));
		transformAndApply(List.of(StaticValueInliningTransformer.class));
		assertEquals(9, invokeStatic("Example", "run", "Example", "Other"));
	}

	@Test
	void readTriggeringForeignInitializerIsNotInlined() {
		compileFull("Example", """
				class Flag {
				    static boolean registered;
				}
				class Registry {
				    static final int ID;
				    static {
				        Flag.registered = true;
				        ID = 40 + two();
				    }
				    private static int two() { return 2; }
				}
				class Example {
				    static int run() {
				        int id = Registry.ID;
				        return Flag.registered ? id : -1;
				    }
				}
				""");

		// The static initializer of Registry sets Flag.registered to true.
		// Inlining the value of Registry.ID into Example.run() would remove the side effect of setting Flag.registered.
		// If that happens, the test will fail because Flag.registered will be false and Example.run() will return -1.
		assertEquals(42, invokeStatic("Example", "run", "Example", "Flag", "Registry"));
		transformAndApply(List.of(StaticValueInliningTransformer.class));
		assertEquals(42, invokeStatic("Example", "run", "Example", "Flag", "Registry"));
	}

	private void validateNoInlining(@Nonnull String assembly) {
		validateNoTransformation(assembly, List.of(StaticValueInliningTransformer.class));
	}

	private void validateInlining(@Nonnull String assembly, @Nonnull String expectedBefore, @Nullable String expectedAfter) {
		validateBeforeAfterDecompile(assembly, List.of(StaticValueInliningTransformer.class), expectedBefore, expectedAfter);
	}
}
