package me.xdark.recaf.jvm;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public interface Compiler {
	Class compileClass(VirtualMachine vm, Class parent, ClassNode node);

	Field compileField(VirtualMachine vm, Class declaringClass, FieldNode node);

	Method compileMethod(VirtualMachine vm, Class declaringClass, MethodNode node);
}
