package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.util.List;

/**
 * Wrapper for results of script execution calls.
 *
 * @author Matt Coley
 * @see ScriptEngine#run(String)
 * @see ScriptEngine#run(GenerateResult)
 */
public class ScriptResult {
	private final List<CompilerDiagnostic> diagnostics;
	private final Throwable throwable;
	private final boolean cancelled;

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 */
	public ScriptResult(@Nonnull List<CompilerDiagnostic> diagnostics) {
		this(diagnostics, null);
	}

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 * @param throwable
	 * 		Runtime error value.
	 */
	public ScriptResult(@Nonnull List<CompilerDiagnostic> diagnostics, @Nullable Throwable throwable) {
		this(diagnostics, throwable, false);
	}

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 * @param throwable
	 * 		Runtime error value.
	 * @param cancelled
	 * 		Flag indicating cancellation was requested and observed.
	 */
	public ScriptResult(@Nonnull List<CompilerDiagnostic> diagnostics, @Nullable Throwable throwable, boolean cancelled) {
		this.diagnostics = diagnostics;
		this.throwable = throwable;
		this.cancelled = cancelled;
	}

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 *
	 * @return Cancelled script result.
	 */
	@Nonnull
	public static ScriptResult cancelled(@Nonnull List<CompilerDiagnostic> diagnostics) {
		return new ScriptResult(diagnostics, null, true);
	}

	/**
	 * @return {@code true} when there were no compiler or runtime errors.
	 */
	public boolean wasSuccess() {
		return !wasCompileFailure() && !wasRuntimeError() && !wasCancelled();
	}

	/**
	 * @return {@code true} when {@link #getCompileDiagnostics()} has content.
	 */
	public boolean wasCompileFailure() {
		return !getCompileDiagnostics().isEmpty();
	}

	/**
	 * @return {@code true} when {@link #getRuntimeThrowable()} is present.
	 */
	public boolean wasRuntimeError() {
		return throwable != null;
	}

	/**
	 * @return {@code true} when script execution was cancelled.
	 */
	public boolean wasCancelled() {
		return cancelled;
	}

	/**
	 * @return List of compiler diagnostics.
	 */
	@Nonnull
	public List<CompilerDiagnostic> getCompileDiagnostics() {
		return diagnostics;
	}

	/**
	 * @return Exception thrown when running the generated script method.
	 */
	@Nullable
	public Throwable getRuntimeThrowable() {
		return throwable;
	}
}
