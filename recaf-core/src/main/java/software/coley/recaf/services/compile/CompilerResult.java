package software.coley.recaf.services.compile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Compiler results wrapper.
 *
 * @author Matt Coley
 */
public class CompilerResult {
	private final CompileMap compilations;
	private final List<CompilerDiagnostic> diagnostics;
	private final Throwable exception;

	/**
	 * @param exception
	 * 		Error thrown when attempting to compile.
	 */
	public CompilerResult(@Nonnull Throwable exception) {
		this(new CompileMap(Collections.emptyMap()), Collections.emptyList(), exception);
	}

	/**
	 * @param compileMap
	 * 		Compilation results.
	 * @param diagnostics
	 * 		Compilation problem diagnostics.
	 */
	public CompilerResult(@Nonnull CompileMap compileMap, @Nonnull List<CompilerDiagnostic> diagnostics) {
		this(compileMap, diagnostics, null);
	}

	private CompilerResult(@Nonnull CompileMap compilations,
						   @Nonnull List<CompilerDiagnostic> diagnostics,
						   @Nullable Throwable exception) {
		this.compilations = compilations;
		this.exception = exception;
		this.diagnostics = diagnostics;
	}

	/**
	 * @return {@code true} when there are compilations, and no errors thrown.
	 */
	public boolean wasSuccess() {
		return compilations != null &&
				compilations.size() > 0 &&
				exception == null &&
				diagnostics.stream().noneMatch(d -> d.getLevel() == CompilerDiagnostic.Level.ERROR);
	}

	/**
	 * @return Compilation results.
	 * Empty when there are is an {@link #getException()}.
	 */
	@Nonnull
	public CompileMap getCompilations() {
		return compilations;
	}

	/**
	 * @return Compilation problem diagnostics.
	 * Empty when {@link #wasSuccess()}.
	 */
	@Nonnull
	public List<CompilerDiagnostic> getDiagnostics() {
		return diagnostics;
	}

	/**
	 * @return Error thrown when attempting to compile.
	 * {@code null} when compilation was a success.
	 */
	@Nullable
	public Throwable getException() {
		return exception;
	}
}
