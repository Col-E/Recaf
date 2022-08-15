package me.coley.recaf.ui.control.parameterinput;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Parent;

import java.util.Map;

public class NodeSwitch<Choice> extends Parent {
	private final ObjectProperty<Choice> choiceProperty = new SimpleObjectProperty<>();

	private final Node defaultNode;

	public NodeSwitch(Map<Choice, Node> choices, Node defaultNode, ObservableValue<Choice> observable) {
		this.defaultNode = defaultNode;
		choiceProperty.bind(observable);
		getChildren().setAll(choices.getOrDefault(observable.getValue(), defaultNode));
		choiceProperty.addListener((obs, oldV, newV) -> {
			Node node = choices.get(newV);
			if (node != null) getChildren().setAll(node);
			else getChildren().clear();
		});
	}

	public Node getDefaultNode() {
		return defaultNode;
	}

	public ObjectProperty<Choice> choiceProperty() {
		return choiceProperty;
	}
}
