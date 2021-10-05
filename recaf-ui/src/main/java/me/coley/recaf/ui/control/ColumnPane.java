package me.coley.recaf.ui.control;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;

/**
 * Wrapper for grid in two columns, intended for labeled <i>(left)</i> content<i>(right)</i>.
 *
 * @author Matt Coley
 */
public class ColumnPane extends BorderPane {
	protected final GridPane grid = new GridPane();
	protected int row;

	/**
	 * Setup grid.
	 *
	 * @param insets
	 * 		Padding between container and border.
	 * @param leftPercent
	 * 		Percent of the space for the left column to fill.
	 * @param rightPercent
	 * 		Percent of the space for right column to fill.
	 * @param vgap
	 * 		Vertical spacing between items.
	 */
	public ColumnPane(Insets insets, double leftPercent, double rightPercent, double vgap) {
		setCenter(grid);
		setPadding(insets);
		ColumnConstraints column1 = new ColumnConstraints();
		ColumnConstraints column2 = new ColumnConstraints();
		column1.setPercentWidth(leftPercent);
		column2.setPercentWidth(rightPercent);
		column2.setFillWidth(true);
		column2.setHgrow(Priority.ALWAYS);
		column2.setHalignment(HPos.RIGHT);
		grid.getColumnConstraints().addAll(column1, column2);
		grid.setVgap(vgap);
	}

	/**
	 * Setup grid.
	 */
	public ColumnPane() {
		this(new Insets(5, 10, 5, 10), 50, 50, 5);
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
		// Add controls
		if (left != null)
			grid.add(left, 0, row);
		if (right != null)
			grid.add(right, 1, row);
		row++;
		// Force allow HGrow, using Region instead of Node due to inconsistent behavior using Node
		if (right instanceof Region)
			((Region) right).setMaxWidth(Double.MAX_VALUE);
	}
}