package me.xdark.recaf.jvm;

import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

public final class VirtualMachine {
	private final Workspace workspace;
	private final Compiler compiler;

	public VirtualMachine(Workspace workspace, Compiler compiler) throws VMException {
		this.workspace = workspace;
		this.compiler = compiler;
	}

	public Compiler getCompiler() {
		return compiler;
	}

	public ClassNode requestClassForCompilation(String className) throws VMException {
		byte[] raw = workspace.getRawClass(className);
		if (raw == null) {
			throw new VMException(className);
		}
		ClassNode node = new ClassNode();
		new ClassReader(raw).accept(node, 0);
		return node;
	}
}
