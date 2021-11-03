package me.coley.recaf.decompile;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.workspace.Workspace;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Decompiler wrapper.
 *
 * @author Matt Coley
 */
public abstract class Decompiler extends Tool<DecompileOption<?>> {
	private final List<DecompileInterceptor> decompileInterceptors = new ArrayList<>();

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
			String text = decompileImpl(options, workspace, classInfo);
			if (text != null)
				text = EscapeUtil.unescapeUnicode(text);
			return new DecompileResult(this, classInfo, text);
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

	/**
	 * @return List of current interceptors.
	 */
	public List<DecompileInterceptor> getDecompileInterceptors() {
		return Collections.unmodifiableList(decompileInterceptors);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to add.
	 */
	public void addDecompileInterceptor(DecompileInterceptor interceptor) {
		decompileInterceptors.add(interceptor);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to remove.
	 */
	public void removeDecompileInterceptor(DecompileInterceptor interceptor) {
		decompileInterceptors.add(interceptor);
	}
}
