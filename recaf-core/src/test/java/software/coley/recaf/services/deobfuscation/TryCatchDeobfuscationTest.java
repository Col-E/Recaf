package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateCatchMergingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

	@Test
	void removeCatchBlocksNotUsableAtRuntime() {
		// The JVM will use the first of "duplicate" blocks like this.
		// More details about this behavior can be found in the redundant catch removing transformer.
		String asm = """
				.method public static example (I[B)V {
					parameters: { index, array },
					exceptions: {
				        { A, B, C, * },
				        { A, B, D, * },
				        { A, B, C, Ljava/lang/ArrayIndexOutOfBoundsException; }
				    },
				    code: {
				    A:
				        aload array
				        iload index
				        baload
				        pop
				    B:
				        goto END
				    C:
				        pop
				        goto END
				    D:
				        invokevirtual java/lang/Throwable.printStackTrace ()V
				        goto END
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("{ A, B, C, * }", dis), "Expected to keep first try-catch");
			assertEquals(0, StringUtil.count("{ A, B, D, * }", dis), "Expected to drop second try-catch");
			assertEquals(0, StringUtil.count("ArrayIndexOutOfBoundsException", dis), "Expected to drop third try-catch");
			assertEquals(0, StringUtil.count("printStackTrace", dis), "Expected to prune dead code of removed D handler");
		});
	}

	@Test
	@Disabled
	void convertAlwaysThrowIntoDirectControlFlow() {
		// TODO: Implement redundant transformer pass for this
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  A,  B,  C, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        aconst_null
				        athrow
				    B:
				        invokestatic Foo.skipped ()V
				        goto END
				    C:
				        invokestatic Foo.prepop ()V
				        pop
				        invokestatic Foo.postpop ()V
				    END:
				        return
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {

		});
	}

	@Test
	void redundantCatchOfTypeNeverThrownInWorkspace() {
		// If we observe 'BogusException' defined in the workspace and know it is never actually constructed
		// then any exception block with it is ALSO never going to be handled. These can be removed.
		String asm = """
				.method public static example ()V {
				    exceptions: {
				        { A, B, C, L\0; }
				    },
				    code: {
					A:
					    invokestatic Foo.foo ()V
					B:
					    return
					C:
					    pop
					    return
					D:
				    }
				}
				""".replace("\0", EXCEPTION_NAME);
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertFalse(dis.contains("exceptions:"), "Expected removal of exceptions");
			assertFalse(dis.contains("pop"), "Expected removal of handler block");
			assertEquals(1, StringUtil.count("return", dis), "Expected dead code to collapse into single return");
			assertTrue(dis.contains("Foo.foo"), "Expected to keep method call");
		});

		// However, if we see an exception type that is not explicitly defined in our workspace's primary resource,
		// such as a library, then we cannot safely assume it cannot be thrown without expensive call-graph walking.
		// So we won't bother with these cases.
		asm = """
				.method public static example ()V {
				    exceptions: {
				        { A, B, C, Lcom/example/Foo; }
				    },
				    code: {
					A:
					    invokestatic Foo.foo ()V
					B:
						return
					C:
					    pop
					    return
					D:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(RedundantTryCatchRemovingTransformer.class));

		// If we have BOTH exceptions then we should remove the 'BogusException' entry but keep the
		// handler block as-is since it is still utilized by the third party 'Foo' exception.
		asm = """
				.method public static example ()V {
				    exceptions: {
				        { A, B, C, Lcom/example/Foo; },
				        { A, B, C, L\0; }
				    },
				    code: {
					A:
					    invokestatic Foo.foo ()V
					B:
						return
					C:
					    pop
					    return
					D:
				    }
				}
				""".replace("\0", EXCEPTION_NAME);
		validateAfterAssembly(asm, List.of(RedundantTryCatchRemovingTransformer.class), dis -> {
			assertTrue(dis.contains("{ A, B, C, Lcom/example/Foo; }"), "Expected to keep 'Foo' exception");
			assertFalse(dis.contains(EXCEPTION_NAME), "Expected to remove '" + EXCEPTION_NAME + "' exception");
			assertTrue(dis.contains("pop"), "Expected keep handler block for 'Foo' use");
			assertEquals(2, StringUtil.count("return", dis), "Expected to keep both returns");
			assertTrue(dis.contains("Foo.foo"), "Expected to keep method call");
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
				        // Dead code space, block 'A' always throws so no need to put a jump to end here.
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
