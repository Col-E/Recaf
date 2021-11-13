package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.martinez.pe.ImagePeHeaders;

/**
 * Generic outline for table display modes.
 *
 * @author Matt Coley
 */
public interface TableDisplayMode {
	/**
	 * @param pe
	 * 		The PE file to pull info from.
	 * @param table
	 * 		The table to populate with information.
	 */
	void apply(ImagePeHeaders pe, SizedDataTypeTable table);
}