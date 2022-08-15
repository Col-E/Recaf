package me.coley.recaf.ui.control.parameterinput.component.container.grid;

import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.AddStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.creation.ConfigureGriddedCreationStage;
import me.coley.recaf.ui.control.parameterinput.util.ColumnConstraintsBuilder;

public interface GridPaneComponentCreationStage<
		NodeOut extends GridPane,
		NextAS extends AddStage<NodeOut, NextAS>,
		Self extends GridPaneComponentCreationStage<NodeOut, NextAS, Self>
	>
	extends ConfigureGriddedCreationStage<NodeOut, NextAS, Self, Self> {

	Self vgap(double vgap);
	Self hgap(double hgap);
	Self gridLinesVisible(boolean visible);
	Self alignment(Pos alignment);
	Self addColumnConstraints(ColumnConstraints... constraints);
	Self addColumnConstraints(ColumnConstraintsBuilder... constraints);
}
