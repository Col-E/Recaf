package me.coley.recaf.ui.jfxbuilder.component.control;

import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public class NodeComponent<OutNode extends Node> implements Component<OutNode, NodeComponent<OutNode>> {

	private final OutNode node;

	NodeComponent(OutNode node) {
		this.node = node;
	}

	@Override
	public OutNode node() {
		return node;
	}

	public static <OutNode extends Node> NodeComponent<OutNode> node(OutNode node) {
		return new NodeComponent<>(node);
	}
}
