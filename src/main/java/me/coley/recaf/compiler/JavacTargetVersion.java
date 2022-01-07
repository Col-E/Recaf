package me.coley.recaf.compiler;

import me.coley.recaf.util.ClassUtil;
import me.coley.recaf.util.Log;
import me.coley.recaf.util.VMUtil;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Field;

/**
 * Wrapper for <i>-target</i> option.
 *
 * @author Matt
 */
public enum JavacTargetVersion {
	V4("1.4"),
	V5("1.5"),
	V6("1.6"),
	V7("1.7"),
	V8("1.8"),
	V9("9"),
	V10("10"),
	V11("11"),
	V12("12"),
	V13("13"),
	V14("14"),
	V15("15"),
	V16("16"),
	V17("17"),
	V18("18"),
	;

	/**
	 * Value to pass as a compiler argument.
	 */
	private final String opt;

	JavacTargetVersion(String opt) {
		this.opt = opt;
	}

	/**
	 * @return VM version.
	 */
	public int version() {
		return 4 + ordinal();
	}

	/**
	 * @param version
	 * 		Major version constant.
	 *
	 * @return version from class file major version.
	 */
	public static JavacTargetVersion fromClassMajor(int version) {
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
			case Opcodes.V14:
				return V14;
			case Opcodes.V15:
				return V15;
			case Opcodes.V16:
				return V16;
			case Opcodes.V17:
				return V17;
			case Opcodes.V18:
				return V18;
			default:
				return V8;
		}
	}

	/**
	 * @return Minimum version supported by Javac.
	 */
	public static JavacTargetVersion getMinJavacSupport() {
		try {
			Class<?> cls = Class.forName("com.sun.tools.javac.jvm.Target");
			Field min = cls.getDeclaredField("MIN");
			min.setAccessible(true);
			Object minTargetInstance = min.get(null);
			Field version = cls.getDeclaredField("majorVersion");
			version.setAccessible(true);
			return fromClassMajor(version.getInt(minTargetInstance));
		} catch (Exception ex) {
			Log.error(ex, "Failed to find javac minimum supported version, defaulting to Java 7");
		}
		return V7;
	}

	/**
	 * @return Maximum version supported by Javac.
	 */
	public static JavacTargetVersion getMaxJavacSupport() {
		try {
			return fromClassMajor(VMUtil.getVmVersion() + ClassUtil.VERSION_OFFSET);
		} catch (Exception ex) {
			Log.error("Failed to find javac maximum supported version, defaulting to Java 8 (52)");
		}
		return V8;
	}

	@Override
	public String toString() {
		return opt;
	}
}