package me.coley.recaf.ui.panel.pe;


import com.kichik.pecoff4j.PE;
import com.kichik.pecoff4j.SectionHeader;

/**
 * Generic outline for table display modes.
 *
 * @author Matt Coley
 */
public interface TableDisplayMode {
	/**
	 * @param pe    The PE file to pull info from.
	 * @param table The table to populate with information.
	 */
	void apply(PE pe, SizedDataTypeTable table);
}