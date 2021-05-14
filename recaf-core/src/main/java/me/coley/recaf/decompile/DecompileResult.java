package me.coley.recaf.decompile;

import me.coley.recaf.plugin.tools.ToolResult;
import me.coley.recaf.code.ClassInfo;

/**
 * Decompile result wrapper.
 *
 * @author Matt Coley
 */
public class DecompileResult extends ToolResult<Decompiler, String> {
	private final ClassInfo classInfo;

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
		super(decompiler, decompiledText, exception);
		this.classInfo = classInfo;
	}

	/**
	 * @return Original class information of the decompile result.
	 */
	public ClassInfo getClassInfo() {
		return classInfo;
	}
}
