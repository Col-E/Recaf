package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.util.List;

/**
 * Wrapper for results of {@link ScriptEngine#compile(String)} calls.
 *
 * @param cls
 * 		Compiled class reference. May be {@code null} when compilation failed.
 * @param diagnostics
 * 		Compiler diagnostic messages.
 *
 * @author Matt Coley
 */
public record GenerateResult(@Nullable Class<?> cls, @Nonnull List<CompilerDiagnostic> diagnostics) {
	/**
	 * @return {@code true} when compilation was a success.
	 */
	public boolean wasSuccess() {
		return cls != null;
	}
}