package me.xdark.recaf.jvm.inject;

import me.xdark.recaf.jvm.Compiler;
import me.xdark.recaf.jvm.UnresolvedClass;
import me.xdark.recaf.jvm.VMException;
import me.xdark.recaf.jvm.VirtualMachine;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import me.xdark.recaf.jvm.instrumentation.ClassFileTransformer;

public final class InjectionTransformer implements ClassFileTransformer {
	@Override
	public boolean instrument(VirtualMachine vm, Compiler compiler, ClassLoader classLoader, UnresolvedClass node) throws VMException {
		return false;
	}
}
