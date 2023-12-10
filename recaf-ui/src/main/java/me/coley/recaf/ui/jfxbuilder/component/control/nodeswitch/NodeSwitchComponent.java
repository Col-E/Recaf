package me.coley.recaf.ui.jfxbuilder.component.control.nodeswitch;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.NodeSwitch;
import me.coley.recaf.ui.jfxbuilder.component.Component;
import me.coley.recaf.ui.jfxbuilder.util.function.ToComponent;
import me.coley.recaf.ui.jfxbuilder.util.function.ToNode;

public interface NodeSwitchComponent<Choice, Self extends NodeSwitchComponent<Choice, Self>>
	extends Component<NodeSwitch<Choice>, Self> {

	default Self forCase(Choice choice, Component<?, ?> component) {
		return forCase(choice, component.node());
	}

	<OutNode extends Node, Ct extends Component<OutNode, Ct>>
	Self forCase(ToComponent<Choice, OutNode, Ct> toComponent);

	Self forCase(Choice choice, Node node);

	Self orElse(ToNode<Choice> node);

	Self orElse(ToComponent<Choice, ?, ?> node);

	default Self orElse(Component<?, ?> component) {
		return orElse(component.node());
	}

	Self orElse(Node node);

	Self nothingFor(Choice choice);

	Self nothingFor(Choice choice, Choice... otherChoices);

	Self orElse(Choice choice);

	Self orElse(Choice choice, Choice... otherChoices);

	static <Choice, R extends NodeSwitchComponent<Choice, R>> R nodeSwitch(Choice[] choices, ObservableValue<Choice> observable) {
		return (R) new NodeSwitchComponentImpl<>(choices, observable);
	}

}
