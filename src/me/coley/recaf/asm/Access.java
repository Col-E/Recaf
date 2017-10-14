package me.coley.recaf.asm;

/**
 * A utility for checking and generating object access.
 *
 * @author Matt
 */
public class Access {
	// @formatter:off
	// Modifiers - Public
	public static final int PUBLIC       = 0x00001;
	public static final int PRIVATE      = 0x00002;
	public static final int PROTECTED    = 0x00004;
	public static final int STATIC       = 0x00008;
	public static final int FINAL        = 0x00010;
	public static final int SYNCHRONIZED = 0x00020;
	public static final int VOLATILE     = 0x00040;
	public static final int TRANSIENT    = 0x00080;
	public static final int NATIVE       = 0x00100;
	public static final int INTERFACE    = 0x00200;
	public static final int ABSTRACT     = 0x00400;
	public static final int STRICT       = 0x00800;
	// Modifiers - Non-Public
	public static final int BRIDGE       = 0x00040;
	public static final int VARARGS      = 0x00080;
	public static final int SYNTHETIC    = 0x01000;
	public static final int ANNOTATION   = 0x02000;
	public static final int ENUM         = 0x04000;
	public static final int MANDATED     = 0x08000;
	public static final int SUPER        = 0x00020;
	// Modifier sets
	public static final int CLASS_MODIFIERS =
		PUBLIC       | PRIVATE    | FINAL  |
		STATIC       | ANNOTATION | ENUM   |
		SYNTHETIC    | INTERFACE  | SUPER  |
		ABSTRACT     | STRICT;
	public static final int INTERFACE_MODIFIERS =
		PUBLIC       | PROTECTED  | PRIVATE |
		ABSTRACT     | STATIC     | STRICT  |
		SYNTHETIC;
	public static final int CONSTRUCTOR_MODIFIERS =
		PUBLIC       | PROTECTED  | PRIVATE |
		BRIDGE       | SYNTHETIC  | VARARGS;
	public static final int METHOD_MODIFIERS =
		PUBLIC       | PROTECTED  | PRIVATE |
		ABSTRACT     | STATIC     | FINAL   |
		SYNCHRONIZED | NATIVE     | STRICT  |
		SYNTHETIC    | BRIDGE     | VARARGS;
	public static final int FIELD_MODIFIERS =
		PUBLIC       | PROTECTED  | PRIVATE |
		STATIC       | TRANSIENT  | FINAL   |
		VOLATILE     | SYNTHETIC;
	// Access checking
	public static boolean isAbstract(int acc) {
		return(acc & ABSTRACT)!=0;
	}
	public static boolean isAnnotation(int acc) {
		return(acc & ANNOTATION)!=0;
	}
	public static boolean isBridge(int acc) {
		return(acc & BRIDGE)!=0;
	}
	public static boolean isEnum(int acc) {
		return(acc & ENUM)!=0;
	}
	public static boolean isFinal(int acc) {
		return(acc & FINAL)!=0;
	}
	public static boolean isInterface(int acc) {
		return(acc & INTERFACE)!=0;
	}
	public static boolean isNative(int acc) {
		return(acc & NATIVE)!=0;
	}
	public static boolean isPrivate(int acc) {
		return(acc & PRIVATE)!=0;
	}
	public static boolean isProtected(int acc) {
		return(acc & PROTECTED)!=0;
	}
	public static boolean isPublic(int acc) {
		return(acc & PUBLIC)!=0;
	}
	public static boolean isStatic(int acc) {
		return(acc & STATIC)!=0;
	}
	public static boolean isStrict(int acc) {
		return(acc & STRICT)!=0;
	}
	public static boolean isSuper(int acc) {
		return(acc & SUPER)!=0;
	}
	public static boolean isSynchronized(int acc) {
		return(acc & SYNCHRONIZED)!=0;
	}
	public static boolean isSynthetic(int acc) {
		return(acc & SYNTHETIC)!=0;
	}
	public static boolean isTransient(int acc) {
		return(acc & TRANSIENT)!=0;
	}
	public static boolean isVarargs(int acc) {
		return(acc & VARARGS)!=0;
	}
	public static boolean isVolatile(int acc) {
		return(acc & VOLATILE)!=0;
	}
	// Access creation
	public static int createAccess(int... acArgs) {
		int access = 0;
		for (int acArg : acArgs) access |= acArg;
		return access;
	}
	public static boolean hasAccess(int access, int... acArgs) {
		for (int acArg : acArgs)
			if ((access & acArg) == 0) return false;
		return true;
	}
}
