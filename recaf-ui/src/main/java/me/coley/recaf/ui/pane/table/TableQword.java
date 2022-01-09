package me.coley.recaf.ui.pane.table;

/**
 * Represents a {@code qword} in a {@link SizedDataTypeTable}.
 *
 * @author Wolfie / win32kbase
 */
public class TableQword extends TableGeneric {
	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableQword(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 */
	public TableQword(String member, String value) {
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
	public TableQword(String member, long value, String meaning) {
		this(member, String.format("%08X", value), meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 */
	public TableQword(String member, long value) {
		this(member, value, "");
	}

}
