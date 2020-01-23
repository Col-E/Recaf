package me.xdark.recaf.jvm;

import me.xdark.recaf.jvm.instrumentation.ClassFileTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public final class StandardCompiler implements Compiler {
	private final List<ClassFileTransformer> transformers = new ArrayList<>(4);
	private final VirtualMachine vm;

	public StandardCompiler(VirtualMachine vm) {
		this.vm = vm;
	}

	@Override
	public Class compileClass(Class parent, UnresolvedClass unresolvedClass) {
		return new Class(this, parent, unresolvedClass);
	}

	@Override
	public Field compileField(Class declaringClass, FieldNode node) {
		int access = node.access;
		return new Field(node.name, node.desc, declaringClass, access, isSynthetic(access), -1); // TODO
	}

	@Override
	public Method compileMethod(Class declaringClass, MethodNode node) {
		int access = node.access;
		return new Method(vm, node.name, node.desc, declaringClass, access, isSynthetic(access), node.instructions, node.maxStack, node.maxLocals, node.tryCatchBlocks);
	}

	@Override
	public void registerTransformer(ClassFileTransformer transformer) {
		transformers.add(transformer);
	}

	private static boolean isSynthetic(int access) {
		return (access & Opcodes.ACC_SYNTHETIC) != 0;
	}
}
