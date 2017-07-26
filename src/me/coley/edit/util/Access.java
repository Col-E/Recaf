package me.coley.edit.util;

/**
 * A utility for checking and generating object access.
 * 
 * @author Matt
 */
public class Access {
    // @formatter:off
    // Constants
    public static final int ABSTRACT     =  0x0400;
    public static final int ANNOTATION   =  0x2000;
    public static final int BDIDGE       =  0x0040;
    public static final int ENUM         =  0x4000;
    public static final int FINAL        =  0x0010;
    public static final int INTERFACE    =  0x0200;
    public static final int NATIVE       =  0x0100;
    public static final int PRIVATE      =  0x0002;
    public static final int PROTECTED    =  0x0004;
    public static final int PUBLIC       =  0x0001;
    public static final int STATIC       =  0x0008;
    public static final int STRICT       =  0x0800;
    public static final int SUPER        =  0x0020;
    public static final int SYNCHRONIZED =  0x0020;
    public static final int SYNTHETIC    =  0x1000;
    public static final int TRANSIENT    =  0x0080;
    public static final int VARARGS      =  0x0080;
    public static final int VOLATILE     =  0x0040;
    // Access checking
    public static boolean isAbstract(int acc){return(acc & ABSTRACT)!=0;}
    public static boolean isAnnotation(int acc){return(acc & ANNOTATION)!=0;}
    public static boolean isBridge(int acc){return(acc & BDIDGE)!=0;}
    public static boolean isEnum(int acc){return(acc & ENUM)!=0;}
    public static boolean isFinal(int acc){return(acc & FINAL)!=0;}
    public static boolean isInterface(int acc){return(acc & INTERFACE)!=0;}
    public static boolean isNative(int acc){return(acc & NATIVE)!=0;}
    public static boolean isPrivate(int acc){return(acc & PRIVATE)!=0;}
    public static boolean isProtected(int acc){return(acc & PROTECTED)!=0;}
    public static boolean isPublic(int acc){return(acc & PUBLIC)!=0;}
    public static boolean isStatic(int acc){return(acc & STATIC)!=0;}
    public static boolean isStrict(int acc){return(acc & STRICT)!=0;}
    public static boolean isSuper(int acc){return(acc & SUPER)!=0;}
    public static boolean isSynchronized(int acc){return(acc & SYNCHRONIZED)!=0;}
    public static boolean isSynthetic(int acc){return(acc & SYNTHETIC)!=0;}
    public static boolean isTransient(int acc){return(acc & TRANSIENT)!=0;}
    public static boolean isVarargs(int acc){return(acc & VARARGS)!=0;}
    public static boolean isVolatile(int acc){return(acc & VOLATILE)!=0;}
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
