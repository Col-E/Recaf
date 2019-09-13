package me.xdark.recaf.runtime;

import sun.misc.Unsafe;

final class Java8ClassDefiner implements ClassDefiner {

    private final Unsafe unsafe;

    Java8ClassDefiner(Unsafe unsafe) {
        this.unsafe = unsafe;
    }

    @Override
    public Class<?> defineClass(Class<?> host, byte[] bytes, Object[] cpPatches) {
        return unsafe.defineAnonymousClass(host, bytes, cpPatches);
    }
}
