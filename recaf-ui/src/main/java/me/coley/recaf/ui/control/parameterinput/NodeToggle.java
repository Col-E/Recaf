package me.coley.recaf.ui.control.parameterinput;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.Parent;
import me.coley.recaf.ui.control.parameterinput.component.Component;

public class NodeToggle extends Parent implements Component<NodeToggle, NodeToggle> {

	private final Node node;

	private final BooleanProperty visibilityProperty = new SimpleBooleanProperty();

	private final boolean reverse;

	private NodeToggle(Node node, ObservableBooleanValue observable, boolean reverse) {
		this.node = node;
		if (reverse) {
			if (!observable.get()) getChildren().setAll(node);
			else getChildren().clear();
		} else {
			if (observable.get()) getChildren().setAll(node);
			else getChildren().clear();
		}
		this.reverse = reverse;
		visibilityProperty.addListener((obs, oldV, newV) -> {
			if (reverse) {
				if (!newV) getChildren().setAll(node);
				else getChildren().clear();
			} else {
				if (newV) getChildren().setAll(node);
				else getChildren().clear();
			}
		});
		visibilityProperty.bind(observable);
	}

	public static NodeToggle nodeToggle(Node node, ObservableBooleanValue observable, boolean reverse) {
		return new NodeToggle(node, observable, reverse);
	}

	public static NodeToggle nodeToggle(Node node, ObservableBooleanValue observable) {
		return new NodeToggle(node, observable, false);
	}

	public static NodeToggle nodeToggleReversed(Node node, ObservableBooleanValue observable) {
		return new NodeToggle(node, observable, true);
	}

	public static NodeToggle nodeToggle(Component<?, ?> component, ObservableBooleanValue observable, boolean reverse) {
		return nodeToggle(component.node(), observable, reverse);
	}

	public static NodeToggle nodeToggle(Component<?, ?> component, ObservableBooleanValue observable) {
		return nodeToggle(component, observable, false);
	}

	public static NodeToggle nodeToggleReversed(Component<?, ?> component, ObservableBooleanValue observable) {
		return nodeToggle(component, observable, true);
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
