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
public final class LookupUtil {

    private static final Lookup LOOKUP;

    /**
     * Deny public constructions.
     */
    private LookupUtil() {
    }

    /**
     * @return {@link Lookup} instance.
     */
    public static Lookup lookup() {
        return LOOKUP;
    }

    static {
        try {
            Field field = ReflectUtil.getDeclaredField(Lookup.class, "IMPL_LOOKUP");
            LOOKUP = (Lookup) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
