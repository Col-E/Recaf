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
	/** Remove duplicate code among handler blocks where possible. */
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

	/** Remove a try-catch that will be impossible to occur. */
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
		// The ClassNotFoundException is not relevant and should be removed
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

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void keepThrowingNpe() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/NullPointerException; }
				    },
				    code: {
				    A:
				        aconst_null
				        athrow
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

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void keepDivideByZeroExceptions() {
		String asm = """
				.method public static example ()V {
					exceptions: {
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
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateNoTransformation(asm, List.of(RedundantTryCatchRemovingTransformer.class));
		asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/ArithmeticException; }
				    },
				    code: {
				    A:
				        fconst_0
				        fconst_0
				        fdiv
				        pop
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
		asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/ArithmeticException; }
				    },
				    code: {
				    A:
				        lconst_0
				        lconst_0
				        ldiv
				        pop2
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
		asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/ArithmeticException; }
				    },
				    code: {
				    A:
				        dconst_0
				        dconst_0
				        ddiv
				        pop2
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

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void keepObviousNpeOnFieldAccess() {
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/NullPointerException; }
				    },
				    code: {
				    A:
				        // field owner context is null
				        aconst_null
				        iconst_0
				        putfield Owner.intField I
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
		asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/NullPointerException; }
				    },
				    code: {
				    A:
				        // field owner context is null
				        aconst_null
				        getfield Owner.intField I
				        pop
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

	/** Ensures {@link RedundantTryCatchRemovingTransformer} is not too aggressive. */
	@Test
	void keepAmbiguousNpeOnFieldAccess() {
		String asm = """
				.method public static example (LOwner;)V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/NullPointerException; }
				    },
				    code: {
				    A:
				        // field owner context is a parameter, and thus can be null or not-null
				        aload 0
				        iconst_0
				        putfield Owner.intField I
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
		asm = """
				.method public static example (LOwner;)V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/NullPointerException; }
				    },
				    code: {
				    A:
				        // field owner context is a parameter, and thus can be null or not-null
				        aload 0
				        getfield Owner.intField I
				        pop
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
