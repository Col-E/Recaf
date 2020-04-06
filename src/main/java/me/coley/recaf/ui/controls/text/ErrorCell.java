package me.coley.recaf.ui.controls.text;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.text.Text;
import me.coley.recaf.util.struct.Pair;
import org.fxmisc.richtext.CodeArea;

/**
 * Cell renderer.
 *
 * @author Matt
 */
public class ErrorCell extends ListCell<Pair<Integer, String>> {
	private final CodeArea codeArea;

	/**
	 * @param codeArea
	 * 		Code area containing the text with errors.
	 */
	public ErrorCell(CodeArea codeArea) {
		this.codeArea = codeArea;
	}

	@Override
	public void updateItem(Pair<Integer, String> item, boolean empty) {
		super.updateItem(item, empty);
		if(empty) {
			setText(null);
			setGraphic(null);
		} else {
			setText(item.getValue());
			int index = item.getKey();
			Node g = new Text(String.valueOf(index + 1));
			g.getStyleClass().addAll("bold", "error-cell");
			setGraphic(g);
			// on-click: go to line
			if(index >= 0) {
				setOnMouseClicked(me -> {
					codeArea.moveTo(index, 0);
					codeArea.requestFollowCaret();
					codeArea.requestFocus();
				});
			} else {
				setText(getText() + "\n(Cannot resolve line number from error)");
			}
		}
	}
}
