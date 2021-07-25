package me.coley.recaf.util;

import java.lang.reflect.Field;

/**
 * Reflection utilities.
 *
 * @author xDark
 */
public final class ReflectUtil {

    /**
     * Deny all constructions.
     */
    private ReflectUtil() {
    }

    /**
     * @param declaringClass class that declares a field.
     * @param name           the name of the field.
     * @return the {@link Field} object for the specified field in the
     * class.
     * @throws NoSuchFieldException if a field with the specified name is not found.
     */
    public static Field getDeclaredField(Class<?> declaringClass, String name) throws NoSuchFieldException {
        Field field = declaringClass.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
