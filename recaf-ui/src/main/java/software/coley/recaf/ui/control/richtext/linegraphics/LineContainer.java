package software.coley.recaf.ui.control.richtext.linegraphics;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/**
 * Container for adding sub-graphics per-line graphic.
 * Created in {@link RootLineGraphicFactory}.
 *
 * @author Matt Coley
 */
public class LineContainer extends StackPane {
	private static final int LINE_V_PADDING = 1;
	private static final int LINE_H_PADDING = 5;
	private static final Insets PADDING = new Insets(LINE_V_PADDING, LINE_H_PADDING, LINE_V_PADDING, LINE_H_PADDING);
	private final HBox box = new HBox();

	/**
	 * Accessible only to local package.
	 */
	LineContainer() {
		box.setAlignment(Pos.CENTER_LEFT);
		box.setPadding(PADDING);
		getChildren().add(box);
	}

	/**
	 * @param child
	 * 		Child to add spanning horizontally in the container.
	 * 		Appends to the left.
	 */
	public void addHorizontal(Node child) {
		box.getChildren().add(child);
	}

	/**
	 * @param child
	 * 		Child to add on top of the container.
	 * 		This refers to Z-indexing, not north-south verticality.
	 */
	public void addTopLayer(Node child) {
		getChildren().add(child);
	}
}
