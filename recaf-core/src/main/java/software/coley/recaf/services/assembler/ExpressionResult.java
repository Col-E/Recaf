package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import me.darknet.assembler.ast.specific.ASTMethod;
import software.coley.recaf.services.compile.CompilerDiagnostic;

import java.util.Collections;
import java.util.List;

/**
 * Result of an expression compilation attempt.
 *
 * @author Matt Coley
 */
public class ExpressionResult {
	private final String assembly;
	private final List<CompilerDiagnostic> diagnostics;
	private final ExpressionCompileException exception;

	/**
	 * @param assembly
	 * 		Output JASM from the input expression.
	 */
	public ExpressionResult(@Nonnull String assembly) {
		this.assembly = assembly;
		this.diagnostics = Collections.emptyList();
		this.exception = null;
	}

	/**
	 * @param diagnostics
	 * 		Compiler warnings and errors from the input compilation attempt.
	 */
	public ExpressionResult(@Nonnull List<CompilerDiagnostic> diagnostics) {
		this.assembly = null;
		this.diagnostics = diagnostics;
		this.exception = null;
	}

	/**
	 * @param exception
	 * 		Exception thrown during the code-building or compilation process.
	 */
	public ExpressionResult(@Nonnull ExpressionCompileException exception) {
		this.assembly = null;
		this.diagnostics = Collections.emptyList();
		this.exception = exception;
	}

	/**
	 * @return The assembled expression.
	 */
	@Nullable
	public String getAssembly() {
		return assembly;
	}

	/**
	 * @return Exception encountered when compiling the expression.
	 */
	@Nullable
	public ExpressionCompileException getException() {
		return exception;
	}

	/**
	 * @return List of compiler errors encountered when compiling the expression.
	 */
	@Nonnull
	public List<CompilerDiagnostic> getDiagnostics() {
		return diagnostics;
	}

	/**
	 * @return {@code true} when the expression was compiled and a {@link #getAssembly() method AST} was created.
	 */
	public boolean wasSuccess() {
		return assembly != null;
	}
}
