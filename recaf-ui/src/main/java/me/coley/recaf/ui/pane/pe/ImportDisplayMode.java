package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.martinez.pe.CachedLibraryImports;

/**
 * Generic outline for imports
 *
 * @author Wolfie / win32kbase
 */
public interface ImportDisplayMode {
	/**
	 * @param importEntry
	 * 		The import entry to pull info from.
	 * @param table
	 * 		The table to populate with information.
	 */
	void apply(CachedLibraryImports importEntry, SizedDataTypeTable table);
}