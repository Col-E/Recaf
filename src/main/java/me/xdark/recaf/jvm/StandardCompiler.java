package me.xdark.recaf.jvm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class StandardCompiler implements Compiler {

	@Override
	public Class compileClass(VirtualMachine vm, Class parent, ClassNode node) {
		return new Class(vm, parent, node);
	}

	@Override
	public Field compileField(VirtualMachine vm, Class declaringClass, FieldNode node) {
		int access = node.access;
		return new Field(vm, node.name, node.desc, declaringClass, access, isSynthetic(access));
	}

	@Override
	public Method compileMethod(VirtualMachine vm, Class declaringClass, MethodNode node) {
		int access = node.access;
		return new Method(vm, node.name, node.desc, declaringClass, access, isSynthetic(access), node.instructions, maxStack, maxLocals, tryCatchBlockNodes);
	}

	private static boolean isSynthetic(int access) {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}
}
