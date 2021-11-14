package me.coley.recaf.ui.pane.table;

/**
 * Represents a {@code byte} in a {@link SizedDataTypeTable}.
 *
 * @author Wolfie / win32kbase
 */
public class TableByte extends TableGeneric {
	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableByte(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 */
	public TableByte(String member, String value) {
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
	public TableByte(String member, int value, String meaning) {
		this(member, String.format("%02X", (byte) value), meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 */
	public TableByte(String member, int value) {
		this(member, value, "");
	}

}
