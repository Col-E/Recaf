package me.coley.recaf.ui.controls.text;

import javafx.scene.control.ListView;
import me.coley.recaf.util.struct.Pair;

public class ErrorList extends ListView<Pair<Integer, String>> {
	private final EditorPane editorPaneOwner;

	public ErrorList(EditorPane editorPaneOwner) {
		this.editorPaneOwner = editorPaneOwner;
		setCellFactory(e -> new ErrorCell(editorPaneOwner.codeArea));
		getStyleClass().add("error-list");
	}
}
