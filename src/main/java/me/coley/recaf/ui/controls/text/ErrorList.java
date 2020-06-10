package me.coley.recaf.ui.controls.text;

import javafx.scene.control.ListView;
import me.coley.recaf.util.struct.Pair;

/**
 * Listview wrapper of errors, denoted by line number and message content.
 *
 * @author Matt
 */
public class ErrorList extends ListView<Pair<Integer, String>> {
	/**
	 * @param editorPaneOwner
	 * 		Panel that contains the code editor with marked errors.
	 */
	public ErrorList(EditorPane<?, ?> editorPaneOwner) {
		setCellFactory(e -> new ErrorCell(editorPaneOwner.codeArea));
		getStyleClass().add("error-list");
	}
}
