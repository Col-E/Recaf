package software.coley.recaf.services.deobfuscation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeAll;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.transform.JvmClassTransformer;
import software.coley.recaf.services.transform.JvmTransformResult;
import software.coley.recaf.services.transform.TransformationApplier;
import software.coley.recaf.services.transform.TransformationApplierService;
import software.coley.recaf.test.CompilerTestBase;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Common setup for deobfuscation tests using transformers.
 */
public abstract class TransformerTestBase extends CompilerTestBase {
	private static final boolean PRINT_BEFORE_AFTER = true;
	private static TransformationApplierService transformationApplierService;

	@BeforeAll
	static void setupServices() {
		transformationApplierService = recaf.get(TransformationApplierService.class);
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
		return disassemble(isFullBody);
	}

	@Nonnull
	protected String decompileTransformed(@Nonnull JvmTransformResult result) {
		result.apply();
		return decompile(get(CLASS_NAME));
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
