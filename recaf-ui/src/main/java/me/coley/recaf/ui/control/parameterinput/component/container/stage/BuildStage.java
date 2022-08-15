package me.coley.recaf.ui.control.parameterinput.component.container.stage;

import javafx.scene.Node;
import me.coley.recaf.ui.control.parameterinput.component.Component;

public interface BuildStage<OutNode extends Node, Self extends BuildStage<OutNode, Self>>
	extends Component<OutNode, Self> {}
