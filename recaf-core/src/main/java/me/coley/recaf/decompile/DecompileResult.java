package me.coley.recaf.decompile;

import me.coley.recaf.workspace.resource.ClassInfo;

/**
 * Decompile result wrapper.
 *
 * @author Matt Coley
 */
public class DecompileResult {
	private final Decompiler decompiler;
	private final ClassInfo classInfo;
	private final String decompiledText;
	private final Exception exception;

	/**
	 * @param decompiler
	 * 		Decompiler responsible for the result.
	 * @param classInfo
	 * 		Original class information of the decompile result.
	 * @param decompiledText
	 * 		Decompiled code of the class.
	 */
	public DecompileResult(Decompiler decompiler, ClassInfo classInfo, String decompiledText) {
		this(decompiler, classInfo, decompiledText, null);
	}

	/**
	 * @param decompiler
	 * 		Decompiler responsible for the result.
	 * @param classInfo
	 * 		Original class information of the decompile result.
	 * @param exception
	 * 		Exception thrown when attempting to decompile.
	 */
	public DecompileResult(Decompiler decompiler, ClassInfo classInfo, Exception exception) {
		this(decompiler, classInfo, null, exception);
	}

	private DecompileResult(Decompiler decompiler, ClassInfo classInfo, String decompiledText, Exception exception) {
		this.decompiler = decompiler;
		this.classInfo = classInfo;
		this.decompiledText = decompiledText;
		this.exception = exception;
	}

	/**
	 * Indicates if the result contains either a {@link #getDecompiledText() text output}
	 * or {@link #getException() error}.
	 *
	 * @return {@code true} if {@link #getDecompiledText()}
	 */
	public boolean wasSuccess() {
		return exception != null;
	}

	/**
	 * @return Decompiler responsible for the result.
	 */
	public Decompiler getDecompiler() {
		return decompiler;
	}

	/**
	 * @return Original class information of the decompile result.
	 */
	public ClassInfo getClassInfo() {
		return classInfo;
	}

	/**
	 * @return Decompiled code. May be {@code null} if an {@link #getException() error} occurred when decompiling.
	 */
	public String getDecompiledText() {
		return decompiledText;
	}

	/**
	 * @return Error thrown when attempting to decompile. May be {@code null} if decompile was a success.
	 */
	public Exception getException() {
		return exception;
	}
}
