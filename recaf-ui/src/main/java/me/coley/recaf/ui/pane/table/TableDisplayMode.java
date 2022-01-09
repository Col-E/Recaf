package me.coley.recaf.ui.pane.table;

/**
 * Generic outline for table display modes.
 *
 * @param <T>
 * 		Generic value type.
 *
 * @author Wolfie / win32kbase
 */
public interface TableDisplayMode<T> {
	/**
	 * @param value
	 * 		The data to pull info from.
	 * @param table
	 * 		The table to populate with information.
	 */
	void apply(T value, SizedDataTypeTable table);
}