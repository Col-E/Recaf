package me.coley.recaf.ui.jfxbuilder;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public class NodeToggle extends Pane implements Component<NodeToggle, NodeToggle> {

	private final Node node;

	private final BooleanProperty visibilityProperty = new SimpleBooleanProperty();

	private final boolean reverse;

	private NodeToggle(Node node, ObservableBooleanValue observable, boolean reverse) {
		this.node = node;
		//		toggleVisibility(node, reverse, observable.get());
		this.reverse = reverse;
		visibilityProperty.addListener((obs, oldV, newV) -> {
			toggleVisibility(node, reverse, newV);
		});
		visibilityProperty.bind(observable);
	}

	private void toggleVisibility(Node node, boolean reverse, Boolean newV) {
		if (reverse) {
			if (!newV) getChildren().setAll(node);
			else getChildren().clear();
		} else {
			if (newV) getChildren().setAll(node);
			else getChildren().clear();
		}
	}

	public static NodeToggle nodeToggle(ObservableBooleanValue observable, boolean reverse, Node node) {
		return new NodeToggle(node, observable, reverse);
	}

	public static NodeToggle nodeToggle(ObservableBooleanValue observable, Node node) {
		return new NodeToggle(node, observable, false);
	}

	public static NodeToggle nodeToggleReversed(ObservableBooleanValue observable, Node node) {
		return new NodeToggle(node, observable, true);
	}

	public static NodeToggle nodeToggle(ObservableBooleanValue observable, boolean reverse, Component<?, ?> component) {
		return nodeToggle(observable, reverse, component.node());
	}

	public static NodeToggle nodeToggle(ObservableBooleanValue observable, Component<?, ?> component) {
		return nodeToggle(observable, false, component);
	}

	public static NodeToggle nodeToggleReversed(ObservableBooleanValue observable, Component<?, ?> component) {
		return nodeToggle(observable, true, component);
	}

	public Node getNode() {
		return node;
	}

	public BooleanProperty visibilityProperty() {
		return visibilityProperty;
	}

	@Override
	public NodeToggle node() {
		return this;
	}
}
