package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSegment;

/**
 * Generic outline for program header table display modes.
 *
 * @author Wolfie / win32kbase
 */
public interface ProgramHeaderTableDisplayMode {
    /**
     * @param elf
     * 		The ELF file to pull info from.
     * @param table
     * 		The table to populate with information.
     */
    void apply(ElfFile elf, ElfSegment programHeader, SizedDataTypeTable table);
}