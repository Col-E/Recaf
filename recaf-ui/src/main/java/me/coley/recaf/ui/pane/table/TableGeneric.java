package me.coley.recaf.ui.pane.table;

public class TableGeneric {
	private String member;
	private String value;
	private String meaning;

	public TableGeneric(String member, String value, String meaning) {
		this.member = member;
		this.value = value;
		this.meaning = meaning;
	}

	public void setMember(String member) {
		this.member = member;
	}

	public String getMember() {
		return member;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getMeaning() {
		return meaning;
	}

	public void setMeaning(String meaning) {
		this.meaning = meaning;
	}
}
