package me.xdark.recaf.jvm;

public final class Field extends Member {

	protected Field(VirtualMachine vm, String name, String descriptor, Class declaringClass, int modifiers, boolean synthetic) {
		super(vm, name, descriptor, declaringClass, modifiers, synthetic);
	}
}
