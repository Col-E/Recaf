package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.ToolResult;

import java.util.Collections;
import java.util.List;

/**
 * Compilation result wrapper.
 *
 * @author Matt Coley
 */
public class CompilerResult extends ToolResult<Compiler, CompileMap> {
	private final List<CompilerDiagnostic> errors;

	/**
	 * Result with class compilations.
	 *
	 * @param compiler
	 * 		Compiler responsible for the compilation map.
	 * @param compilations
	 * 		Resulting compiled classes.
	 */
	public CompilerResult(Compiler compiler, CompileMap compilations) {
		this(compiler, compilations, null, Collections.emptyList());
	}

	/**
	 * Result where compiler fails due to errors in the source.
	 *
	 * @param compiler
	 * 		Compiler responsible for the compilation map.
	 * @param errors
	 * 		Exception thrown when attempting to compile.
	 */
	public CompilerResult(Compiler compiler, List<CompilerDiagnostic> errors) {
		this(compiler, null, null, errors);
	}

	/**
	 * Result where compiler throws an exception.
	 *
	 * @param compiler
	 * 		Compiler responsible for the compilation map.
	 * @param exception
	 * 		Exception thrown when attempting to compile.
	 */
	public CompilerResult(Compiler compiler, Throwable exception) {
		this(compiler, null, exception, Collections.emptyList());
	}

	private CompilerResult(Compiler compiler, CompileMap compilations, Throwable exception,
						   List<CompilerDiagnostic> errors) {
		super(compiler, compilations, exception);
		this.errors = errors;
	}

	/**
	 * @return Compiler diagnostic outputs when the source contains errors.
	 */
	public List<CompilerDiagnostic> getErrors() {
		return errors;
	}
}
