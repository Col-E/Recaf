package me.coley.recaf.ui.pane.pe;

public class TableGeneric {
	private String member = null;
	private String value = null;
	private String meaning = null;

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
