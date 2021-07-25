package me.coley.recaf.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;

/**
 * Utility class that holds {@link Lookup}
 * and {@link Unsafe} instances.
 *
 * @author xDark
 */
public final class UnsafeUtil {

    private static final Unsafe UNSAFE;
    private static final Lookup LOOKUP;

    /**
     * Deny public constructions.
     */
    private UnsafeUtil() {
    }

    /**
     * @return {@link Unsafe} instance.
     */
    public static Unsafe unsafe() {
        return UNSAFE;
    }

    /**
     * @return {@link Lookup} instance.
     */
    public static Lookup lookup() {
        return LOOKUP;
    }

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            UNSAFE = unsafe;
            field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
            unsafe.ensureClassInitialized(Lookup.class);
            LOOKUP = (Lookup) unsafe.getObject(unsafe.staticFieldBase(field),
                    unsafe.staticFieldOffset(field));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
