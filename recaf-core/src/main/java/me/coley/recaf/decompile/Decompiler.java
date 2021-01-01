package me.coley.recaf.decompile;

import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.ClassInfo;

import java.util.Map;
import java.util.Objects;

/**
 * Decompiler wrapper.
 *
 * @author Matt Coley
 */
public abstract class Decompiler implements Comparable<Decompiler> {
	private final Map<String, DecompileOption<?>> defaultOptions = createDefaultOptions();
	private final String name;
	private final String version;

	protected Decompiler(String name, String version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * @return Decompiler name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Decompiler version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return Map of default options used by the decompiler.
	 */
	public Map<String, DecompileOption<?>> getDefaultOptions() {
		return defaultOptions;
	}

	/**
	 * Decompile a class, using default options.
	 *
	 * @param workspace
	 * 		Workspace to supply additional classes for reference.
	 * 		Can be {@code null} at the cost of accuracy depending on the decompiler implementation.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Result from decompilation.
	 * Wraps either the code decompiled or the error information that prevented decompilation.
	 */
	public DecompileResult decompile(Workspace workspace, ClassInfo classInfo) {
		return decompile(getDefaultOptions(), workspace, classInfo);
	}

	/**
	 * Decompile a class.
	 *
	 * @param options
	 * 		Decompiler options.
	 * @param workspace
	 * 		Workspace to supply additional classes for reference.
	 * 		Can be {@code null} at the cost of accuracy depending on the decompiler implementation.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Result from decompilation.
	 * Wraps either the code decompiled or the error information that prevented decompilation.
	 */
	public DecompileResult decompile(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo) {
		try {
			return new DecompileResult(this, classInfo, decompileImpl(options, workspace, classInfo));
		} catch (Exception ex) {
			return new DecompileResult(this, classInfo, ex);
		}
	}

	/**
	 * Internal decompile call that will be implemented by implementations.
	 *
	 * @param options
	 * 		Decompiler options.
	 * @param workspace
	 * 		Workspace to pull additional classes if the implementation needs to reference separate classes.
	 * 		May be {@code null}.
	 * @param classInfo
	 * 		Class to decompile.
	 *
	 * @return Decompiled code of a class.
	 */
	protected abstract String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace, ClassInfo classInfo);

	/**
	 * @return Map of options used by the decompiler as a default.
	 */
	protected abstract Map<String, DecompileOption<?>> createDefaultOptions();

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Decompiler)) return false;
		Decompiler that = (Decompiler) o;
		return name.equals(that.name) &&
				version.equals(that.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, version);
	}

	@Override
	public int compareTo(Decompiler o) {
		return getName().compareTo(o.getName());
	}
}
