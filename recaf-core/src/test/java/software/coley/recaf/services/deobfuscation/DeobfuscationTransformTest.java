package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
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
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.services.decompile.cfr.CfrConfig;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.deobfuscation.builtin.StaticValueInliningTransformer;
import software.coley.recaf.services.transform.TransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.List;
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
		assembler = recaf.get(JvmAssemblerPipeline.class);
		transformationApplier = transformationApplierService.newApplierForCurrentWorkspace();
	}

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
			validateBeforeAfter(asm, "println(foo);", "println(5);");

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
			validateBeforeAfter(asm, "println(foo);", "println(\"Hello\");");
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
			validateNoTransformation(asm);
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
			validateBeforeAfter(asm, "println(foo);", "println(5);");
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
			validateBeforeAfter(asm, "println(foo);", "println(5);");
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
			validateBeforeAfter(asm, "println(foo);", "println(25);");
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
			validateBeforeAfter(asm, "println(foo);", "println(\"Hello\");");
		}
	}

	private void validateNoTransformation(@Nonnull String assembly) {
		JvmClassInfo cls = assemble(assembly);

		// Transforming should not actually result in any changes
		TransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm( List.of(StaticValueInliningTransformer.class)));
		assertTrue(result.getJvmTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(0, result.getJvmTransformedClasses().size(), "There were unexpected transformations applied");
	}

	private void validateBeforeAfter(@Nonnull String assembly, @Nonnull String expectedBefore, @Nonnull String expectedAfter) {
		JvmClassInfo cls = assemble(assembly);

		// Before transformation, check that the expected before-state is matched
		String initialDecompile = decompile(cls);
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + initialDecompile);
		assertTrue(initialDecompile.contains(expectedBefore));

		// Run the transformer
		TransformResult result = assertDoesNotThrow(() -> transformationApplier.transformJvm( List.of(StaticValueInliningTransformer.class)));
		assertTrue(result.getJvmTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getJvmTransformedClasses().size(), "Expected transformation to be applied");

		// Validate output has been transformed to match the expected after-state.
		String transformedDecompile = decompileTransformed(result);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDecompile);
		assertTrue(transformedDecompile.contains(expectedAfter));
	}

	@Nonnull
	private String decompileTransformed(@Nonnull TransformResult result) {
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