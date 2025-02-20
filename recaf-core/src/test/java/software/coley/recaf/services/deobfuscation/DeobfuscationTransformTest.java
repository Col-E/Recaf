package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.services.decompile.cfr.CfrConfig;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.deobfuscation.transform.generic.DeadCodeRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateCatchMergingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.EnumNameRestorationTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.GotoInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalVarargsRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LinearOpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.RedundantTryCatchRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StackOperationFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.VariableFoldingTransformer;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for various deobfuscation focused transformers.
 */
class DeobfuscationTransformTest extends TestBase {
	private static final boolean PRINT_BEFORE_AFTER = true;
	private static final String CLASS_NAME = "Example";
	private static JvmAssemblerPipeline assembler;
	private static TransformationApplierService transformationApplierService;
	private static TransformationApplier transformationApplier;
	private static JvmDecompiler decompiler;
	private static Workspace workspace;

	@BeforeAll
	static void setupServices() {
		transformationApplierService = recaf.get(TransformationApplierService.class);
		decompiler = new CfrDecompiler(new CfrConfig());
	}

	@BeforeEach
	void setupWorkspace() {
		workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		workspaceManager.setCurrentIgnoringConditions(workspace);
		assembler = recaf.get(AssemblerPipelineManager.class).newJvmAssemblerPipeline(workspace);
		transformationApplier = transformationApplierService.newApplierForCurrentWorkspace();
	}

	/**
	 * @see StaticValueInliningTransformer
	 * @see StaticValueCollectionTransformer
	 */
	@Nested
	class StaticValueInlining {
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

	/**
	 * For transformers that generally remove/cleanup content.
	 */
	@Nested
	class Removals {
		@Test
		void illegalSignatureRemoving() {
			// An int primitive is an invalid argument for a field signature (Valhalla when Brian?).
			// Most other invalid signatures aren't visible via decompilation so this suffices to show the transformer works.
			// Most of it delegates to code that is tested elsewhere.
			String asm = """
					.signature "Ljava/util/List<I>;"
					.field private static foo Ljava/lang/List;
					""";
			validateBeforeAfterDecompile(asm, List.of(IllegalSignatureRemovingTransformer.class), "List<int> foo", "List foo");
		}

		@Test
		void illegalVarargsRemoving() {
			// CFR actually checks for illegal varargs use and emits a nice warning for us.
			// So we'll just check if that goes away.
			String asm = """
					.method public static varargs example ([I[II)V {
					    code: {
					    A:
					        return
					    B:
					    }
					}
					""";
			validateBeforeAfterDecompile(asm, List.of(IllegalVarargsRemovingTransformer.class), "/* corrupt varargs signature?! */", null);
		}

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

		@Test
		void enumNameRestoration() {
			String asm = """
					.super java/lang/Enum
					.class public final enum Example {
					    .field public static final enum a LExample;
					    .field public static final enum b LExample;
					    .field public static final enum c LExample;
					    .field public static final enum d LExample;
					    .field private static final x [LExample;
										
					    .method static <clinit> ()V {
					        code: {
					        A:
					            new Example
					            dup
					            ldc "STATIC"
					            iconst_0
					            invokespecial Example.<init> (Ljava/lang/String;I)V
					            putstatic Example.a LExample;
					        B:
					            new Example
					            dup
					            ldc "WORLDGEN"
					            iconst_1
					            invokespecial Example.<init> (Ljava/lang/String;I)V
					            putstatic Example.b LExample;
					        C:
					            new Example
					            dup
					            ldc "DIMENSIONS"
					            iconst_2
					            invokespecial Example.<init> (Ljava/lang/String;I)V
					            putstatic Example.c LExample;
					        D:
					            new Example
					            dup
					            ldc "RELOADABLE"
					            iconst_3
					            invokespecial Example.<init> (Ljava/lang/String;I)V
					            putstatic Example.d LExample;
					        E:
					            invokestatic Example.$values ()[LExample;
					            putstatic Example.x [LExample;
					        F:
					            return
					        G:
					        }
					    }
					}
					""";
			validateMappingAfterAssembly(asm, List.of(EnumNameRestorationTransformer.class), dis -> {
				assertEquals(1, StringUtil.count("enum STATIC LExample;", dis), "Missing enum const mapping");
				assertEquals(1, StringUtil.count("enum WORLDGEN LExample;", dis), "Missing enum const mapping");
				assertEquals(1, StringUtil.count("enum DIMENSIONS LExample;", dis), "Missing enum const mapping");
				assertEquals(1, StringUtil.count("enum RELOADABLE LExample;", dis), "Missing enum const mapping");
				assertEquals(1, StringUtil.count("final $values [LExample;", dis), "Missing enum $values array mapping");
			});
		}
	}

	@Nested
	class Folding {
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
		void foldConversions() {
			String asm = """
					.method public static example ()I {
					    code: {
					    A:
					        ldc 5.125
					        d2l
					        l2f
					        f2i
					        ireturn
					    B:
					    }
					}
					""";
			validateAfterAssembly(asm, List.of(LinearOpaqueConstantFoldingTransformer.class), dis -> {
				assertEquals(1, StringUtil.count("iconst_5", dis), "Expected to fold to 5");
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

	@Nested
	class Regressions {
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

	private void validateNoTransformation(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers) {
		boolean isFullBody = assembly.contains(".class");
		JvmClassInfo cls = assemble(assembly, isFullBody);

		// Transforming should not actually result in any changes
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(0, result.getTransformerFailures().size(), "There were unexpected transformations applied");
	}

	private void validateBeforeAfterDecompile(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                          @Nonnull String expectedBefore, @Nullable String expectedAfter) {
		boolean isFullBody = assembly.contains(".class");
		JvmClassInfo cls = assemble(assembly, isFullBody);

		// Before transformation, check that the expected before-state is matched
		String initialDecompile = decompile(cls);
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + initialDecompile);
		assertTrue(initialDecompile.contains(expectedBefore));

		// Run the transformer
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getTransformedClasses().size(), "Expected transformation to be applied");

		// Validate output has been transformed to match the expected after-state.
		String transformedDecompile = decompileTransformed(result);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDecompile);
		if (expectedAfter == null)
			// If the 'after' is null, we should just check if the 'before' no longer exists
			assertFalse(transformedDecompile.contains(expectedBefore));
		else
			// Otherwise, check if the 'after' exists
			assertTrue(transformedDecompile.contains(expectedAfter));
	}

	private void validateAfterAssembly(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                   @Nonnull Consumer<String> assertionChecker) {
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + assembly);
		boolean isFullBody = assembly.contains(".class");

		// Run the transformer
		JvmClassInfo cls = assemble(assembly, isFullBody);
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getTransformedClasses().size(), "Expected transformation to be applied");

		// Validate output has been transformed to match the expected after-state.
		String transformedDisassembly = disassembleTransformed(result, isFullBody);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDisassembly);
		assertionChecker.accept(transformedDisassembly);
	}

	private void validateMappingAfterAssembly(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                          @Nonnull Consumer<String> assertionChecker) {
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + assembly);
		boolean isFullBody = assembly.contains(".class");

		// Run the transformer
		JvmClassInfo cls = assemble(assembly, isFullBody);
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertFalse(result.getMappingsToApply().isEmpty(), "Expected transformation to register mappings");

		// Validate output has been transformed to match the expected after-state.
		String transformedDisassembly = disassembleTransformed(result, isFullBody);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDisassembly);
		assertionChecker.accept(transformedDisassembly);
	}

	@Nonnull
	private String disassembleTransformed(@Nonnull JvmTransformResult result, boolean isFullBody) {
		result.apply();
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		Result<String> disassembly;
		if (isFullBody) {
			ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, cls);
			disassembly = assembler.disassemble(path);
		} else {
			MethodMember method = cls.getFirstDeclaredMethodByName("example");
			if (method == null)
				fail("Failed to find 'example' method, cannot disassemble");
			ClassMemberPathNode path = PathNodes.memberPath(workspace, resource, bundle, cls, method);
			disassembly = assembler.disassemble(path);
		}

		if (disassembly.isOk())
			return disassembly.get();
		fail(disassembly.errors().stream().map(Error::toString).collect(Collectors.joining("\n")));
		return "<error>";
	}

	@Nonnull
	private String decompileTransformed(@Nonnull JvmTransformResult result) {
		result.apply();
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		return decompile(cls);
	}

	@Nonnull
	private String decompile(@Nonnull JvmClassInfo cls) {
		DecompileResult result = decompiler.decompile(workspace, cls);
		if (result.getText() == null)
			fail("Missing decompilation result");
		return result.getText();
	}

	@Nonnull
	private JvmClassInfo assemble(@Nonnull String body, boolean isFullBody) {
		String assembly = isFullBody ? body : """
				.super java/lang/Object
				.class public super %NAME% {
				%CODE%
				}
				""".replace("%NAME%", CLASS_NAME).replace("%CODE%", body);
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		ClassPathNode path = PathNodes.classPath(workspace, resource, bundle, new StubClassInfo(CLASS_NAME).asJvmClass());
		Result<JavaCompileResult> result = assembler.tokenize(assembly, "<assembly>")
				.flatMap(assembler::roughParse)
				.flatMap(assembler::concreteParse)
				.flatMap(concreteAst -> assembler.assemble(concreteAst, path))
				.ifErr(errors -> fail("Errors assembling test input:\n - " + errors.stream().map(Error::toString).collect(Collectors.joining("\n - "))));
		JavaClassRepresentation representation = result.get().representation();
		if (representation == null) fail("No assembler output for test case");
		JvmClassInfo cls = new JvmClassInfoBuilder(representation.classFile()).build();
		bundle.put(cls);
		return cls;
	}
}