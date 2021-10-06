package me.coley.recaf.ui.pane.pe;

import me.martinez.pe.CachedImageExports;

/**
 * Generic outline for imports
 *
 * @author Wolfie / win32kbase
 */
public interface ExportDisplayMode {
    /**
     * @param importEntry
     * 		The import entry to pull info from.
     * @param table
     * 		The table to populate with information.
     */
    void apply(CachedImageExports importEntry, SizedDataTypeTable table);
}