package me.coley.recaf.decompile;

import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.code.ClassInfo;

import java.util.Map;

/**
 * Decompiler wrapper.
 *
 * @author Matt Coley
 */
public abstract class Decompiler extends Tool<DecompileOption<?>> {
	protected Decompiler(String name, String version) {
		super(name, version);
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
	public DecompileResult decompile(Map<String, DecompileOption<?>> options, Workspace workspace,
									 ClassInfo classInfo) {
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
	protected abstract String decompileImpl(Map<String, DecompileOption<?>> options, Workspace workspace,
											ClassInfo classInfo);
}
