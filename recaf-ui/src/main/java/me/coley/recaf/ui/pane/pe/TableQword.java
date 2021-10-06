package me.coley.recaf.ui.pane.pe;

public class TableQword extends TableGeneric {

	public TableQword(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	public TableQword(String member, String value) {
		super(member, value, "");
	}

	public TableQword(String member, long value, String meaning) {
		this(member, Long.toHexString(value), meaning);
	}

	public TableQword(String member, long value) {
		this(member, (short) value, "");
	}

}
