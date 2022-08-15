package me.coley.recaf.ui.control.parameterinput.component.container;

import javafx.scene.layout.GridPane;
import me.coley.recaf.ui.control.parameterinput.component.container.grid.GridComponent;
import me.coley.recaf.ui.control.parameterinput.component.container.grid.GridPaneComponentCreationStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.AddStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.creation.AddingStrategy;
import me.coley.recaf.ui.control.parameterinput.component.control.label.LabelComponent;

import static me.coley.recaf.ui.control.parameterinput.util.ColumnConstraintsBuilder.columnConstrains;

public class Containers {
	private Containers() {}


	public static
	<NextAS extends AddStage<GridPane, NextAS>, R extends GridPaneComponentCreationStage<GridPane, NextAS, R>>
	R gridAsRows(int columns) {
		return (R) GridComponent.gridAsRows(columns);
	}

	public static
	<NextAS extends AddStage<GridPane, NextAS>, R extends GridPaneComponentCreationStage<GridPane, NextAS, R>>
	R gridAsColumns(int rows) {
		return (R) GridComponent.gridAsColumns(rows);
	}

	void test() {
		gridAsRows(2).strategy(AddingStrategy.ROW)
			.addColumnConstraints(
				columnConstrains().percentWidth(100),
				columnConstrains().percentWidth(100)
			)
			.add(LabelComponent.label("Hello")).build().node();
	}
}
