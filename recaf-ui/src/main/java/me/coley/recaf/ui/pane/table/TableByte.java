package me.coley.recaf.ui.pane.table;

public class TableByte extends TableGeneric {

	public TableByte(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	public TableByte(String member, String value) {
		super(member, value, "");
	}

	public TableByte(String member, int value, String meaning) {
		this(member, String.format("%02X", (byte) value), meaning);
	}

	public TableByte(String member, int value) {
		this(member, value, "");
	}

}
