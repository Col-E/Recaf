package me.coley.recaf.ui.controls.text;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import me.coley.recaf.util.struct.Pair;

/**
 * Cell renderer.
 *
 * @author Matt
 */
public class ErrorCell extends ListCell<Pair<Integer, String>> {
	@Override
	public void updateItem(Pair<Integer, String> item, boolean empty) {
		super.updateItem(item, empty);
		if(empty) {
			setText(null);
			setGraphic(null);
		} else {
			setText(item.getValue());
			Node g = new Text(item.getKey().toString());
			g.getStyleClass().addAll("bold", "error-cell");
			setGraphic(g);
		}
	}
}
