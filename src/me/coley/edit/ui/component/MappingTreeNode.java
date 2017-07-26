package me.coley.edit.ui.component;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.objectweb.asm.tree.ClassNode;

@SuppressWarnings("serial")
public class MappingTreeNode extends DefaultMutableTreeNode {
	private final Map<String, MappingTreeNode> children = new HashMap<>();
	private final ClassNode node;

	public MappingTreeNode(String title, ClassNode node) {
		super(title);
		this.node = node;
	}
	
	public MappingTreeNode getChild(String name) {
		return children.get(name);
	}

	public void addChild(String name, MappingTreeNode node) {
		children.put(name, node);
	}

	public final ClassNode getNode() {
		return node;
	}
}
