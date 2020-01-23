package me.xdark.recaf.jvm.transformers;

import me.xdark.recaf.jvm.Compiler;
import me.xdark.recaf.jvm.UnresolvedClass;
import me.xdark.recaf.jvm.VMException;
import me.xdark.recaf.jvm.VirtualMachine;
import me.xdark.recaf.jvm.classloading.ClassLoader;
import me.xdark.recaf.jvm.instrumentation.ClassFileTransformer;
import org.objectweb.asm.tree.ClassNode;

public final class ObjectClassTransformer implements ClassFileTransformer {
	@Override
	public boolean instrument(VirtualMachine vm, Compiler compiler, ClassLoader classLoader, UnresolvedClass node) throws VMException {
		if ("java/lang/Object".equals(node.getClassName())) {
			ClassNode resolved = node.resolve();
			return true;
		}
		return false;
	}
}
