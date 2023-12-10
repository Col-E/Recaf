package me.coley.recaf.ui.jfxbuilder.component.container.stage.creation;

import javafx.scene.layout.Region;
import me.coley.recaf.ui.jfxbuilder.component.container.stage.AddStage;

public interface ConfigureGriddedCreationStage<
		NodeOut extends Region,
		NextAS extends AddStage<NodeOut, NextAS>,
		Next extends AddOrConfigureStage<NodeOut, NextAS>,
		Self extends ConfigureGriddedCreationStage<NodeOut, NextAS, Next, Self>
	> extends RegionCreationStage<NodeOut, NextAS, Next, Self> {

	Next strategy(AddingStrategy strategy);

}
