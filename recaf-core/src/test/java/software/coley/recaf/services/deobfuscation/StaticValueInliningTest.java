package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;

import java.util.List;

/**
 * Tests for {@link StaticValueCollectionTransformer} / {@link StaticValueInliningTransformer}.
 */
public class StaticValueInliningTest extends BaseDeobfuscationTest{
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
	@Disabled("Requires array content tracking & tracking value into the creation of a 'new String(T)'")
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
					        ldc "SGVsbG8="
					        invokestatic java/util/Base64.getDecoder ()Ljava/util/Base64$Decoder;
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

	private void validateNoInlining(@Nonnull String assembly) {
		validateNoTransformation(assembly, List.of(StaticValueInliningTransformer.class));
	}


	private void validateInlining(@Nonnull String assembly, @Nonnull String expectedBefore, @Nullable String expectedAfter) {
		validateBeforeAfterDecompile(assembly, List.of(StaticValueInliningTransformer.class), expectedBefore, expectedAfter);
	}
}
