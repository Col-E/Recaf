package me.xdark.recaf.runtime;

import me.xdark.recaf.runtime.util.VMUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class Java9ClassDefiner implements ClassDefiner {

    private static final MethodHandle MH_DEFINE_CLASS;
    private static final Object JDK_UNSAFE;
    private final Unsafe unsafe;

    Java9ClassDefiner(Unsafe unsafe) {
        this.unsafe = unsafe;
    }

    @Override
    public Class<?> defineClass(Class<?> host, byte[] bytes, Object[] cpPatches) {
        try {
            return (Class<?>) MH_DEFINE_CLASS.invoke(JDK_UNSAFE, host, bytes, cpPatches);
        } catch (Throwable t) {
            // We re-throw exception, but compiler does not know about it
            unsafe.throwException(t);
            return null;
        }
    }

    static {
        try {
            MethodHandles.Lookup lookup = VMUtil.lookup();
            Class<?> unsafeClass = Class.forName("jdk.internal.misc.Unsafe", true, null);
            MH_DEFINE_CLASS = lookup.findVirtual(unsafeClass, "defineAnonymousClass",
                    MethodType.methodType(Class.class, Class.class, byte[].class, Object[].class));
            JDK_UNSAFE = lookup.findStatic(unsafeClass, "getUnsafe", MethodType.methodType(unsafeClass)).invoke();
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }
}
