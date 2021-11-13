package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.martinez.pe.ImageSectionHeader;

/**
 * Generic outline for section header table display modes.
 *
 * @author Wolfie / win32kbase
 */
public interface TableSectionDisplayMode {
	/**
	 * @param sectionHeader
	 * 		The section header to pull info from.
	 * @param table
	 * 		The table to populate with information.
	 */
	void apply(ImageSectionHeader sectionHeader, SizedDataTypeTable table);
}