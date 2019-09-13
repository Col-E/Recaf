package me.xdark.recaf.runtime.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

/**
 * Utility class to provide access to trusted lookup and Unsafe
 *
 * @author xDark
 */
public final class VMUtil {

    private static final Unsafe UNSAFE;
    private static final MethodHandles.Lookup LOOKUP;
    private static final int VM_VERSION;

    private VMUtil() {
    }

    /**
     * @return {@link Unsafe}
     */
    public static Unsafe unsafe() {
        return UNSAFE;
    }

    /**
     * @return {@link MethodHandles.Lookup}
     */
    public static MethodHandles.Lookup lookup() {
        return LOOKUP;
    }

    /**
     * @return version of JVM
     */
    public static int vmVersion() {
        return VM_VERSION;
    }

    static {
        try {
            VM_VERSION = (int) (Float.parseFloat(System.getProperty("java.class.version")) - 44);
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = UNSAFE = (Unsafe) field.get(null);
            field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.publicLookup();
            LOOKUP = (MethodHandles.Lookup) unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
}
