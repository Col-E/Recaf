package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.util.ClassDefiner;

import java.util.List;
import java.util.Map;

/**
 * Result of script template generation.
 *
 * @author xDark
 */
sealed interface ScriptTemplate {

	/**
	 * @return Result of the generation process.
	 */
	@Nonnull
	GenerateResult generateResult();

	/**
	 * Generation succeeded. A class was generated.
	 *
	 * @param className
	 * 		Name of the generated class.
	 * @param classMap
	 * 		Map of class names to bytecode for all generated classes.
	 * 		The main script class is included in this map.
	 * @param diagnostics
	 * 		Compiler diagnostics from the generation process.
	 * 		May include warnings or informational messages.
	 */
	record Generated(@Nonnull String className,
	                 @Nonnull Map<String, byte[]> classMap,
	                 @Nonnull List<CompilerDiagnostic> diagnostics) implements ScriptTemplate {
		private static final DebuggingLogger logger = Logging.get(Generated.class);

		@Nonnull
		@Override
		public GenerateResult generateResult() {
			try {
				ClassDefiner definer = new ClassDefiner(classMap);
				Class<?> cls = definer.findClass(className);
				return new GenerateResult(cls, diagnostics);
			} catch (Exception ex) {
				logger.error("Failed to define generated script class", ex);
			}
			return new GenerateResult(null, diagnostics);
		}
	}

	/**
	 * Generation failed. No class was generated.
	 *
	 * @param diagnostics
	 * 		Compiler diagnostics from the failed generation.
	 */
	record Failed(@Nonnull List<CompilerDiagnostic> diagnostics) implements ScriptTemplate {

		@Nonnull
		@Override
		public GenerateResult generateResult() {
			return new GenerateResult(null, diagnostics);
		}
	}
}
