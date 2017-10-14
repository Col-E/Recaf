package me.coley.recaf.ui.component.tree;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

/**
 * Tree node containing ClassNode and FieldNode data.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class ASMFieldTreeNode extends ASMTreeNode {
	private final FieldNode field;

	public ASMFieldTreeNode(String title, ClassNode node, FieldNode field) {
		super(title, node);
		this.field = field;
	}

	public FieldNode getField() {
		return field;
	}
}
