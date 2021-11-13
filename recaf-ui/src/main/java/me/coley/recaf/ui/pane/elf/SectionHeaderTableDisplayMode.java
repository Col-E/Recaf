package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSectionHeader;

/**
 * Generic outline for section header table display modes.
 *
 * @author Wolfie / win32kbase
 */
public interface SectionHeaderTableDisplayMode {
    /**
     * @param elf
     * 		The ELF file to pull info from.
     * @param table
     * 		The table to populate with information.
     */
    void apply(ElfFile elf, ElfSectionHeader programHeader, SizedDataTypeTable table);
}