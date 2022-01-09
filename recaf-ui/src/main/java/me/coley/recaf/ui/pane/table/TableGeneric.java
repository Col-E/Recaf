package me.coley.recaf.ui.pane.table;

/**
 * Basic unit for table entries in {@link SizedDataTypeTable}.
 *
 * @author Wolfie / win32kbase
 */
public class TableGeneric {
	private String member;
	private String value;
	private String meaning;

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableGeneric(String member, String value, String meaning) {
		this.member = member;
		this.value = value;
		this.meaning = meaning;
	}

	/**
	 * @param member
	 * 		Member name.
	 */
	public void setMember(String member) {
		this.member = member;
	}

	/**
	 * @return Member name.
	 */
	public String getMember() {
		return member;
	}

	/**
	 * @return Member value represented as text.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 * 		Member value represented as text.
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * @return Explanation of the value.
	 */
	public String getMeaning() {
		return meaning;
	}

	/**
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public void setMeaning(String meaning) {
		this.meaning = meaning;
	}
}
