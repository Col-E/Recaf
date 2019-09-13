package me.xdark.recaf.runtime;

public interface ClassDefiner {

    /**
     * Defines a class that does not belong to any {@link ClassLoader} instance
     *
     * @param host      defined class's host
     * @param bytes     bytecode of the class
     * @param cpPatches patches that will be applied to the constant pool
     * @return defined class
     */
    Class<?> defineClass(Class<?> host, byte[] bytes, Object[] cpPatches);
}
