package me.coley.recaf.ui.control.parameterinput.component.container.grid;

import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.creation.ConfigureGriddedCreationStage;
import me.coley.recaf.ui.control.parameterinput.util.ColumnConstraintsBuilder;

import java.util.Arrays;
import java.util.function.Consumer;

public interface GridPaneComponentCreationStage<
		NodeOut extends GridPane,
		NextAS extends GridComponentAddStage<NodeOut, NextAS>,
		Self extends GridPaneComponentCreationStage<NodeOut, NextAS, Self>
	>
	extends ConfigureGriddedCreationStage<NodeOut, NextAS, Self, Self> {

	Self vgap(double vgap);
	Self hgap(double hgap);
	Self gridLinesVisible(boolean visible);
	Self alignment(Pos alignment);
	Self addColumnConstraints(ColumnConstraints... constraints);
	default Self addColumnConstraints(ColumnConstraintsBuilder... constraints) {
		return addColumnConstraints(Arrays.stream(constraints).map(ColumnConstraintsBuilder::build).toArray(ColumnConstraints[]::new));
	}

	default Self columnConstraint(Consumer<ColumnConstraintsBuilder> builder) {
		var b = ColumnConstraintsBuilder.columnConstrains();
		builder.accept(b);
		return addColumnConstraints(b.build());
	}

	default Self columnConstraint(ColumnConstraintsBuilder builder) {
		return addColumnConstraints(builder.build());
	}

	default Self defaultColumnConstraint() {
		return addColumnConstraints(new ColumnConstraints());
	}
}
