package me.xdark.recaf.jvm;

import org.objectweb.asm.tree.ClassNode;

final class WrapperClass extends Class {

	protected WrapperClass(VirtualMachine vm, Class superclass, ClassNode handle) {
		super(vm, superclass, handle);
	}
}
