package me.coley.recaf.ui.control.parameterinput.component.container.stage.creation;

import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.AddStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.ConfigureCreationStage;

public interface AddOrConfigureStage<NodeOut extends Node, Next extends AddStage<NodeOut, Next>>
	extends AddStage<NodeOut, Next>, ConfigureCreationStage<NodeOut, Next> {}
