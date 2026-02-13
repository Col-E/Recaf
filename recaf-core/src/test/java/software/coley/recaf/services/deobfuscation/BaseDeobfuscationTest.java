package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.visitor.JavaCompileResult;
import me.darknet.assembler.error.Error;
import me.darknet.assembler.error.Result;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.assembler.AssemblerPipelineManager;
import software.coley.recaf.services.assembler.JvmAssemblerPipeline;
import software.coley.recaf.services.compile.CompilerResult;
import software.coley.recaf.services.compile.JavacArguments;
import software.coley.recaf.services.compile.JavacArgumentsBuilder;
import software.coley.recaf.services.compile.JavacCompiler;
import software.coley.recaf.services.decompile.DecompileResult;
import software.coley.recaf.services.decompile.JvmDecompiler;
import software.coley.recaf.services.decompile.cfr.CfrConfig;
import software.coley.recaf.services.decompile.cfr.CfrDecompiler;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.EmptyWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.BasicWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Common setup for deobfuscation tests.
 */
public abstract class BaseDeobfuscationTest extends TestBase {
	private static final boolean PRINT_BEFORE_AFTER = true;
	protected static final String CLASS_NAME = "Example";
	protected static final String EXCEPTION_NAME = "BogusException";
	private static JvmAssemblerPipeline assembler;
	private static TransformationApplierService transformationApplierService;
	private static JavacCompiler javac;
	private static JvmDecompiler decompiler;
	private static Workspace workspace;

	@BeforeAll
	static void setupServices() {
		transformationApplierService = recaf.get(TransformationApplierService.class);
		javac = recaf.get(JavacCompiler.class);
		decompiler = new CfrDecompiler(recaf.get(WorkspaceManager.class), new CfrConfig());
	}

	@BeforeEach
	void setupWorkspace() {
		workspace = new BasicWorkspace(new WorkspaceResourceBuilder().build());
		workspace.getPrimaryResource().getJvmClassBundle().put(TestClassUtils.createClass(EXCEPTION_NAME, n -> n.superName = "java/lang/Exception"));
		workspaceManager.setCurrentIgnoringConditions(workspace);
		assembler = recaf.get(AssemblerPipelineManager.class).newJvmAssemblerPipeline(workspace);
	}

	protected void validateNoTransformation(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers) {
		boolean isFullBody = assembly.contains(".class");
		JvmClassInfo cls = assemble(assembly, isFullBody);

		// Transforming should not actually result in any changes
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier().transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		if (!result.getTransformedClasses().isEmpty()) {
			String transformedDisassembly = disassembleTransformed(result, isFullBody);
			fail("There were unexpected transformations applied:\n\n" + transformedDisassembly);
		}
	}

	protected void validateBeforeAfterDecompile(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                            @Nonnull String expectedBefore, @Nullable String expectedAfter) {
		boolean isFullBody = assembly.contains(".class");
		JvmClassInfo cls = assemble(assembly, isFullBody);

		// Before transformation, check that the expected before-state is matched
		String initialDecompile = decompile(cls);
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + initialDecompile);
		assertTrue(initialDecompile.contains(expectedBefore));

		// Run the transformer
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier().transformJvm(transformers));
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

	protected void validateAfterAssembly(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                     @Nonnull Consumer<String> assertionChecker) {
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + assembly);
		boolean isFullBody = assembly.contains(".class");

		// Run the transformer.
		JvmClassInfo cls = assemble(assembly, isFullBody);
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier().transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getTransformedClasses().size(), "Expected transformation to be applied");

		// Validate output has been transformed to match the expected after-state.
		String transformedDisassembly = disassembleTransformed(result, isFullBody);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDisassembly);
		assertionChecker.accept(transformedDisassembly);
	}

	protected void validateAfterRepeatedAssembly(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                             @Nonnull Consumer<String> assertionChecker) {
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + assembly);
		boolean isFullBody = assembly.contains(".class");

		// Run the transformer until no changes are observed.
		JvmClassInfo cls = assemble(assembly, isFullBody);
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier(10).transformJvm(transformers));

		// No transform step should fail.
		result.getTransformerFailures().forEach((path, failureMap) -> {
			System.err.println(path.getValue().getName());
			failureMap.forEach((transformer, error) -> {
				System.err.println(transformer.getSimpleName());
				error.printStackTrace(System.err);
				System.err.println();
			});
		});

		// Validate output has been transformed without any errors.
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertEquals(1, result.getTransformedClasses().size(), "Expected transformation to be applied");

		// Update the assembly to hold the transformed output.
		// Validate the new disassembly matches our assertion checker's assumptions.
		assembly = disassembleTransformed(result, isFullBody);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + assembly);
		assertionChecker.accept(assembly);
	}

	protected void validateMappingAfterAssembly(@Nonnull String assembly, @Nonnull List<Class<? extends JvmClassTransformer>> transformers,
	                                            @Nonnull Consumer<String> assertionChecker) {
		if (PRINT_BEFORE_AFTER) System.out.println("======== BEFORE ========\n" + assembly);
		boolean isFullBody = assembly.contains(".class");

		// Run the transformer
		JvmClassInfo cls = assemble(assembly, isFullBody);
		JvmTransformResult result = assertDoesNotThrow(() -> newApplier().transformJvm(transformers));
		assertTrue(result.getTransformerFailures().isEmpty(), "There were transformation failures");
		assertFalse(result.getMappingsToApply().isEmpty(), "Expected transformation to register mappings");

		// Validate output has been transformed to match the expected after-state.
		String transformedDisassembly = disassembleTransformed(result, isFullBody);
		if (PRINT_BEFORE_AFTER) System.out.println("========= AFTER ========\n" + transformedDisassembly);
		assertionChecker.accept(transformedDisassembly);
	}

	@Nonnull
	protected String disassembleTransformed(@Nonnull JvmTransformResult result, boolean isFullBody) {
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
	protected String decompileTransformed(@Nonnull JvmTransformResult result) {
		result.apply();
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		return decompile(cls);
	}

	@Nonnull
	protected String decompile(@Nonnull JvmClassInfo cls) {
		DecompileResult result = decompiler.decompile(workspace, cls);
		if (result.getText() == null)
			fail("Missing decompilation result");
		return result.getText();
	}

	@Nonnull
	protected String compile(@Nonnull String src, Class<?>... importedType) {
		WorkspaceResource resource = new WorkspaceResourceBuilder().build();
		JvmClassBundle bundle = resource.getJvmClassBundle();
		Workspace workspace = new BasicWorkspace(resource);
		JavacArguments args = new JavacArgumentsBuilder()
				.withClassName(CLASS_NAME)
				.withClassSource("%IMPORTS%\nclass %NAME% {\n%SRC%\n}"
						.replace("%IMPORTS%", Arrays.stream(importedType).map(c -> "import " + c.getName() + ";").collect(Collectors.joining("\n")))
						.replace("%NAME%", CLASS_NAME)
						.replace("%SRC%", src))
				.build();
		CompilerResult result = javac.compile(args, workspace, null);
		if (result.wasSuccess())
			result.getCompilations().forEach((name, code) -> bundle.put(name, new JvmClassInfoBuilder(code).build()));
		else
			fail("Failed to compile test input");
		JvmClassInfo cls = bundle.get(CLASS_NAME);
		return assembler.disassemble(PathNodes.classPath(workspace, resource, bundle, cls)).get();
	}

	@Nonnull
	protected JvmClassInfo assemble(@Nonnull String body, boolean isFullBody) {
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

	@Nonnull
	protected TransformationApplier newApplier() {
		return newApplier(1);
	}

	@Nonnull
	protected TransformationApplier newApplier(int passCount) {
		TransformationApplier applier = transformationApplierService.newApplierForCurrentWorkspace();
		assertNotNull(applier);
		applier.setMaxPasses(passCount);
		return applier;
	}
}
