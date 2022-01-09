package me.coley.recaf.ui.pane.table;

/**
 * Represents a {@code word} in a {@link SizedDataTypeTable}.
 *
 * @author Wolfie / win32kbase
 */
public class TableWord extends TableGeneric {
	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableWord(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 */
	public TableWord(String member, String value) {
		super(member, value, "");
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableWord(String member, int value, String meaning) {
		this(member, "", meaning);

		if (value == -1) {
			setValue("");
		} else {
			setValue(String.format("%04X", value));
		}
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 */
	public TableWord(String member, int value) {
		this(member, value, "");
	}
}
