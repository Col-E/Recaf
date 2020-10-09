package me.coley.recaf.ui.controls;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;

/**
 * Wrapper for grid in two columns, intended for labeled <i>(left)</i> content<i>(right)</i>.
 *
 * @author Matt
 */
public class ColumnPane extends BorderPane {
	protected final GridPane grid = new GridPane();
	protected int row;

	/**
	 * Setup grid.
	 */
	public ColumnPane() {
		setCenter(grid);
		setPadding(new Insets(5, 10, 5, 10));
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(70);
		column2.setPercentWidth(30);
		column2.setFillWidth(true);
		column2.setHgrow(Priority.ALWAYS);
		column2.setHalignment(HPos.RIGHT);
		grid.getColumnConstraints().addAll(column1, column2);
		grid.setVgap(5.0);
	}

	/**
	 * Add row of controls.
	 *
	 * @param left
	 * 		Smaller left control.
	 * @param right
	 * 		Larger right control.
	 */
	public void add(Node left, Node right) {
		// Dummy SubLabeled for proper sizing. Otherwise, a Region would suffice.
		if (left == null)
			left = new SubLabeled(" ", " ");
		// Add controls
		grid.add(left, 0, row);
		grid.add(right, 1, row);
		row++;
		// Force allow HGrow, using Region instead of Node due to inconsistent behavior using Node
		if (right instanceof Region)
			((Region) right).setMaxWidth(Double.MAX_VALUE);
	}
}
