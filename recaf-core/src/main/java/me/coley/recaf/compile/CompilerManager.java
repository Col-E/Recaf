package me.coley.recaf.compile;

import me.coley.recaf.compile.javac.JavacCompiler;
import me.coley.recaf.plugin.tools.ToolManager;

/**
 * Manager of compilers.
 *
 * @author Matt Coley
 */
public class CompilerManager extends ToolManager<Compiler> {
	/**
	 * Initialize the compiler manager with local javac implementations.
	 */
	public CompilerManager() {
		register(new JavacCompiler());
	}
}
