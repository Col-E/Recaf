package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.ToolResult;

/**
 * Compilation result wrapper.
 *
 * @author Matt Coley
 */
public class CompilerResult extends ToolResult<Compiler, CompileMap> {
	/**
	 * @param compiler
	 * 		Compiler responsible for the compilation map.
	 * @param compilations
	 * 		Resulting compiled classes.
	 */
	protected CompilerResult(Compiler compiler, CompileMap compilations) {
		super(compiler, compilations, null);
	}

	/**
	 * @param compiler
	 * 		Compiler responsible for the compilation map.
	 * @param exception
	 * 		Exception thrown when attempting to compile.
	 */
	protected CompilerResult(Compiler compiler, Throwable exception) {
		super(compiler, null, exception);
		// TODO: Compilers may emit multiple outputs,
		//    it would be wise to bundle them in a custom exception type
		//    to conform to the ToolResult layout
	}
}
