package me.coley.recaf.compile;

import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.workspace.resource.Resource;

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
	 * @return {@code true} when the compiler can be invoked.
	 */
	public boolean isAvailable() {
		return true;
	}

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

	/**
	 * @param resources
	 * 		Resources to add.
	 */
	public void addVirtualClassPath(Iterable<? extends Resource> resources) {
		resources.forEach(this::addVirtualClassPath);
	}

	/**
	 * @param resource
	 * 		Resource to add.
	 */
	public abstract void addVirtualClassPath(Resource resource);

	/**
	 * Clears the virtual classpath of {@link Resource}s.
	 */
	public abstract void clearVirtualClassPath();
}
