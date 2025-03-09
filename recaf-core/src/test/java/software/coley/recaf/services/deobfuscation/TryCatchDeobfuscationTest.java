package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateCatchMergingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link DuplicateCatchMergingTransformer} / {@link RedundantTryCatchRemovingTransformer}.
 */
public class TryCatchDeobfuscationTest extends BaseDeobfuscationTest {
	@Test
	void duplicateCatchHandlers() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/Throwable0; },
				       {  A,  B,  D, Ljava/lang/Throwable1; },
				       {  A,  B,  E, Ljava/lang/Throwable2; },
				       {  A,  B,  F, Ljava/lang/Throwable3; }
				    },
				    code: {
				    A:
				        invokestatic Example.throwing ()V
				    B:
				        goto END
				    C:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        invokestatic  com/example/MyApp.logFailure ()V
				        goto END
				    D:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        invokestatic  com/example/MyApp.logFailure ()V
				        goto END
				    E:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        invokestatic  com/example/MyApp.logFailure ()V
				        goto END
				    F:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        invokestatic  com/example/MyApp.logFailure ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(DuplicateCatchMergingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("printStackTrace", dis), "Catch block contents were not merged");
			assertEquals(5, StringUtil.count("goto", dis), "Expected 5 goto instructions after merging");
		});
	}

	@Test
	void redundantTryCatch() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/Throwable; }
				    },
				    code: {
				    A:
				        aconst_null
				        pop
				    B:
				        goto END
				    C:
				        astore ex
				        aload ex
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("exceptions:", dis), "Should have removed exception ranges");
			assertEquals(0, StringUtil.count("printStackTrace", dis), "Should have removed catch block contents");
			assertEquals(0, StringUtil.count("astore", dis), "Should have removed catch block contents");
		});
	}

	/** Ensures {@link RedundantTryCatchRemovingTransformer} keeps at least one try-catch if multiple identical ones are found. */
	@Test
	void keepOneTryCatchIfIdenticalCopyFound() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/Exception; },
				       {  A,  B,  C, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        iconst_0
				        iconst_0
				        idiv
				        pop
				    B:
				        goto END
				    C:
				        astore ex
				        aload ex
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("Ljava/lang/Exception;", dis), "Should have one exception block");
		});
	}

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void oneRedundantOneRelevantTryCatch() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/ClassNotFoundException; },
				       {  A,  B,  C, Ljava/lang/ArithmeticException; }
				    },
				    code: {
				    A:
				        iconst_0
				        iconst_0
				        idiv
				        pop
				    B:
				        goto END
				    C:
				        astore ex
				        aload ex
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("Ljava/lang/ArithmeticException;", dis), "Should not have removed ArithmeticException");
			assertEquals(0, StringUtil.count("Ljava/lang/ClassNotFoundException;", dis), "Should have removed ClassNotFoundException");
		});
	}

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void keepPotentialThrowingMethodTryCatch() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/Throwable; }
				    },
				    code: {
				    A:
				        invokestatic Example.throwing ()V
				    B:
				        goto END
				    C:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateNoTransformation(asm, List.of(RedundantTryCatchRemovingTransformer.class));
	}
}
