package me.xdark.recaf.jvm.instrumentation;

import me.xdark.recaf.jvm.Compiler;
import me.xdark.recaf.jvm.UnresolvedClass;
import me.xdark.recaf.jvm.VMException;
import me.xdark.recaf.jvm.VirtualMachine;
import me.xdark.recaf.jvm.classloading.ClassLoader;

@FunctionalInterface
public interface ClassFileTransformer {
	boolean instrument(VirtualMachine vm, Compiler compiler, ClassLoader classLoader, UnresolvedClass node) throws VMException;
}
