package me.coley.recaf.ui.control.parameterinput.component.container.grid;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.AddStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.BuildStage;
import me.coley.recaf.ui.control.parameterinput.component.container.stage.creation.AddingStrategy;
import me.coley.recaf.ui.control.parameterinput.util.ColumnConstraintsBuilder;

import java.util.function.Consumer;

interface IGridComponent extends GridPaneComponentCreationStage<GridPane, IGridComponent, IGridComponent>,
	BuildStage<GridPane, IGridComponent>, AddStage<GridPane, IGridComponent> {}

public class GridComponent implements IGridComponent {

	private final int rows;
	private final int columns;

	private int row = 0;
	private int column = 0;

	private AddingStrategy strategy = AddingStrategy.ROW;
	private final GridPane grid = new GridPane();

	private GridComponent(int rows, int columns) {
		this.rows = rows;
		this.columns = columns;
	}

	public static GridComponent gridAsRows(int columns) {
		return new GridComponent(-1, columns).strategy(AddingStrategy.ROW);
	}

	public static GridComponent gridAsColumns(int rows) {
		return new GridComponent(rows, -1).strategy(AddingStrategy.COLUMN);
	}

	@Override
	public GridPane node() {
		return grid;
	}

	@Override
	public GridComponent vgap(double vgap) {
		grid.setVgap(vgap);
		return this;
	}

	@Override
	public GridComponent hgap(double hgap) {
		grid.setHgap(hgap);
		return this;
	}

	@Override
	public GridComponent gridLinesVisible(boolean visible) {
		grid.setGridLinesVisible(visible);
		return this;
	}

	@Override
	public GridComponent alignment(Pos alignment) {
		grid.setAlignment(alignment);
		return this;
	}

	@Override
	public GridComponent addColumnConstraints(ColumnConstraints... constraints) {
		grid.getColumnConstraints().addAll(constraints);
		return this;
	}

	@Override
	public GridComponent addColumnConstraints(ColumnConstraintsBuilder... constraints) {
		for (ColumnConstraintsBuilder builder : constraints) {
			grid.getColumnConstraints().add(builder.build());
		}
		return this;
	}

	@Override
	public GridComponent add(Node node) {
		switch (strategy) {
			case ROW:
				grid.add(node, 0, row++, columns, 1);
				break;
			case COLUMN:
				grid.add(node, column++, 0, 1, rows);
				break;
			default:
				throw new IllegalStateException("Unsupported adding strategy: " + strategy);
		}
		return this;
	}

	@Override
	public GridComponent add(Node... nodes) {
		if (nodes.length == 0) {
			switch (strategy) {
				case ROW:
					row++;
					break;
				case COLUMN:
					column++;
				default:
			}
			return this;
		}
		switch (strategy) {
			case ROW:
				if (nodes.length != columns) {
					HBox box = new HBox(nodes);
					box.setAlignment(Pos.TOP_LEFT);
					box.setStyle("-fx-fill: red");
					add(box);
				} else {
					column = 0;
					for (Node node : nodes) {
						if (node == null) column++;
						else grid.add(node, column++, row, 1, 1);
					}
					row++;
				}
				break;
			case COLUMN:
				if (nodes.length != rows) {
					add(new VBox(nodes));
				} else {
					row = 0;
					for (Node node : nodes) {
						if (node == null) row++;
						else grid.add(node, column, row++, 1, 1);
					}
					column++;
				}
				break;
			default:
				throw new IllegalStateException("Unsupported adding strategy: " + strategy);
		}
		return this;
	}

	@Override
	public <Next extends BuildStage<GridPane, Next>> Next build() {
		return (Next) this;
	}

	@Override
	public GridComponent configure(Consumer<GridPane> config) {
		return null;
	}

	@Override
	public GridComponent strategy(AddingStrategy strategy) {
		this.strategy = strategy;
		return this;
	}

	@Override
	public GridComponent minWidth(double minWidth) {
		grid.setMinWidth(minWidth);
		return this;
	}

	@Override
	public GridComponent minHeight(double minHeight) {
		grid.setMinHeight(minHeight);
		return this;
	}

	@Override
	public GridComponent maxWidth(double maxWidth) {
		grid.setMaxWidth(maxWidth);
		return this;
	}

	@Override
	public GridComponent maxHeight(double maxHeight) {
		grid.setMaxHeight(maxHeight);
		return this;
	}

	@Override
	public GridComponent prefWidth(double prefWidth) {
		grid.setPrefWidth(prefWidth);
		return this;
	}

	@Override
	public GridComponent prefHeight(double prefHeight) {
		grid.setPrefHeight(prefHeight);
		return this;
	}

	@Override
	public GridComponent maxSize(double maxWidth, double maxHeight) {
		grid.setMaxSize(maxWidth, maxHeight);
		return this;
	}

	@Override
	public GridComponent minSize(double minWidth, double minHeight) {
		grid.setMinSize(minWidth, minHeight);
		return this;
	}

	@Override
	public GridComponent prefSize(double prefWidth, double prefHeight) {
		grid.setPrefSize(prefWidth, prefHeight);
		return this;
	}

	@Override
	public GridComponent snapToPixel(boolean snapToPixel) {
		grid.setSnapToPixel(snapToPixel);
		return this;
	}

	@Override
	public GridComponent padding(double top, double right, double bottom, double left) {
		grid.setPadding(new Insets(top, right, bottom, left));
		return this;
	}
}
