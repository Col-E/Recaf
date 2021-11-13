package me.coley.recaf.ui.pane.table;

public class TableWord extends TableGeneric {

	public TableWord(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	public TableWord(String member, String value) {
		super(member, value, "");
	}

	public TableWord(String member, int value, String meaning) {
		this(member, "", meaning);

		if (value == -1) {
			this.setValue("");
		} else {
			this.setValue(String.format("%04X", value));
		}
	}

	public TableWord(String member, int value) {
		this(member, value, "");
	}

}
