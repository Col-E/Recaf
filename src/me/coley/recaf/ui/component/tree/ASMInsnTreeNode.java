package me.coley.recaf.ui.component.tree;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Tree node containing ClassNode, MethodNode, and AbstractInsnNode data.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class ASMInsnTreeNode extends ASMMethodTreeNode {
	private final AbstractInsnNode ain;

	public ASMInsnTreeNode(String title, ClassNode node, MethodNode method, AbstractInsnNode ain) {
		super(title, node, method);
		this.ain = ain;
	}

	public AbstractInsnNode getInsn() {
		return ain;
	}
}
