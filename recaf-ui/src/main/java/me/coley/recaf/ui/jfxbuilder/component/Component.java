package me.coley.recaf.ui.jfxbuilder.component;

import javafx.scene.Node;

import java.util.function.Consumer;

public interface Component<OutNode extends Node, Self extends Component<OutNode, Self>> {

	OutNode node();

	default OutNode node(Consumer<OutNode> config) {
		OutNode node = node();
		config.accept(node);
		return node;
	}

	default OutNode nodeWithData(Object data) {
		OutNode node = node();
		node.setUserData(data);
		return node;
	}

	default OutNode nodeWithData(Object data, Consumer<OutNode> config) {
		OutNode node = nodeWithData(data);
		config.accept(node);
		return node;
	}

	default Self configureSelf(Consumer<Self> config) {
		// "why unchecked?"
		// because someone might set Self to a completely different type
		config.accept((Self) this);
		return (Self) this;
	}
}

