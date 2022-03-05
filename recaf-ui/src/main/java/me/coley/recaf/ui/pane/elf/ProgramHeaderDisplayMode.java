package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSegment;

import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for ELF program headers.
 *
 * @author Wolfie / win32kbase
 */
public class ProgramHeaderDisplayMode implements ElfTableDisplayMode<ElfSegment> {
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
			put(0x6474E550, "PT_GNU_EH_FRAME, location and size of exception info");
			put(0x6474E551, "PT_GNU_STACK, permissions of stack segment");
			put(0x6474E552, "PT_GNU_RELRO, location and size of relocated read-only segment");
		}
	};
	private ElfFile elf;

	@Override
	public void apply(ElfSegment programHeader, SizedDataTypeTable table) {
		table.addDword("p_type", programHeader.p_type, ST_MAP.getOrDefault(programHeader.p_type, "Unknown"));
		table.addDword("p_flags", programHeader.p_flags, "Flags");
		table.addAddress("p_offset", programHeader.p_offset, "Offset of segment", elf);
		table.addAddress("p_vaddr", programHeader.p_vaddr, "Virtual address of segment", elf);
		table.addAddress("p_paddr", programHeader.p_paddr, "Physical address of segment", elf);
		table.addAddress("p_filesz", programHeader.p_filesz, "Physical size of segment", elf);
		table.addAddress("p_memsz", programHeader.p_memsz, "Virtual size of segment", elf);
		table.addAddress("p_align", programHeader.p_align, programHeader.p_align <= 1 ? "No alignment" : "Has alignment", elf);
	}

	@Override
	public void onUpdate(ElfFile elf) {
		this.elf = elf;
	}
}
