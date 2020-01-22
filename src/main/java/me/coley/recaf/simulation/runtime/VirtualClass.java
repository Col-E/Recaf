package me.coley.recaf.simulation.runtime;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

public final class VirtualClass {
	private final ClassNode node;
	private final MemberTable<MethodNode> dispatchTable;
	private final MemberTable<FieldNode> fieldTable;

	public VirtualClass(ClassNode node) {
		this.node = node;
		this.dispatchTable = new MemberTable<>(node.methods, m -> m.name, m -> m.desc);
		this.fieldTable = new MemberTable<>(node.fields, f -> f.name, f -> f.desc);
	}
}
