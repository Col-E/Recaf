package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.CallResultInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FoldingDeobfuscationTest extends BaseDeobfuscationTest {
	@Test
	void foldIntegerMath() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_1
				        iconst_1
				        iadd
				        // 1 + 1  --> 2
				        ineg
				        // 2      --> -2
				        iconst_2
				        imul
				        // -2 x 2 --> -4
				        iconst_4
				        idiv
				        // -4 / 4 --> -1
				        iconst_m1
				        isub
				        // -1 --1 --> 0
				        iconst_1
				        ior
				        // 1 | 0 --> 1
				        iconst_1
				        ishl
				        // 1 << 1 --> 2
				        iconst_1
				        ishr
				        // 2 >> 1 --> 1
				        iconst_2
				        iand
				        // 1 & 2 --> 0
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
	}

	@Test
	void foldFloatMath() {
		String asm = """
				.method public static example ()F {
				    code: {
				    A:
				        fconst_1
				        fconst_1
				        fadd
				        // 1 + 1  --> 2
				        fneg
				        // 2      --> -2
				        fconst_2
				        fmul
				        // -2 x 2 --> -4
				        ldc 4F
				        fdiv
				        // -4 / 4 --> -1
				        ldc -1F
				        fsub
				        // -1 --1 --> 0
				        freturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("fconst_0", dis), "Expected to fold to 0F");
		});
	}

	@Test
	void foldDoubleMath() {
		String asm = """
				.method public static example ()D {
				    code: {
				    A:
				        dconst_1
				        dconst_1
				        dadd
				        // 1 + 1  --> 2
				        dneg
				        // 2      --> -2
				        ldc 2.0
				        dmul
				        // -2 x 2 --> -4
				        ldc 4.0
				        ddiv
				        // -4 / 4 --> -1
				        ldc -1.0
				        dsub
				        // -1 --1 --> 0
				        dreturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("dconst_0", dis), "Expected to fold to 0.0");
		});
	}

	@Test
	void foldLongMath() {
		String asm = """
				.method public static example ()J {
				    code: {
				    A:
				        lconst_1
				        lconst_1
				        ladd
				        // 1 + 1  --> 2
				        lneg
				        // 2      --> -2
				        ldc 2L
				        lmul
				        // -2 x 2 --> -4
				        ldc 4L
				        ldiv
				        // -4 / 4 --> -1
				        ldc -1L
				        lsub
				        // -1 --1 --> 0
				        lreturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("lconst_0", dis), "Expected to fold to 0L");
		});
	}

	@Test
	void foldIntMathWithSwap() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        swap
				        ishl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to stack operation");
			assertEquals(0, StringUtil.count("ishl", dis), "Expected to math operation");
			assertEquals(1, StringUtil.count("bipush 16", dis), "Expected to fold to 16 from (2 << 3)");
		});

		// Same thing but with two swaps
		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        swap
				        swap
				        ishl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to stack operation");
			assertEquals(0, StringUtil.count("ishl", dis), "Expected to math operation");
			assertEquals(1, StringUtil.count("bipush 12", dis), "Expected to fold to 16 from (3 << 2)");
		});

		// Same thing but with three swaps
		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        swap
				        swap
				        swap
				        ishl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to stack operation");
			assertEquals(0, StringUtil.count("ishl", dis), "Expected to math operation");
			assertEquals(1, StringUtil.count("bipush 16", dis), "Expected to fold to 16 from (2 << 3)");
		});
	}

	@Test
	void foldLongWithDup2NotConfusedByPrecedingIntOnStack() {
		// Ensures that the stack dup2 operation folds correctly:
		//  [int, long] --> [int, long, long]
		// The preceding int shouldn't confuse the fold logic.
		String asm = """
				.method public static example ()J {
				    code: {
				    A:
				        iconst_0
				        // 1L + 1L = 2L
				        lconst_1
				        dup2
				        ladd
				        // Store var so that we can pop the int off the stack
				        lstore tmp
				        pop
				        lload tmp
				        lreturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("lconst_1", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup2", dis), "Expected to stack operation");
			assertEquals(0, StringUtil.count("ladd", dis), "Expected to operation");
			assertEquals(1, StringUtil.count("ldc 2L", dis), "Expected to fold stack operation to 2L");
		});
	}

	@Test
	void foldLongWithDup2X1NotConfusedByPrecedingIntOnStack() {
		// Ensures that the stack dup2_x1 operation folds correctly:
		//  [int, long] --> [long, int, long]
		// The preceding int shouldn't confuse the stack simplification logic.
		// This in turn will allow the value folding step to fold the whole method.
		String asm = """
				.method public static example ()J {
				    code: {
				    A:
				        iconst_0
				        lconst_1
				        dup2_x1
				        // Store var so that we can pop the int off the stack
				        lstore tmp
				        pop
				        lconst_1
				        ladd
				        // The duplicated long below the int should be all that remains
				        lreturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("lconst_1", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold inputs");
			assertEquals(1, StringUtil.count("ldc 2L", dis), "Expected to fold stack operation to 2L");
		});
	}

	@Test
	void foldPopsToNothing() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        iconst_1
				        pop2
				        pop
				        return
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove pop operations");
		});
	}

	@Test
	void foldPopsWithDupX1ToNothing() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_2
				        iconst_1
				        dup_x1
				        pop2
				        pop
				        return
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove pop operations");
		});
	}

	@Test
	void foldPopsWithDupX2ToNothing() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        iconst_1
				        dup_x1
				        pop2
				        pop2
				        return
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove pop operations");
		});
	}

	@Test
	void foldPopsWithDup2X1ToNothing() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        iconst_1
				        dup2_x1
				        pop2
				        pop2
				        pop
				        return
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove pop operations");
		});
	}

	@Test
	void foldPopsWithDup2X2ToNothing() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_4
				        iconst_3
				        iconst_2
				        iconst_1
				        dup2_x2
				        pop2
				        pop2
				        pop2
				        return
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to remove redundant popped values");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove pop operations");
		});
	}

	@Test
	void foldMathWithSwapPop() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
						iconst_1
						iconst_4
						bipush 9
						swap
						pop
						iadd
						ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to fold stack swapping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(1, StringUtil.count("bipush 10", dis), "Expected to fold to 10 from 9+1");
		});
	}

	@Test
	void foldWithDup2() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        dup2
				        iadd
				        iadd
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(1, StringUtil.count("bipush 10", dis), "Expected to fold to 10 from (3+2)*2");
		});
	}

	@Test
	void foldWithDup2Pop() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        dup2
				        pop
				        iadd
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(1, StringUtil.count("bipush 8", dis), "Expected to fold to 8 from (3+2+3)");
		});
	}

	@Test
	void foldWithDup2X1() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_1
				        iconst_1
				        iconst_1
				        dup2_x1
				        iadd
				        iadd
				        iadd
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_1", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5 from 1+1+1+1+1");
		});
	}

	@Test
	void foldWithDup2X1Pop() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_3
				        iconst_2
				        iconst_1
				        dup2_x1
				        pop2
				        iadd
				        imul
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to fold stack popping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold multiplication");
			assertEquals(1, StringUtil.count("bipush 8", dis), "Expected to fold to 8 from (3+1)*2");
		});
	}

	@Test
	void fold1Plus1() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_1
				        iconst_1
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_1", dis), "Expected to fold inputs");
			assertEquals(1, StringUtil.count("iconst_2", dis), "Expected to fold to 2 from 1+1");
		});
	}

	@Test
	void foldWithDup2X2Pop() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_4
				        iconst_3
				        iconst_2
				        iconst_1
				        dup2_x2
				        iadd
				        imul
				        swap
				        pop
				        iadd
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to fold stack popping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold multiplication");
			assertEquals(1, StringUtil.count("bipush 12", dis), "Expected to fold to 12 from ((2+1)*3)+1+2");
		});
	}

	@Test
	void foldWithDupX1() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        bipush 10
				        iconst_5
				        dup_x1
				        imul
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold multiplication");
			assertEquals(1, StringUtil.count("bipush 55", dis), "Expected to fold to 55 from (10*5)+5");
		});
	}

	@Test
	void foldWithDupX2() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        bipush 15
				        bipush 8
				        iconst_2
				        dup_x2
				        iadd
				        iadd
				        imul
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold stack duplication");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold addition");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold multiplication");
			assertEquals(1, StringUtil.count("bipush 50", dis), "Expected to fold to 50 from (2+8+15)*2");
		});
	}

	@Test
	void foldRedundantOperationOnUnknownValue() {
		String asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        iconst_0
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operation");
		});

		asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iconst_0
				        iload a
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operation");
		});

		asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        iconst_0
				        swap
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to fold redundant input swapping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operation");
		});

		asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        iconst_0
				        swap
				        swap
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to fold redundant input swapping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operation");
		});

		asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        iconst_0
				        swap
				        swap
				        swap
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("swap", dis), "Expected to fold redundant input swapping");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operation");
		});
	}

	@Test
	void foldWhenUnknownValueMultipliedByZero() {
		String asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        iconst_0
				        imul
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iload", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold redundant operation");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep zero as output");
		});
	}

	@Test
	void opaqueConstantFoldingLongShift() {
		// We had a bug where shift instructions were not reporting the right stack consumption amount
		// but this has since been fixed. So these should cleanly be folded now.
		String asm = """
				.method public static example ()J {
				    code: {
				    A:
				        ldc 1000000L
				        iconst_5
				        lushr
				        lreturn
				    H:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			// Expected folded value
			assertTrue(dis.contains("31250L"));
		});

		asm = """
				.method public static example ()J {
				    code: {
				    A:
				        ldc 1000000L
				        iconst_5
				        lshr
				        lreturn
				    H:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			// Expected folded value
			assertTrue(dis.contains("31250L"));
		});

		asm = """
				.method public static example ()J {
				    code: {
				    A:
				        ldc 1000000L
				        iconst_5
				        lshl
				        lreturn
				    H:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			// Expected folded value
			assertTrue(dis.contains("32000000L"));
		});
	}

	@Test
	void foldDoubleDup2X1() {
		String asm = """
				.method public static example (I)I {
					parameters: { a },
					code: {
				    A:
				        iload a
				        dconst_0
				        dup2_x1
				        pop2
				        i2d
				        dmul
				        d2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iload", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold redundant operation");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep zero as output");
		});
	}

	@Test
	void foldDoubleDup2X2() {
		String asm = """
				.method public static example (D)I {
					parameters: { a },
					code: {
				    A:
				        dload a
				        dconst_0
				        dup2_x2
				        pop2
				        dmul
				        d2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iload", dis), "Expected to fold redundant operation inputs");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold redundant operation");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to keep zero as output");
		});
	}

	@Test
	void foldWithMultipleDupsSwapsAndOperations() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc -2556828284053591649L
				        ldc 2556828284013841450L
				        lxor
				        l2i
				        ldc -415261744
				        i2l
				        ldc 1102084415
				        i2l
				        lxor
				        l2i
				        dup2
				        dup_x1
				        ineg
				        iconst_m1
				        iadd
				        swap
				        dup_x1
				        iconst_m1
				        ixor
				        ior
				        swap
				        iconst_m1
				        ixor
				        isub
				        iadd
				        dup_x2
				        pop
				        iconst_1
				        isub
				        iconst_m1
				        ixor
				        iconst_m1
				        iadd
				        swap
				        iconst_1
				        isub
				        iconst_m1
				        ixor
				        iconst_m1
				        iadd
				        dup_x1
				        ineg
				        iconst_m1
				        iadd
				        swap
				        dup_x1
				        iconst_m1
				        ixor
				        ior
				        swap
				        iconst_m1
				        ixor
				        isub
				        iadd
				        swap
				        dup_x1
				        ineg
				        iconst_m1
				        iadd
				        dup_x1
				        iconst_m1
				        ixor
				        iand
				        iadd
				        swap
				        ineg
				        iconst_m1
				        iadd
				        isub
				        i2l
				        ldc -6788200298164900932L
				        ldc -6788200298671981035L
				        lxor
				        l2i
				        ldc -1757344751
				        i2l
				        ldc -1441456414
				        i2l
				        lxor
				        l2i
				        dup2
				        dup_x1
				        ineg
				        iconst_m1
				        iadd
				        swap
				        dup_x1
				        iconst_m1
				        ixor
				        ior
				        swap
				        iconst_m1
				        ixor
				        isub
				        iadd
				        dup_x2
				        pop
				        swap
				        dup_x1
				        ineg
				        iconst_m1
				        iadd
				        dup_x1
				        iconst_m1
				        ixor
				        iand
				        iadd
				        swap
				        ineg
				        iconst_m1
				        iadd
				        isub
				        isub
				        i2l
				        lxor
				        l2i
				        ireturn
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "return (", "return 0;");
	}

	@Test
	void foldRedundantOperatorsOnParameterToParameter() {
		String asm = """
				.method public static example (I)I {
					parameters: { a },
				    code: {
				    A:
				        iload a
				        iconst_0
				        dup2
				        swap
				        imul
				        iadd
				        iadd
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst", dis), "Expected to fold redundant inputs");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to fold redundant operations");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold redundant operations");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold redundant operations");
		});
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "return ", "return a;");
	}

	@Test
	void foldDupSwappingUnknownValueMultipliedBy0To0() {
		String asm = """
				.method public static example (I)I {
					parameters: { a },
				    code: {
				    A:
				        iload a
				        iconst_0
				        // [a, 0]
				        dup2
				        // [a, 0, a, 0]
				        swap
				        // [a, 0, a, 0]
				        imul
				        // [a, 0, 0]
				        imul
				        // [a, 0]
				        imul
				        // [0]
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iload", dis), "Expected to fold inputs");
			assertEquals(0, StringUtil.count("imul", dis), "Expected to fold redundant operations");
			assertEquals(0, StringUtil.count("dup", dis), "Expected to fold redundant operations");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "return ", "return 0;");
	}

	@Test
	void foldConvertInt2Float() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 10
				        i2f
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("i2f", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc 10F", dis), "Expected to fold to converted value");
		});
	}

	@Test
	void foldConvertLong2Float() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 100000000000000000L
				        l2f
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("l2f", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc " + StringUtil.toString(100000000000000000F) + "F", dis),
					"Expected to fold to converted value");
		});
	}

	@Test
	void foldConvertDouble2Float() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 5.125
				        d2f
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("d2f", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc 5.125F", dis), "Expected to fold to converted value");
		});
	}

	@Test
	void foldConvertLong2Int() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 100000000000000000L
				        l2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("l2i", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc " + (int) 100000000000000000L, dis), "Expected to fold to converted value");
		});
	}

	@Test
	void foldConvertFloat2Int() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 100000000000000000F
				        f2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("f2i", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc " + (int) 100000000000000000F, dis), "Expected to fold to converted value");
		});
	}

	@Test
	void foldConvertDouble2Int() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 10000000000000000000000000000000.0
				        d2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("d2i", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc " + (int) 10000000000000000000000000000000.0, dis), "Expected to fold to converted value");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 0.95
				        d2i
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("d2i", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to converted value");
		});
	}

	@Test
	void foldLongComparison() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 123L
				        ldc 321L
				        lcmp
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_m1", dis), "Expected to fold to -1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 321L
				        ldc 123L
				        lcmp
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to fold to 1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 321L
				        dup2
				        lcmp
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
	}

	@Test
	void foldFloatComparison() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 123.0F
				        ldc 321.0F
				        fcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_m1", dis), "Expected to fold to -1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				    	ldc 321.0F
				        ldc 123.0F
				        fcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to fold to 1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				    	ldc 321.0F
				        ldc 321.0F
				        fcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
	}

	@Test
	void foldDoubleComparison() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 123.0
				        ldc 321.0
				        dcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_m1", dis), "Expected to fold to -1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 321.0
				        ldc 123.0
				        dcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to fold to 1");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc 321.0
				        ldc 321.0
				        dcmpl
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
	}

	@Test
	void foldCommonStaticMethodCalls() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        getstatic java/lang/Byte.BYTES I
				        getstatic java/lang/Long.BYTES I
				        invokestatic java/lang/Math.min (II)I
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to fold to 1");
			assertEquals(0, StringUtil.count("iconst_5", dis), "Expected to fold argument");
			assertEquals(0, StringUtil.count("Math.min", dis), "Expected to fold method call");
		});
	}

	@Test
	void foldStringInstanceMethodCalls() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc "12345"
				        invokevirtual java/lang/String.length ()I
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5");
			assertEquals(0, StringUtil.count("ldc", dis), "Expected to fold original string");
			assertEquals(0, StringUtil.count("invoke", dis), "Expected to fold method call");
		});

		asm = """
				.method public static example ()I {
				    code: {
				    A:
				        ldc "0123456789"
				        ldc "5"
				        invokevirtual java/lang/String.indexOf (Ljava/lang/String;)I
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5");
			assertEquals(0, StringUtil.count("ldc", dis), "Expected to fold original string");
			assertEquals(0, StringUtil.count("invoke", dis), "Expected to fold method call");
		});

		// Found in a random sample, the control flow was resulting in the code under label 'C' being revisited.
		// The merge code in our analysis for strings was wrong, which prevented this from being combined.
		// This test ensures that bad frame merge is no longer an issue.
		asm = """
				.method public static example ([B)Ljava/lang/String; {
					parameters: { data },
				    code: {
				    A:
				        iconst_0
				        istore counter
				    B:
				        iload counter
				        aload data
				        arraylength
				        if_icmpge C
				        iinc counter 1
				        goto B
				    C:
				        ldc "UTF-"
				        ldc "8"
				        invokevirtual java/lang/String.concat (Ljava/lang/String;)Ljava/lang/String;
				    D:
				        areturn
				    E:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("ldc \"UTF-8\"", dis), "Expected to fold to single string");
			assertEquals(0, StringUtil.count("invoke", dis), "Expected to remove method calls");
		});
	}

	@Test
	void foldCharArrayIntoStringFromValueOf() {
		String asm = """
				.method public static example ()Ljava/lang/String; {
				    code: {
				    A:
				        iconst_2
				        newarray char
				        dup
				        iconst_0
				        ldc 'H'
				        castore
				        dup
				        iconst_1
				        ldc 'i'
				        castore
				        invokestatic java/lang/String.valueOf ([C)Ljava/lang/String;
				        areturn
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class),
				"return String.valueOf(new char[]{'H', 'i'});", "return \"Hi\";");
	}

	@Test
	void foldStringToCharArrayFetch() {
		String asm = """
				.method public static example ()C {
				    code: {
				    A:
				        ldc "abc"
				        invokevirtual java/lang/String.toCharArray ()[C
				        iconst_0
				        caload
				        ireturn
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class),
				"return \"abc\".toCharArray()[0]", "return 'a';");
	}

	@Test
	void simulatingThrowingMethodDoesNotFold() {
		String asm = """
				.method public static example ()[C {
				    code: {
				    A:
				        ldc "abc"
				        invokevirtual java/lang/String.toCharArray ()[C
				        iconst_m1
				        iconst_m1
				        invokestatic java/util/Arrays.copyOfRange ([CII)[C
				        areturn
				    B:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(OpaqueConstantFoldingTransformer.class));
	}

	@Test
	void foldFormattedString() {
		// Has Object[] parameter with values that we want to fold
		String asm = """
				.method public static example ()Ljava/lang/String; {
				    code: {
				    A:
				        ldc "Hello %s - Number %d"
				        iconst_2
				        anewarray java/lang/Object
				        dup
				        iconst_0
				        ldc "Name"
				        aastore
				        dup
				        iconst_1
				        bipush 32
				        invokestatic java/lang/Integer.valueOf (I)Ljava/lang/Integer;
				        aastore
				        invokevirtual java/lang/String.formatted ([Ljava/lang/Object;)Ljava/lang/String;
				        areturn
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(OpaqueConstantFoldingTransformer.class), "", "");
	}

	@Test
	void foldStaticCallToXorString() {
		String asm = """
				.super java/lang/Object
				.class Example {
					.method static example ()V {
					    code: {
					    A:
					        getstatic java/lang/System.out Ljava/io/PrintStream;
					        ldc "㘯㘂㘋㘋㘈㙇㘐㘈㘕㘋㘃"
					        ldc 0b11011001100111
					        invokestatic Example.decrypt (Ljava/lang/String;I)Ljava/lang/String;
					        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
					    B:
					        return
					    C:
					    }
					}
					.method static decrypt (Ljava/lang/String;I)Ljava/lang/String; {
					    parameters: { input, xor },
					    code: {
					    A:
					        aload input
					        invokevirtual java/lang/String.length ()I
					        istore length
					    B:
					        iload length
					        newarray char
					        astore chars
					    C:
					        iconst_0
					        istore i
					    D:
					        iload i
					        iload length
					        if_icmpge G
					    E:
					        aload chars
					        iload i
					        aload input
					        iload i
					        invokevirtual java/lang/String.charAt (I)C
					        iload xor
					        ixor
					        i2c
					        castore
					    F:
					        iinc i 1
					        goto D
					    G:
					        aload chars
					        invokestatic java/lang/String.valueOf ([C)Ljava/lang/String;
					        areturn
					    H:
					    }
					}
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(CallResultInliningTransformer.class),
				"System.out.println(Example.decrypt(\"\\u362f\\u3602\\u360b\\u360b\\u3608\\u3647\\u3610\\u3608\\u3615\\u360b\\u3603\", 13927));",
				"System.out.println(\"Hello world\");");
	}

	@Test
	void foldRedundant1DIntArray() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        // return new int[] { 5 }[0];
						iconst_1
						newarray int
						dup
						iconst_0
						iconst_5
						iastore
						iconst_0
						iaload
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("array", dis), "Expected to fold redundant array");
			assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5");
		});
	}

	@Test
	void foldRedundant2DIntArray() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        // int[][] array = new int[4][3];
					    iconst_4
					    iconst_3
					    multianewarray [[I 2
					    astore array
					    
					    // array[1][2] = 5;
					    aload array
					    iconst_1
					    aaload
					    iconst_2
					    iconst_5
					    iastore
					    
					    // return array[1][2];
					    aload array
					    iconst_1
					    aaload
					    iconst_2
					    iaload
					    ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("array", dis), "Expected to fold redundant array");
			assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5");
		});
	}

	@Test
	void foldOpaqueIfeq() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        ifeq C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifeq", dis), "Expected to remove ifeq");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifeq <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIflt() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_m1
				        iflt C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iflt", dis), "Expected to remove iflt");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace iflt <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfle() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        ifle C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iflt", dis), "Expected to remove ifle");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifle <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfgt() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_1
				        ifgt C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iflt", dis), "Expected to remove ifgt");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifgt <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfge() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        ifge C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifge", dis), "Expected to remove ifge");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifge <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfnull() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        ifnull C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifnull", dis), "Expected to remove ifnull");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifnull <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfnonnull() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        ldc ""
				        ifnonnull C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("ifnull", dis), "Expected to remove ifnonnull");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace ifnonnull <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmpeq() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_0
				        if_icmpeq C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmpeq", dis), "Expected to remove if_icmpeq");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmpeq <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmpne() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_1
				        if_icmpne C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmpne", dis), "Expected to remove if_icmpne");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmpne <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmplt() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        iconst_3
				        if_icmplt C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmplt", dis), "Expected to remove if_icmplt");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmplt <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmpge() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        iconst_3
				        if_icmpge C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmpge", dis), "Expected to remove if_icmpge");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmpge <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmpgt() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_5
				        iconst_3
				        if_icmpgt C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmpgt", dis), "Expected to remove if_icmpgt");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmpgt <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfIcmple() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        iconst_3
				        if_icmple C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_icmple", dis), "Expected to remove if_icmple");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_icmple <target> with goto <target>");
		});
	}

	@Test
	void foldOpaqueIfAcmpeq() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        dup
				        if_acmpeq C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_acmpeq", dis), "Expected to remove if_acmpeq");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_acmpeq <target> with goto <target>");
		});

		// Fall-through case
		asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        ldc "not_null"
				        if_acmpeq C
				    B:
				        return
				    C:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_acmpeq", dis), "Expected to remove if_acmpeq");
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to optimize out all code");
		});
	}

	@Test
	void foldOpaqueIfAcmpne() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        ldc "not null"
				        if_acmpne C
				    B:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    C:
				        return
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_acmpne", dis), "Expected to remove if_acmpne");
			assertEquals(1, StringUtil.count("goto", dis), "Expected to replace if_acmpne <target> with goto <target>");
		});

		// Fall-through case
		asm = """
				.method public static example ()V {
				    code: {
				    A:
				        aconst_null
				        dup
				        if_acmpne C
				    B:
				        return
				    C:
				        // Should be skipped over by transformer
				        aconst_null
				        athrow
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("if_acmpne", dis), "Expected to remove if_acmpne");
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to optimize out all code");
		});
	}

	@Test
	void foldOpaqueTableSwitch() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_2
				        tableswitch {
				            min: 0,
				            max: 2,
				            cases: { B, C, D },
				            default: E
				        }
				    B:
				        aconst_null
				        athrow
				    C:
				        aconst_null
				        athrow
				    D:
				        return
				    E:
				        aconst_null
				        athrow
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			// Switch should be replaced with a single goto
			assertEquals(0, StringUtil.count("tableswitch", dis), "Expected to remove tableswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace tableswitch <target> with goto <target>");

			// Dead code should be removed
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to remove dead aconst_null");
			assertEquals(0, StringUtil.count("athrow", dis), "Expected to remove dead athrow");
		});
	}

	@Test
	void foldOpaqueLookupSwitch() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_3
				        lookupswitch {
				            1: B,
				            2: C,
				            3: D,
				            default: E
				        }
				    B:
				        aconst_null
				        athrow
				    C:
				        aconst_null
				        athrow
				    D:
				        return
				    E:
				        aconst_null
				        athrow
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			// Switch should be replaced with a single goto
			assertEquals(0, StringUtil.count("lookupswitch", dis), "Expected to remove lookupswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace lookupswitch <target> with goto <target>");

			// Dead code should be removed
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to remove dead aconst_null");
			assertEquals(0, StringUtil.count("athrow", dis), "Expected to remove dead athrow");
		});
	}

	/** @see #foldLookupSwitchOfUnknownParameterIfIsEffectiveGoto() */
	@Test
	void foldTableSwitchOfUnknownParameterIfIsEffectiveGoto() {
		String asm = """
				.method public static example (I)V {
					parameters: { key },
				    code: {
				    A:
				        iload key
				        tableswitch {
				            min: 0,
				            max: 2,
				            cases: { D, D, D },
				            default: D
				        }
				    B:
				        aconst_null
				        athrow
				    C:
				        aconst_null
				        athrow
				    D:
				        return
				    E:
				        aconst_null
				        dup
				        pop
				        athrow
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			// Switch should be replaced with a single goto
			assertEquals(0, StringUtil.count("iload key", dis), "Expected to remove tableswitch argument");
			assertEquals(0, StringUtil.count("tableswitch", dis), "Expected to remove tableswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace tableswitch <target> with goto <target>");

			// Dead code should be removed
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to remove dead aconst_null");
			assertEquals(0, StringUtil.count("athrow", dis), "Expected to remove dead athrow");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove dead athrow");
		});
	}

	/** @see #foldTableSwitchOfUnknownParameterIfIsEffectiveGoto() */
	@Test
	void foldLookupSwitchOfUnknownParameterIfIsEffectiveGoto() {
		String asm = """
				.method public static example (I)V {
					parameters: { key },
				    code: {
				    A:
				        iload key
				        lookupswitch {
				            1: D,
				            2: D,
				            3: D,
				            default: D
				        }
				    B:
				        aconst_null
				        athrow
				    C:
				        aconst_null
				        athrow
				    D:
				        return
				    E:
				        aconst_null
				        dup
				        pop
				        athrow
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class), dis -> {
			// Switch should be replaced with a single goto
			assertEquals(0, StringUtil.count("iload key", dis), "Expected to remove lookupswitch argument");
			assertEquals(0, StringUtil.count("lookupswitch", dis), "Expected to remove lookupswitch");
			assertEquals(1, StringUtil.count("goto B", dis), "Expected to replace lookupswitch <target> with goto <target>");

			// Dead code should be removed
			assertEquals(0, StringUtil.count("aconst_null", dis), "Expected to remove dead aconst_null");
			assertEquals(0, StringUtil.count("athrow", dis), "Expected to remove dead athrow");
			assertEquals(0, StringUtil.count("pop", dis), "Expected to remove dead athrow");
		});
	}

	/** Showcase pairing of {@link VariableFoldingTransformer} with {@link OpaqueConstantFoldingTransformer} */
	@Test
	void foldVar() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_0
				        istore zero
				        bipush 50
				        istore unused0
				        bipush 51
				        istore unused1
				        bipush 52
				        istore unused2
				        bipush 53
				        istore unused3
				        bipush 54
				        istore unused4
				    B:
				        iload zero
				        ireturn
				    C:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("istore unused", dis), "Expected to remove variable stores where no reads are used");
			assertEquals(0, StringUtil.count("bipush", dis), "Expected to remove unused value pushes of variables");
			assertEquals(0, StringUtil.count("zero", dis), "Expected to inline 'zero' variable");
		});
	}

	/** Chose case {@link VariableFoldingTransformer} along with other flow-based cleanup transformers. */
	@Test
	void foldVarAndFlow() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_m1
				        istore foo
				        iconst_0
				        istore foo
				    B:
				        iload foo
				        ifeq C
				        iload foo
				        ireturn
				    C:
				        iload foo
				        ireturn
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(
				VariableFoldingTransformer.class,
				OpaquePredicateFoldingTransformer.class,
				OpaqueConstantFoldingTransformer.class,
				GotoInliningTransformer.class
		), dis -> {
			assertEquals(1, StringUtil.count("iconst", dis), "Expected only one const");
			assertEquals(1, StringUtil.count("ireturn", dis), "Expected only one return");
			assertEquals(0, StringUtil.count("ifeq", dis), "Expected to fold opaque ifeq");
			assertEquals(0, StringUtil.count("foo", dis), "Expected to fold redundant variable declaration");
		});
	}

	/** Show {@link VariableFoldingTransformer} can inline when parameters are effectively unused/overwritten */
	@Test
	void foldVarsOfOverwrittenParameters() {
		String asm = """
				.method public static example (II)I {
					parameters: { a, b },
				    code: {
				    A:
				        iconst_0
				        istore a
				        iconst_0
				        istore b
				    B:
				        iload a
				        iload b
				        iadd
				        ireturn
				    C:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class,
				OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("istore", dis), "Expected to remove redundant istore");
			assertEquals(0, StringUtil.count("iload", dis), "Expected to inline redundant iload");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to have single iconst_0");
		});
	}

	@Test
	void foldUnusedIincWithVariableFolding() {
		// The variable folding transformer will not clean up the "iconst_0, pop, nop" sequence
		// as that is the role of the constant folding transformer, but when we decompile the
		// bytecode it should be smart enough to figure out the value has been folded to const 4.
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_0
				        istore a
				        iinc a 4
						iload a
				        ireturn
				    B:
				    }
				}
				""";
		validateBeforeAfterDecompile(asm, List.of(VariableFoldingTransformer.class), "return a += 4;", "return 4;");
	}

	@Test
	void foldUnusedIincWithConstantFolding() {
		// As opposed to the variable folding transformer, this will one-shot the entire sequence and handle
		// all cleanup in a single pass.
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_0
				        istore a
				        iinc a 1
				        iload a
				        iconst_1
				        iadd
				        istore a
				        iinc a 2
						iload a
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("iconst_0", dis), "Expected to remove initial 0 value");
			assertEquals(0, StringUtil.count("istore", dis), "Expected to remove redundant istore");
			assertEquals(0, StringUtil.count("iinc", dis), "Expected to remove redundant iinc");
			assertEquals(0, StringUtil.count("iadd", dis), "Expected to remove redundant iadd");
			assertEquals(0, StringUtil.count("iload", dis), "Expected to inline redundant iload");
			assertEquals(1, StringUtil.count("iconst_4", dis), "Expected to have single iconst_4");
		});
	}

	@Test
	void repeatedSteps() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_1
				        istore a
				        iload a
				        istore b
				        iload b
				        istore c
				        iload c
				        istore d
				        iload d
				        istore e
				        iload a
				        iload b
				        iadd
				        istore c
				        iload c
				        iload d
				        iadd
				        istore e
				        return
				    B:
				    }
				}
				""";
		validateAfterRepeatedAssembly(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class, OpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("const", dis), "Expected to remove redundant iconst_0");
			assertEquals(0, StringUtil.count("load", dis), "Expected to remove redundant iload");
			assertEquals(0, StringUtil.count("store", dis), "Expected to remove redundant istore");
		});
	}

	/** Show {@link VariableFoldingTransformer} isn't too aggressive */
	@Test
	void dontFoldVarsOfUsedParameters() {
		String asm = """
				.method public static example (II)I {
					parameters: { a, b },
				    code: {
				    A:
				        iconst_0
				        istore c
				    B:
				        iload a
				        iload b
				        iadd
				        istore c
				    C:
				        sipush 1000
				        iload c
				        if_icmpge B
				    D:
				        iload c
				        ireturn
				    E:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class));
	}

	/** Show {@link VariableFoldingTransformer} isn't too aggressive */
	@Test
	void dontFoldVarsOfUsedButOverwrittenParameters() {
		String asm = """
				.method public static example (II)I {
					parameters: { a, b },
				    code: {
				    A:
				        iconst_0
				        istore c
				    B:
				        iload a
				        iload b
				        iadd
				        istore c
				    C:
				        sipush 1000
				        iload c
				        if_icmpge B
				    D:
				        iconst_1
				        istore a
				        iconst_1
				        istore b
				        iload c
				        ireturn
				    E:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class, OpaqueConstantFoldingTransformer.class));
	}

	@Test
	void foldOpaquePredicateAlsoRemovesTryCatchesThatAreNowDeadCode() {
		String asm = """
				.method example ()V {
				    exceptions: {
				        { B, C, C, Ljava/lang/Exception; },
				        { C, D, B, Ljava/lang/Exception; }
				     },
				    code: {
				    A:
				        // When this opaque predicate gets folded it will make the try-catch ranges into dead code.
				        // The dead code filter will remove the contents of that code range, and thus the
				        // try-catch declarations should also be removed from the method.
				        iconst_0
				        ifeq D
				        aconst_null
				    B:
				        athrow
				    C:
				        athrow
				    D:
				        aload this
				        invokespecial java/lang/Object.<init> ()V
				        return
				    E:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class /* Dead code pass implicitly added */), dis -> {
			assertEquals(0, StringUtil.count("exceptions:", dis), "Try-catch blocks should have been removed");
		});

		// Jump to try end will remove it
		asm = """
				.method public static example ()Ljava/lang/String; {
				    exceptions: {
				        { B, D, E, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        goto D
				    B:
				        invokestatic Foo.b ()Ljava/lang/String;
				    C:
				        invokestatic Foo.c ()Ljava/lang/String;
				    D:
				        goto F
				    E:
				        pop
				    F:
				        aconst_null
				        areturn
				    G:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(DeadCodeRemovingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("exceptions:", dis), "Try-catch blocks should have been removed");
			assertFalse(dis.contains("Foo.b"), "Expected to remove dead method call");
			assertFalse(dis.contains("Foo.c"), "Expected to remove dead method call");
			assertFalse(dis.contains("pop"), "Expected to remove dead handler pop");
		});
	}

	@Test
	void dontFoldTryCatchThatIsNotDeadCode() {
		String asm = """
				.method public static example ()Ljava/lang/String; {
				    exceptions: {
				        { A, B, C, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        invokestatic Foo.get ()Ljava/lang/String;
				    B:
				        athrow
				    C:
				        aconst_null
				        areturn
				    D:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(DeadCodeRemovingTransformer.class));

		asm = """
				.method public static example ()Ljava/lang/String; {
				    exceptions: {
				        { A, B, C, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        invokestatic Foo.get ()Ljava/lang/String;
				        areturn
				    B:
				        nop
				    C:
				        aconst_null
				        areturn
				    D:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(DeadCodeRemovingTransformer.class), dis -> {
			assertTrue(dis.contains("Ljava/lang/Exception;"), "Expected to keep exception");
			assertTrue(dis.contains("Foo.get"), "Expected to keep method call");
			assertTrue(dis.contains("aconst_null"), "Expected to keep handler");
			assertFalse(dis.contains("nop"), "Expected to remove dead code");
		});

		asm = """
				.method public static example ()Ljava/lang/String; {
				    exceptions: {
				        { B, D, E, Ljava/lang/Exception; }
				    },
				    code: {
				    A:
				        goto C
				    B:
				        invokestatic Foo.dead ()V
				    C:
				        invokestatic Foo.get ()Ljava/lang/String;
				    D:
				        goto F
				    E:
				        pop
				        aconst_null
				    F:
				        areturn
				    G:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(DeadCodeRemovingTransformer.class), dis -> {
			assertTrue(dis.contains("Ljava/lang/Exception;"), "Expected to keep exception");
			assertFalse(dis.contains("Foo.dead"), "Expected to remove dead method call");
			assertTrue(dis.contains("Foo.get"), "Expected to keep method call");
			assertTrue(dis.contains("pop"), "Expected to keep handler pop");
		});
	}

	/** Simple case used to cover base case in transformer impl */
	@Test
	void foldImmediateGotoNext() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        goto B
				    B:
				        return
				    C:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(GotoInliningTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("goto", dis), "Expected to replace all goto <target> with inlining");
			assertEquals(0, StringUtil.count("C:", dis), "Expected to simplify out 'C' label, only two labels needed in this example after inlining");
		});
	}

	@Test
	void foldUselessGoto() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        goto D
				    B:
				        goto C
				    C:
				        goto G
				    D:
				        goto H
				    E:
				        goto L
				    F:
				        goto N
				    G:
				        goto E
				    H:
				        goto M
				    I:
				        goto J
				    J:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        ldc "Hello world"
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    K:
				        goto I
				    L:
				        goto K
				    M:
				        goto F
				    N:
				        goto B
				    O:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(GotoInliningTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("goto", dis), "Expected to replace all goto <target> with inlining");
		});
	}

	/** Showcase pairing of {@link OpaquePredicateFoldingTransformer} with {@link GotoInliningTransformer} */
	@Test
	void foldOpaquePredicatesIntoGotosThenInlineTheGotos() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_0
				        ifeq D
				        goto E
				    B:
				        iconst_1
				        ifne C
				        goto E
				    C:
				        iconst_0
				        ifne E
				        goto G
				    D:
				        iconst_0
				        ifeq H
				        goto N
				    E:
				        iconst_2
				        ifeq N
				        goto L
				    F:
				        iconst_2
				        ifne N
				        iconst_0
				        ifeq A
				        goto L
				    G:
				        iconst_0
				        ifeq E
				        goto N
				    H:
				        iconst_1
				        ifeq L
				        goto M
				    I:
				        iconst_4
				        ifeq A
				    J:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        ldc "Hello world"
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    K:
				        iconst_5
				        ifne I
				        goto L
				    L:
				        iconst_0
				        ifne J
				        goto K
				    M:
				        iconst_2
				        ifne F
				        goto N
				    N:
				        goto B
				    O:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class, GotoInliningTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("goto", dis), "Expected to replace all goto <target> with inlining");
		});
	}

	@Test
	void doNotFoldGotoCycle() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        goto A
				    B:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(GotoInliningTransformer.class));
	}

	@Test
	void doNotFoldGotoOfTransitionBlockCycle() {
		String asm = """
				.method public static example ()V {
				    code: {
				    A:
				        iconst_1
				        pop
				        // block "A" naturally flows into block "B"
				    B:
				        iconst_2
				        pop
				        // block "B" naturally flows into block "C"
				    C:
				        iconst_3
				        ifne D
				        // We should not inline block "B" here because "A" --> "B" would be broken if we did that.
				        goto B
				    D:
				        return
				    E:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(GotoInliningTransformer.class));
	}

	@Test
	void doNotFoldGotoInsideTryRangeWithCodeOutsideOfTryRange() {
		// Because this would technically be a behavior change (unless we do lots of more analysis)
		// we do not inline goto instructions that span the boundaries of try-catch.
		//
		// You would first need to apply a transformer to remove the try-catch if it is not actually useful.
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  E0,  E1,  EH, Ljava/lang/Throwable; }
				    },
				    code: {
				    E0:
				        goto X
				    E1:
				    EH:
				        athrow
				    X:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        ldc "Hello world"
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    Z:
				    }
				}
				""";
		validateNoTransformation(asm, List.of(GotoInliningTransformer.class));
	}

	@Test
	void doNotFoldGotoOutsideTryRangeWithCodeInsideOfTryRange() {
		// Because this would technically be a behavior change (unless we do lots of more analysis)
		// we do not inline goto instructions that span the boundaries of try-catch.
		//
		// You would first need to apply a transformer to remove the try-catch if it is not actually useful.
		String asm = """
				.method public static example ()V {
					exceptions: {
				       {  E0,  E1,  EH, Ljava/lang/Throwable; }
				    },
				    code: {
				    A:
				        goto E0
				    E0:
				        getstatic java/lang/System.out Ljava/io/PrintStream;
				        ldc "Hello world"
				        invokevirtual java/io/PrintStream.println (Ljava/lang/String;)V
				        return
				    E1:
				    EH:
				        athrow
				    }
				}
				""";
		validateNoTransformation(asm, List.of(GotoInliningTransformer.class));
	}
}
