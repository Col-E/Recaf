package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSectionHeader;

import java.util.Map;
import java.util.TreeMap;
/**
 * Table display for ELF section headers.
 *
 * @author Wolfie / win32kbase
 */
public class SectionHeaderDisplayMode implements ElfTableDisplayMode<ElfSectionHeader> {
	/**
	 * Section types.
	 */
	private static final Map<Integer, String> ST_MAP = new TreeMap<Integer, String>() {
		{
			put(0x0, "SHT_NULL, section header table entry unused");
			put(0x1, "SHT_PROGBITS, program data");
			put(0x2, "SHT_SYMTAB, symbol table");
			put(0x3, "SHT_STRTAB, string table");
			put(0x4, "SHT_RELA, relocation entries with addends");
			put(0x5, "SHT_HASH, symbol hash table");
			put(0x6, "SHT_DYNAMIC, dynamic linking information");
			put(0x7, "SHT_NOTE, notes");
			put(0x8, "SHT_NOBITS, program space with no data (bss)");
			put(0x9, "SHT_REL, relocation entries, no addends");
			put(0x0A, "SHT_SHLIB, reserved");
			put(0x0B, "SHT_DYNSYM, dynamic linker symbol table");
			put(0x0E, "SHT_INIT_ARRAY, array of constructors");
			put(0x0F, "SHT_FINI_ARRAY, array of destructors");
			put(0x10, "SHT_PREINIT_ARRAY, array of pre-constructors");
			put(0x11, "SHT_GROUP, section group");
			put(0x12, "SHT_SYMTAB_SHNDX, extended section indices");
			put(0x13, "SHT_NUM, number of defined types.");
			put(0x60000000, "SHT_LOOS, start OS-specific.");
		}
	};

	/**
	 * Section attributes.
	 */
	private static final Map<Long, String> SA_MAP = new TreeMap<Long, String>() {{
		put(0x1L, "SHF_WRITE, writable");
		put(0x2L, "SHF_ALLOC, occupies memory during execution");
		put(0x4L, "SHF_EXECINSTR, executable");
		put(0x10L, "SHF_MERGE, might be merged");
		put(0x20L, "SHF_STRINGS, contains null-terminated strings");
		put(0x40L, "SHF_INFO_LINK, 'sh_info' contains SHT index");
		put(0x80L, "SHF_LINK_ORDER, preserve order after combining");
		put(0x100L, "SHF_OS_NONCONFORMING, non-standard OS specific handling required");
		put(0x200L, "SHF_GROUP, section is member of a group");
		put(0x400L, "SHF_TLS, section hold thread-local data");
		put(0x0ff00000L, "SHF_MASKOS, OS-specific");
		put(0xf0000000L, "SHF_MASKPROC, processor-specific");
		put(0x4000000L, "SHF_ORDERED, special ordering requirement (Solaris)");
		put(0x8000000L, "SHF_EXCLUDE, section is excluded unless referenced or allocated (Solaris)");
	}};
	private ElfFile elf;

	@Override
	public void apply(ElfSectionHeader programHeader, SizedDataTypeTable table) {
		table.addDword("sh_name", programHeader.sh_name, String.format("Offset to name (%s)", programHeader.getName()));
		table.addDword("sh_type", programHeader.sh_type, ST_MAP.getOrDefault(programHeader.sh_type, "Unknown"));
		table.addAddress("sh_flags", programHeader.sh_flags, "", elf);

		long sh_flags = programHeader.sh_flags;
		SA_MAP.forEach((attributeValue, name) -> {
			if ((sh_flags & attributeValue) > 0) {
				table.addAddress("", attributeValue, name, elf);
			}
		});

		table.addAddress("sh_addr", programHeader.sh_addr, "Virtual address of section", elf);
		table.addAddress("sh_offset", programHeader.sh_offset, "Section offset", elf);
		table.addAddress("sh_size", programHeader.sh_size, "Physical size of section", elf);
		table.addDword("sh_link", programHeader.sh_link, "Section index of associated section");
		table.addDword("sh_info", programHeader.sh_info, "Additional section information");
		table.addAddress("sh_addralign", programHeader.sh_addralign, "Alignment", elf);
		table.addAddress("sh_entsize", programHeader.sh_entsize, "Size of each entry", elf);
	}

	@Override
	public void onUpdate(ElfFile elf) {
		this.elf = elf;
	}
}
