package me.coley.recaf.ui.control.parameterinput.component.container.stage;

import javafx.scene.Node;

import java.util.function.Consumer;

public interface ConfigureCreationStage<OutNode extends Node, Next extends AddStage<OutNode, Next>> {
	Next configure(Consumer<OutNode> config);
}
