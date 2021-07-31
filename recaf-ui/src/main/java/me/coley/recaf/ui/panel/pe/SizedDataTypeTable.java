package me.coley.recaf.ui.panel.pe;

import javafx.scene.control.TableView;

/**
 * A {@link TableGeneric} oriented {@link TableView} with handy utility calls.
 *
 * @author Matt Coley
 */
public class SizedDataTypeTable extends TableView<TableGeneric> {
	public void addWord(String memberName, int value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	public void addWord(String memberName, String value, String meaning) {
		getItems().add(new TableWord(memberName, value, meaning));
	}

	public void addDword(String memberName, int value, String meaning) {
		getItems().add(new TableDword(memberName, value, meaning));
	}

	public void addByte(String memberName, int value, String meaning) {
		getItems().add(new TableByte(memberName, value, meaning));
	}
}
