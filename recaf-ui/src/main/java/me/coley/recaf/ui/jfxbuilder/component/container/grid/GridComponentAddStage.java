package me.coley.recaf.ui.jfxbuilder.component.container.grid;

import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.component.Component;
import me.coley.recaf.ui.jfxbuilder.component.container.stage.AddStage;
import me.coley.recaf.ui.jfxbuilder.util.supplier.ComponentSupplier;
import me.coley.recaf.ui.jfxbuilder.util.supplier.ComponentsSupplier;
import me.coley.recaf.ui.jfxbuilder.util.supplier.NodeSupplier;
import me.coley.recaf.ui.jfxbuilder.util.supplier.NodesSupplier;

import javax.annotation.Nullable;
import java.util.Arrays;

public interface GridComponentAddStage<NodeOut extends Node, Self extends GridComponentAddStage<NodeOut, Self>>
	extends AddStage<NodeOut, Self> {

	Self addIf(ObservableBooleanValue condition, Node node);

	Self addIf(ObservableBooleanValue condition, Node... nodes);

	default Self addIf(ObservableBooleanValue condition, @Nullable Component<?, ?> component) {
		return addIf(condition, component != null ? component.node() : null);
	}

	default Self addIf(ObservableBooleanValue condition, ComponentSupplier<?, ?> component) {
		return addIf(condition, component.get());
	}

	default Self addIf(ObservableBooleanValue condition, Component<?, ?>... components) {
		return addIf(condition, Arrays.stream(components).map((component) -> component != null ? component.node() : null).toArray(Node[]::new));
	}

	default Self addIf(ObservableBooleanValue condition, ComponentsSupplier<?, ?> components) {
		return addIf(condition, components.get());
	}

	default Self addIf(ObservableBooleanValue condition, NodeSupplier node) {
		return addIf(condition, node.get());
	}

	default Self addIf(ObservableBooleanValue condition, NodesSupplier nodes) {
		return addIf(condition, nodes.get());
	}
}
