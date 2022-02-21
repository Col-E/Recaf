package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import net.fornwall.jelf.ElfFile;

import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for ELF headers.
 *
 * @author Wolfie / win32kbase
 */
public class ElfHeaderDisplayMode implements TableDisplayMode<ElfFile> {
	/**
	 * Application binary interface names.
	 */
	private static final Map<Integer, String> ABI_MAP = new TreeMap<Integer, String>() {{
		put(0x1, "System V");
		put(0x2, "HP-UX");
		put(0x3, "NetBSD");
		put(0x4, "Linux");
		put(0x5, "GNU Hurd");
		put(0x6, "Solaris");
		put(0x7, "AIX");
		put(0x8, "IRIX");
		put(0x9, "FreeBSD");
		put(0xA, "Tru64");
		put(0xB, "Novell Modesto");
		put(0xC, "OpenBSD");
		put(0xD, "OpenVMS");
		put(0xE, "NonStop Kernel");
		put(0xF, "AROS");
		put(0x10, "Fenix OS");
		put(0x11, "CloudABI");
		put(0x12, "Stratus Technologies OpenVOS");
	}};

	/**
	 * Object file types.
	 */
	private static final Map<Integer, String> OFT_MAP = new TreeMap<Integer, String>() {{
		put(0x00, "ET_NONE");
		put(0x01, "ET_REL");
		put(0x02, "ET_EXEC");
		put(0x03, "ET_DYN");
		put(0x04, "ET_CORE");
		put(0xFE00, "ET_LOOS");
		put(0xFEFF, "ET_HIOS");
		put(0xFF00, "ET_LOPROC");
		put(0xFFFF, "ET_HIPROC");
	}};

	/**
	 * Instruction set architectures.
	 */
	private static final Map<Integer, String> ISA_MAP = new TreeMap<Integer, String>() {{
		put(0x00, "No specific instruction set");
		put(0x01, "AT&T WE 32100");
		put(0x02, "SPARC");
		put(0x03, "x86");
		put(0x04, "Motorola 68000 (M68k)");
		put(0x05, "Motorola 88000 (M88k)");
		put(0x06, "Intel MCU");
		put(0x07, "Intel 80860");
		put(0x08, "MIPS");
		put(0x09, "IBM System/370");
		put(0x0A, "MIPS RS3000 Little-endian");
		put(0x0B, "Reserved");
		put(0x0C, "Reserved");
		put(0x0D, "Reserved");
		put(0x0E, "Hewlett-Packard PA-RISC");
		put(0x0F, "Reserved");
		put(0x13, "Intel 80960");
		put(0x14, "PowerPC");
		put(0x15, "PowerPC (64-bit)");
		put(0x16, "S390/S390x");
		put(0x17, "IBM SPU/SPC");
		put(0x18, "Reserved");
		put(0x19, "Reserved");
		put(0x20, "Reserved");
		put(0x21, "Reserved");
		put(0x22, "Reserved");
		put(0x23, "Reserved");
		put(0x24, "NEC V800");
		put(0x25, "Fujitsu FR20");
		put(0x26, "TRW RH-32");
		put(0x27, "Motorola RCE");
		put(0x28, "ARM");
		put(0x29, "Digital Alpha");
		put(0x2A, "SuperH");
		put(0x2B, "SPARC Version 9");
		put(0x2C, "Siemens TriCore embedded processor");
		put(0x2D, "Argonaut RISC Core");
		put(0x2E, "Hitachi H8/300");
		put(0x2F, "Hitachi H8/300H");
		put(0x30, "Hitachi H8S");
		put(0x31, "Hitachi H8/500");
		put(0x32, "IA-64");
		put(0x33, "Stanford MIPS-X");
		put(0x34, "Motorola ColdFire");
		put(0x35, "Motorola M68HC12");
		put(0x36, "Fujitsu MMA Multimedia Accelerator");
		put(0x37, "Siemens PCP");
		put(0x38, "Sony nCPU embedded RISC processor");
		put(0x39, "Denso NDR1 microprocessor");
		put(0x3A, "Motorola Star*Core processor");
		put(0x3B, "Toyota ME16 processor");
		put(0x3C, "STMicroelectronics ST100 processor");
		put(0x3D, "TinyJ");
		put(0x3E, "AMD x86-64");
		put(0x8C, "TMS320C6000 Family");
		put(0xAF, "MCST Elbrus e2k");
		put(0xB7, "ARM 64-bits");
		put(0xF3, "RISC-V");
		put(0xF7, "Berkeley Packet Filter");
		put(0x10, "WDC 65C816");
	}};

	@Override
	public void apply(ElfFile elf, SizedDataTypeTable table) {
		table.addDword("ei_mag", 0x464c457f, "Magic number");
		table.addByte("ei_class", elf.ei_class, elf.ei_class == 1 ? "32 bit binary" : "64 bit binary");
		table.addByte("ei_data", elf.ei_data, elf.ei_data == 1 ? "Little endian" : "Big endian");
		table.addByte("ei_version", elf.ei_version, elf.ei_version == 1 ? "Original version of ELF" : "Unknown");
		table.addByte("ei_osabi", elf.ei_osabi, ABI_MAP.getOrDefault((int) elf.ei_osabi, "Unknown"));
		table.addByte("es_abiversion", elf.es_abiversion, "ABI version");
		table.addByte("ei_pad", "00, 00, 00, 00, 00, 00, 00", "Padding (7)");
		table.addWord("e_type", elf.e_type, OFT_MAP.getOrDefault((int) elf.e_type, "Unknown"));
		table.addWord("e_machine", elf.e_machine, ISA_MAP.getOrDefault((int) elf.e_machine, "Unknown"));
		table.addDword("e_version", elf.e_version, elf.e_version == 1 ? "Original version of ELF" : "Unknown");
		table.addAddress("e_entry", elf.e_entry, "Address of entry point", elf);
		table.addAddress("e_phoff", elf.e_phoff, "Start of program header", elf);
		table.addAddress("e_shoff", elf.e_shoff, "Start of section header", elf);
		table.addDword("e_flags", elf.e_flags, "Flags");
		table.addWord("e_ehsize", elf.e_ehsize, "Elf header size");
		table.addWord("e_phentsize", elf.e_phentsize, "Size of program header entry");
		table.addWord("e_phnum", elf.e_phnum, "Number of program header entries");
		table.addWord("e_shentsize", elf.e_shentsize, "Size of section header entry");
		table.addWord("e_shnum", elf.e_shnum, "Number of section header entries");
		table.addWord("e_shstrndx", elf.e_shstrndx, "Index of section name string table");
	}
}
