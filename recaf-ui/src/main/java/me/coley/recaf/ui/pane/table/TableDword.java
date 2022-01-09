package me.coley.recaf.ui.pane.table;

/**
 * Represents a {@code dword} in a {@link SizedDataTypeTable}.
 *
 * @author Wolfie / win32kbase
 */
public class TableDword extends TableGeneric {
	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableDword(String member, String value, String meaning) {
		super(member, value, meaning);
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value represented as text.
	 */
	public TableDword(String member, String value) {
		super(member, value, "");
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 */
	public TableDword(String member, int value) {
		this(member, (short) value, "");
	}

	/**
	 * @param member
	 * 		Member name.
	 * @param value
	 * 		Member value.
	 * @param meaning
	 * 		Explanation of the value.
	 */
	public TableDword(String member, int value, String meaning) {
		this(member, String.format("%04X", value), meaning);
	}
}
