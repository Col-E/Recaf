package me.coley.recaf.ui.jfxbuilder.component.container.stage.creation;

import javafx.scene.layout.Region;
import me.coley.recaf.ui.jfxbuilder.component.container.stage.AddStage;

public interface RegionCreationStage<
		NodeOut extends Region,
		NextAS extends AddStage<NodeOut, NextAS>,
		Next extends AddOrConfigureStage<NodeOut, NextAS>,
		Self extends RegionCreationStage<NodeOut, NextAS, Next, Self>
	> extends AddOrConfigureStage<NodeOut, NextAS> {

	Self minWidth(double minWidth);
	Self minHeight(double minHeight);
	Self maxWidth(double maxWidth);
	Self maxHeight(double maxHeight);
	Self prefWidth(double prefWidth);
	Self prefHeight(double prefHeight);
	Self maxSize(double maxWidth, double maxHeight);
	Self minSize(double minWidth, double minHeight);
	Self prefSize(double prefWidth, double prefHeight);
	Self snapToPixel(boolean snapToPixel);
	Self padding(double top, double right, double bottom, double left);
}
