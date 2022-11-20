package me.coley.recaf.decompile;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.decompile.fallback.FallbackDecompiler;
import me.coley.recaf.plugin.tools.Tool;
import me.coley.recaf.workspace.Workspace;

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
	private final List<PreDecompileInterceptor> preDecompileInterceptors = new ArrayList<>();
	private final List<PostDecompileInterceptor> postDecompileInterceptors = new ArrayList<>();

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
			text = applyPostInterceptors(text);
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
	 * @param code
	 * 		Input bytecode.
	 *
	 * @return Output bytecode with all {@link #getPreDecompileInterceptors() interceptors} applied.
	 */
	public byte[] applyPreInterceptors(byte[] code) {
		for (PreDecompileInterceptor interceptor : getPreDecompileInterceptors())
			code = interceptor.apply(code);
		return code;
	}

	/**
	 * @param code
	 * 		Input decompiled code.
	 *
	 * @return Output decompiled code with all {@link #getPreDecompileInterceptors() interceptors} applied.
	 */
	public String applyPostInterceptors(String code) {
		for (PostDecompileInterceptor interceptor : getPostDecompileInterceptors())
			code = interceptor.apply(code);
		return code;
	}

	/**
	 * @return List of current interceptors for bytecode filtering.
	 */
	public List<PreDecompileInterceptor> getPreDecompileInterceptors() {
		return Collections.unmodifiableList(preDecompileInterceptors);
	}

	/**
	 * @return List of current interceptors for decompiled code filtering.
	 */
	public List<PostDecompileInterceptor> getPostDecompileInterceptors() {
		return Collections.unmodifiableList(postDecompileInterceptors);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to add.
	 */
	public void addPreDecompileInterceptor(PreDecompileInterceptor interceptor) {
		preDecompileInterceptors.add(interceptor);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to remove.
	 */
	public void removePreDecompileInterceptor(PreDecompileInterceptor interceptor) {
		preDecompileInterceptors.add(interceptor);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to add.
	 */
	public void addPostDecompileInterceptor(PostDecompileInterceptor interceptor) {
		postDecompileInterceptors.add(interceptor);
	}

	/**
	 * @param interceptor
	 * 		Interceptor to remove.
	 */
	public void removePostDecompileInterceptor(PostDecompileInterceptor interceptor) {
		postDecompileInterceptors.add(interceptor);
	}

	@Override
	public int compareTo(Tool o) {
		if (o instanceof FallbackDecompiler)
			return -1;
		return super.compareTo(o);
	}
}
