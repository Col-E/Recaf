package software.coley.recaf.services.script;

import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.util.List;

/**
 * Wrapper for results of {@link ScriptEngine#run(String)} calls.
 *
 * @author Matt Coley
 */
public class ScriptResult {
	private final List<CompilerDiagnostic> diagnostics;
	private final Throwable throwable;

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 */
	public ScriptResult(List<CompilerDiagnostic> diagnostics) {
		this(diagnostics, null);
	}

	/**
	 * @param diagnostics
	 * 		Compiler error list.
	 * @param throwable
	 * 		Runtime error value.
	 */
	public ScriptResult(List<CompilerDiagnostic> diagnostics, Throwable throwable) {
		this.diagnostics = diagnostics;
		this.throwable = throwable;
	}

	/**
	 * @return {@code true} when there were no compiler or runtime errors.
	 */
	public boolean wasSuccess() {
		return !wasCompileFailure() && !wasRuntimeError();
	}

	/**
	 * @return {@code true} when {@link #getCompileDiagnostics()} has content.
	 */
	public boolean wasCompileFailure() {
		return getCompileDiagnostics().size() > 0;
	}

	/**
	 * @return {@code true} when {@link #getRuntimeThrowable()} is present.
	 */
	public boolean wasRuntimeError() {
		return throwable != null;
	}

	/**
	 * @return List of compiler diagnostics.
	 */
	public List<CompilerDiagnostic> getCompileDiagnostics() {
		return diagnostics;
	}

	/**
	 * @return Exception thrown when running the generated script method.
	 */
	public Throwable getRuntimeThrowable() {
		return throwable;
	}
}
