package me.coley.recaf.ui.jfxbuilder.component.container.stage.creation;

import javafx.scene.layout.VBox;
import me.coley.recaf.ui.jfxbuilder.component.container.stage.AddStage;
import me.coley.recaf.ui.jfxbuilder.component.container.stage.ConfigureCreationStage;

public interface VerticalFlowCreationStage {
	<Next extends AddStage<VBox, Next> & ConfigureCreationStage<VBox, Next>>
	Next verticalFlow();
}
