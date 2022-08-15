package me.coley.recaf.ui.control.parameterinput.component.container.stage;

import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.Component;

import javax.annotation.Nullable;
import java.util.Arrays;


public interface AddStage<
		OutNode extends Node,
		Self extends AddStage<OutNode, Self>
	> {

	Self add(Node node);

	Self add(Node... nodes);

	default Self add(@Nullable Component<?, ?> component) {
		return add(component != null ? component.node() : null);
	}

	default Self add(ComponentSupplier<?, ?> component) {
		return add(component.get());
	}

	default Self add(Component<?, ?>... components) {
		return add(Arrays.stream(components).map((component) -> component != null ? component.node() : null).toArray(Node[]::new));
	}

	default Self add(ComponentsSupplier<?, ?> components) {
		return add(components.get());
	}

	default Self add(NodeSupplier node) {
		return add(node.get());
	}

	default Self add(NodesSupplier nodes) {
		return add(nodes.get());
	}

	<Next extends BuildStage<OutNode, Next>>
	Next build();
}
