package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSegment;

import java.util.Map;
import java.util.TreeMap;

public class ProgramHeaderDisplayMode implements ProgramHeaderTableDisplayMode {
    /**
     * Segment types.
     */
    private static final Map<Integer, String> ST_MAP = new TreeMap<Integer, String>() {
        {
            put(0x00000000, "PT_NULL, program header table entry unused");
            put(0x00000001, "PT_LOAD, loadable segment");
            put(0x00000002, "PT_DYNAMIC, dynamic linking information");
            put(0x00000003, "PT_INTERP, interpreter information");
            put(0x00000004, "PT_NOTE, auxiliary information");
            put(0x00000005, "PT_SHLIB, reserved");
            put(0x00000006, "PT_PHDR, segment containing program header table");
            put(0x00000007, "PT_TLS, thread-local storage template");
            put(0x60000000, "PT_LOOS, reserved inclusive range");
            put(0x6FFFFFFF, "PT_HIOS, reserved inclusive range");
            put(0x70000000, "PT_LOPROC, reserved inclusive range");
            put(0x7FFFFFFF, "PT_HIPROC, reserved inclusive range");
        }};

    @Override
    public void apply(ElfFile elf, ElfSegment programHeader, SizedDataTypeTable table) {
        table.addDword("p_type", programHeader.type, ST_MAP.getOrDefault(programHeader.type, "Unknown"));
        table.addDword("p_flags", programHeader.flags, "Flags");
        table.addAddress("p_offset", programHeader.offset, "Offset of segment", elf);
        table.addAddress("p_vaddr", programHeader.virtual_address, "Virtual address of segment", elf);
        table.addAddress("p_paddr", programHeader.physical_address, "Physical address of segment", elf);
        table.addAddress("p_filesz", programHeader.file_size, "Physical size of segment", elf);
        table.addAddress("p_memsz", programHeader.mem_size, "Virtual size of segment", elf);
        table.addAddress("p_align", programHeader.alignment, programHeader.alignment <= 1 ? "No alignment" : "Has alignment", elf);
    }
}
