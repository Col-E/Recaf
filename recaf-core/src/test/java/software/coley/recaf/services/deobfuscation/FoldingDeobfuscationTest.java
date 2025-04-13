package software.coley.recaf.services.deobfuscation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LinearOpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StackOperationFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.util.StringUtil;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("lconst_0", dis), "Expected to fold to 0L");
		});
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("i2f", dis), "Expected to fold conversion");
			assertEquals(1, StringUtil.count("ldc 10.0F", dis), "Expected to fold to converted value");
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to fold to 0");
		});
	}

	@Test
	@Disabled("Requires an impl for the ReInterpreter lookups")
	void foldMethodCalls() {
		String asm = """
				.method public static example ()I {
				    code: {
				    A:
				        iconst_1
				        iconst_5
				        invokestatic java/lang/Math.min (II)I
				        ireturn
				    B:
				    }
				}
				""";
		validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(1, StringUtil.count("iconst_1", dis), "Expected to fold to 1");
			assertEquals(0, StringUtil.count("iconst_5", dis), "Expected to prune argument");
			assertEquals(0, StringUtil.count("Math.min", dis), "Expected to prune method call");
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

	/** Showcase pairing of {@link VariableFoldingTransformer} with {@link StackOperationFoldingTransformer} */
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
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class, StackOperationFoldingTransformer.class), dis -> {
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
				StackOperationFoldingTransformer.class,
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
		validateAfterAssembly(asm, List.of(VariableFoldingTransformer.class, StackOperationFoldingTransformer.class,
				LinearOpaqueConstantFoldingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("istore", dis), "Expected to remove redundant istore");
			assertEquals(0, StringUtil.count("iload", dis), "Expected to inline redundant iload");
			assertEquals(1, StringUtil.count("iconst_0", dis), "Expected to have single iconst_0");
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
		validateAfterRepeatedAssembly(asm, List.of(VariableFoldingTransformer.class, StackOperationFoldingTransformer.class, LinearOpaqueConstantFoldingTransformer.class), dis -> {
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
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class, StackOperationFoldingTransformer.class));
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
		validateNoTransformation(asm, List.of(VariableFoldingTransformer.class, StackOperationFoldingTransformer.class));
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
		validateAfterAssembly(asm, List.of(OpaquePredicateFoldingTransformer.class, DeadCodeRemovingTransformer.class), dis -> {
			assertEquals(0, StringUtil.count("exceptions:", dis), "Try-catch blocks should have been removed");
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
