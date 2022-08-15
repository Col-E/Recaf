package me.coley.recaf.ui.control.parameterinput.component.control.nodeswitch;

import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.NodeSwitch;
import me.coley.recaf.ui.control.parameterinput.component.Component;
import me.coley.recaf.ui.control.parameterinput.util.ToComponent;
import me.coley.recaf.ui.control.parameterinput.util.ToNode;

import java.util.HashMap;
import java.util.Map;

public class NodeSwitchComponentImpl<Choice>
	implements NodeSwitchComponent<Choice, NodeSwitchComponentImpl<Choice>> {

	private final Choice[] choices;
	private ObservableValue<Choice> observable;
	private Node defaultNode;

	private final Map<Choice, Node> choiceNodes = new HashMap<>();

	NodeSwitchComponentImpl(Choice[] choices, ObservableValue<Choice> observable) {
		this.choices = choices;
		this.observable = observable;
	}

	@Override
	public NodeSwitch<Choice> node() {
		return new NodeSwitch<>(choiceNodes, defaultNode, observable);
	}

	@Override
	public <OutNode extends Node, Ct extends Component<OutNode, Ct>> NodeSwitchComponentImpl<Choice>
	forCase(ToComponent<Choice, OutNode, Ct> toComponent) {
		for (Choice choice : choices) {
			if (choiceNodes.containsKey(choice)) continue;
			choiceNodes.put(choice, toComponent.apply(choice).node());
		}
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> forCase(Choice choice, Node node) {
		choiceNodes.put(choice, node);
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> orElse(ToNode<Choice> node) {
		for (Choice choice : choices) {
			if (choiceNodes.containsKey(choice)) continue;
			choiceNodes.put(choice, node.apply(choice));
		}
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> orElse(ToComponent<Choice, ?, ?> node) {
		for (Choice choice : choices) {
			if (choiceNodes.containsKey(choice)) continue;
			choiceNodes.put(choice, node.apply(choice).node());
		}
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> orElse(Node node) {
		defaultNode = node;
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> nothingFor(Choice choice) {
		choiceNodes.put(choice, null);
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> nothingFor(Choice choice, Choice... otherChoices) {
		choiceNodes.put(choice, null);
		for (Choice otherChoice : otherChoices) {
			choiceNodes.put(otherChoice, null);
		}
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> orElse(Choice choice) {
		choiceNodes.remove(choice);
		return this;
	}

	@Override
	public NodeSwitchComponentImpl<Choice> orElse(Choice choice, Choice... otherChoices) {
		choiceNodes.remove(choice);
		for (Choice otherChoice : otherChoices) {
			choiceNodes.remove(otherChoice);
		}
		return null;
	}
}
