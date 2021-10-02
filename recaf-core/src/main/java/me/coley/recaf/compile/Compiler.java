package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.Tool;

import java.io.File;
import java.util.Collection;
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

	/**
	 * Convenience call for {@link #setClasspath(Map, String)}.
	 *
	 * @param options
	 * 		Options map to update.
	 * @param classpath
	 * 		Multiple file paths to include.
	 */
	public void setClasspath(Map<String, CompileOption<?>> options, Collection<String> classpath) {
		setClasspath(options, String.join(File.pathSeparator, classpath));
	}

	/**
	 * @param options
	 * 		Options map to update.
	 * @param classpath
	 * 		Path with one or more file paths split by {@link File#separatorChar}.
	 */
	public abstract void setClasspath(Map<String, CompileOption<?>> options, String classpath);

	/**
	 * @param options
	 * 		Options map to update.
	 * @param version
	 * 		Target version to support.
	 * 		Java 8 will simply be 8. The current version can be retrieved via: {@link me.coley.recaf.util.JavaVersion}.
	 */
	public abstract void setTarget(Map<String, CompileOption<?>> options, int version);

	/**
	 * @param options
	 * 		Options map to update.
	 * @param debug
	 * 		Debug parameters.
	 */
	public abstract void setDebug(Map<String, CompileOption<?>> options, String debug);
}
