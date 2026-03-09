package software.coley.recaf.services.script;

import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.util.ClassDefiner;

import java.util.List;
import java.util.Map;

sealed interface ScriptTemplate {

	GenerateResult generateResult();

	record Generated(
			String className,
			Map<String, byte[]> classMap,
			List<CompilerDiagnostic> diagnostics
	) implements ScriptTemplate {
		private static final DebuggingLogger logger = Logging.get(Generated.class);

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

	record Failed(List<CompilerDiagnostic> diagnostics) implements ScriptTemplate {

		@Override
		public GenerateResult generateResult() {
			return new GenerateResult(null, diagnostics);
		}
	}
}
