package me.coley.recaf.compiler;

import org.objectweb.asm.Opcodes;

/**
 * Wrapper for <i>-target</i> option.
 *
 * @author Matt
 */
public enum TargetVersion {
	V4("1.4"), V5("1.5"), V6("1.6"), V7("1.7"), V8("1.8"), V9("9"), V10("10"), V11("11"), V12("12"), V13("13");

	/**
	 * Value to pass as a compiler argument.
	 */
	private final String opt;

	TargetVersion(String opt) {
		this.opt = opt;
	}

	/**
	 * @param version
	 * 		Major version constant.
	 *
	 * @return version from class file major version.
	 */
	public static TargetVersion fromClassMajor(int version) {
		switch(version) {
			case Opcodes.V1_4:
				return V4;
			case Opcodes.V1_5:
				return V5;
			case Opcodes.V1_6:
				return V6;
			case Opcodes.V1_7:
				return V7;
			case Opcodes.V1_8:
				return V8;
			case Opcodes.V9:
				return V9;
			case Opcodes.V10:
				return V10;
			case Opcodes.V11:
				return V11;
			case Opcodes.V12:
				return V12;
			case Opcodes.V13:
				return V13;
			default:
				return V8;
		}
	}

	@Override
	public String toString() {
		return opt;
	}
}