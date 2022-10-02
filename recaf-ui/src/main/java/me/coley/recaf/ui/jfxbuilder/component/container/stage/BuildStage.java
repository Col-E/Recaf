package me.coley.recaf.ui.jfxbuilder.component.container.stage;

import javafx.scene.Node;
import me.coley.recaf.ui.jfxbuilder.component.Component;

public interface BuildStage<OutNode extends Node, Self extends BuildStage<OutNode, Self>>
	extends Component<OutNode, Self> {}
