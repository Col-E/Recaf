package me.coley.recaf.util;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Package-private util to deal with modules.
 *
 * @author xxDark
 */
final class Java9Util {

    private static final MethodHandle CLASS_MODULE;
    private static final MethodHandle CLASS_LOADER_MODULE;
    private static final MethodHandle METHOD_MODIFIERS;

    /**
     * Deny all constructions.
     */
    private Java9Util() {
    }

    /**
     * @param klass {@link Class} to get module from.
     * @return {@link Module} of the class.
     */
    static Module getClassModule(Class<?> klass) {
        try {
            return (Module) CLASS_MODULE.invokeExact(klass);
        } catch (Throwable t) {
            // That should never happen.
            throw new AssertionError(t);
        }
    }

    /**
     * @param loader {@link ClassLoader} to get module from.
     * @return {@link Module} of the class.
     */
    static Module getLoaderModule(ClassLoader loader) {
        try {
            return (Module) CLASS_LOADER_MODULE.invokeExact(loader);
        } catch (Throwable t) {
            // That should never happen.
            throw new AssertionError(t);
        }
    }


    /**
     * @param method {@link Method} to change modifiers for.
     * @param modifiers new modifiers.
     */
    static void setMethodModifiers(Method method, int modifiers) {
        try {
            METHOD_MODIFIERS.invokeExact(method, modifiers);
        } catch (Throwable t) {
            // That should never happen.
            throw new AssertionError(t);
        }
    }


    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Unsafe unsafe = (Unsafe) field.get(null);
            field = Lookup.class.getDeclaredField("IMPL_LOOKUP");
            MethodHandles.publicLookup();
            Lookup lookup = (Lookup)
                    unsafe.getObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field));
            MethodType type = MethodType.methodType(Module.class);
            CLASS_MODULE = lookup.findVirtual(Class.class, "getModule", type);
            CLASS_LOADER_MODULE = lookup.findVirtual(ClassLoader.class, "getUnnamedModule", type);
            METHOD_MODIFIERS = lookup.findSetter(Method.class, "modifiers", Integer.TYPE);
        } catch (NoSuchMethodException | IllegalAccessException | NoSuchFieldException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
