package me.xdark.recaf.jvm.classloading;

import me.coley.recaf.workspace.Workspace;
import me.xdark.recaf.jvm.Class;
import me.xdark.recaf.jvm.Compiler;
import me.xdark.recaf.jvm.VirtualMachine;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class WorkspaceClassLoader extends ClassLoader {
	private final Workspace workspace;
	private final VirtualMachine vm;
	private final Compiler compiler;

	public WorkspaceClassLoader(Workspace workspace, VirtualMachine vm, Compiler compiler, ClassLoader parent) {
		super(parent);
		this.workspace = workspace;
		this.vm = vm;
		this.compiler = compiler;
	}

	@Override
	public Class findClass(String name) throws ClassNotFoundException {
		ClassReader reader = workspace.getClassReader(name);
		if (reader == null) {
			throw new ClassNotFoundException(name);
		}
		ClassNode node = new ClassNode();
		reader.accept(node, 0);
		Class parent = loadClass(node.superName);
		return compiler.compileClass(vm, parent, node);
	}
}
