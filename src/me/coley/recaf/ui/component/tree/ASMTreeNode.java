package me.coley.recaf.ui.component.tree;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.objectweb.asm.tree.ClassNode;

/**
 * Tree node containing ClassNode data.
 *
 * @author Matt
 */
@SuppressWarnings("serial")
public class ASMTreeNode extends DefaultMutableTreeNode {
	private final Map<String, ASMTreeNode> children = new HashMap<>();
	private final ClassNode node;

	public ASMTreeNode(String title, ClassNode node) {
		super(title);
		this.node = node;
	}

	public ASMTreeNode getChild(String name) {
		return children.get(name);
	}

	public void addChild(String name, ASMTreeNode node) {
		children.put(name, node);
	}

	public final ClassNode getNode() {
		return node;
	}
}
