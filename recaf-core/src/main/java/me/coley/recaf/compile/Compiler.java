package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.Tool;

import java.util.Map;

/**
 * Compiler wrapper.
 *
 * @author Matt Coley
 */
public abstract class Compiler extends Tool<CompileOption<?>> {
	protected Compiler(String name, String version) {
		super(name, version);
	}

	/**
	 * @param className
	 * 		Name of class represented by source.
	 * @param classSource
	 * 		Class source.
	 *
	 * @return Result of compilation attempt.
	 */
	public CompilerResult compile(String className, String classSource) {
		// Create new default options since default options may produce new values depending on invoking context
		Map<String, CompileOption<?>> options = createDefaultOptions();
		return compile(className, classSource, options);
	}

	/**
	 * @param className
	 * 		Name of class represented by source.
	 * @param classSource
	 * 		Class source.
	 * @param options
	 * 		Compiler options.
	 *
	 * @return Result of compilation attempt.
	 */
	public abstract CompilerResult compile(String className, String classSource, Map<String, CompileOption<?>> options);
}
