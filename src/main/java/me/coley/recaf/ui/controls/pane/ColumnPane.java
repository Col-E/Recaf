package me.coley.recaf.ui.controls.pane;

import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import me.coley.recaf.ui.controls.SubLabeled;

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
		this(new Insets(5, 10, 5, 10), 70, 30, 5);
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
