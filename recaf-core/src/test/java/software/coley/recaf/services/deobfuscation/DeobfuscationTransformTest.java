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
import software.coley.recaf.services.deobfuscation.transform.generic.DuplicateCatchMergingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalSignatureRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.IllegalVarargsRemovingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.LinearOpaqueConstantFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.OpaquePredicateFoldingTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueCollectionTransformer;
import software.coley.recaf.services.deobfuscation.transform.generic.StaticValueInliningTransformer;
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
	}

	private void validateNoTransformation(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers) {
		JvmClassInfo cls = assemble(assembly);

		// Transforming should not actually result in any changes
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(0, result.getTransformerFailures().size(), "There were unexpected transformations applied");
	}

	private void validateBeforeAfterDecompile(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                          @Nonnull String expectedBefore, @Nullable String expectedAfter) {
		JvmClassInfo cls = assemble(assembly);

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

		// Run the transformer
		JvmClassInfo cls = assemble(assembly);
		JvmTransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getTransformedClasses().size(), "Expected transformation to be applied");

		// Validate output has been transformed to match the expected after-state.
		String transformedDisassembly = disassembleTransformed(result);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDisassembly);
		assertionChecker.accept(transformedDisassembly);
	}

	@Nonnull
	private String disassembleTransformed(@Nonnull JvmTransformResult result) {
		result.apply();
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		WorkspaceResource resource = workspace.getPrimaryResource();
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		MethodMember method = cls.getFirstDeclaredMethodByName("example");
		if (method == null)
			fail("Failed to find 'example' method, cannot disassemble");
		ClassMemberPathNode path = PathNodes.memberPath(workspace, resource, bundle, cls, method);
		Result<String> disassembly = assembler.disassemble(path);
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
	private JvmClassInfo assemble(@Nonnull String body) {
		String assembly = """
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