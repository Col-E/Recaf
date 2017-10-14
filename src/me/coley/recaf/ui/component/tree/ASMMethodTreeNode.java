package me.coley.recaf.ui.component.tree;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Tree node containing ClassNode and MethodNode data.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class ASMMethodTreeNode extends ASMTreeNode {
	private final MethodNode method;

	public ASMMethodTreeNode(String title, ClassNode node, MethodNode method) {
		super(title, node);
		this.method = method;
	}

	public MethodNode getMethod() {
		return method;
	}
}
