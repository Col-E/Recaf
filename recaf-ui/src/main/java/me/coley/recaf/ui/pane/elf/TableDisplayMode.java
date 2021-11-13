package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;

/**
 * Generic outline for table display modes.
 *
 * @author Wolfie / win32kbase
 */
public interface TableDisplayMode {
    /**
     * @param elf
     * 		The ELF file to pull info from.
     * @param table
     * 		The table to populate with information.
     */
    void apply(ElfFile elf, SizedDataTypeTable table);
}