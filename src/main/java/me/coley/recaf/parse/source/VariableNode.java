package me.coley.recaf.parse.source;

import org.objectweb.asm.tree.LocalVariableNode;

public class VariableNode {
	private LocalVariableNode lvn;

	public VariableNode(LocalVariableNode lvn) {
		this.lvn = lvn;
	}

	public String getType() {
		return lvn.desc;
	}
}
