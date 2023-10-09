package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.coley.recaf.ui.pane.table.TableWord;
import me.martinez.pe.PeImage;
import me.martinez.pe.headers.ImageFileHeader;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for file characteristics.
 *
 * @author Wolfie / win32kbase
 */
public class FileTableDisplayMode implements TableDisplayMode<PeImage> {
	/**
	 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/debug/pe-format#characteristics">
	 * PE Format - Characteristics</a>
	 */
	private static final Map<Integer, String> CHARACTERISTICS_MAP = new TreeMap<Integer, String>() {{
		put(0x0001, "IMAGE_FILE_RELOCS_STRIPPED");
		put(0x0002, "IMAGE_FILE_EXECUTABLE_IMAGE");
		put(0x0004, "IMAGE_FILE_LINE_NUMS_STRIPPED");
		put(0x0008, "IMAGE_FILE_LOCAL_SYMS_STRIPPED");
		put(0x0010, "IMAGE_FILE_AGGRESSIVE_WS_TRIM");
		put(0x0020, "IMAGE_FILE_LARGE_ADDRESS_AWARE");
		put(0x0040, "IMAGE_FILE_RESERVED");
		put(0x0080, "IMAGE_FILE_BYTES_REVERSED_LO");
		put(0x0100, "IMAGE_FILE_32BIT_MACHINE");
		put(0x0200, "IMAGE_FILE_DEBUG_STRIPPED");
		put(0x0400, "IMAGE_FILE_REMOVABLE_RUN_FROM_SWAP");
		put(0x0800, "IMAGE_FILE_NET_RUN_FROM_SWAP");
		put(0x1000, "IMAGE_FILE_SYSTEM");
		put(0x2000, "IMAGE_FILE_DLL");
		put(0x4000, "IMAGE_FILE_UP_SYSTEM_ONLY");
		put(0x8000, "IMAGE_FILE_BYTES_REVERSED_HI");
	}};


	@Override
	public void apply(PeImage pe, SizedDataTypeTable table) {
		ImageFileHeader fileHeader = pe.ntHeaders.fileHeader;

		table.addWord("Machine", fileHeader.machine, getMachineType(fileHeader.machine));
		table.addWord("NumberOfSections", fileHeader.numberOfSections, "Number of sections");
		table.addDword("TimeDateStamp", (int) fileHeader.timeDateStamp, Instant.ofEpochSecond(fileHeader.timeDateStamp).toString());
		table.addDword("PointerToSymbolTable", (int) fileHeader.pointerToSymbolTable, "Pointer to symbol table");
		table.addDword("NumberOfSymbols", (int) fileHeader.numberOfSymbols, "Number of symbols");
		table.addWord("SizeOfOptionalHeader", fileHeader.sizeOfOptionalHeader, "Size of optional header");
		table.addWord("Characteristics", fileHeader.characteristics, "PE characteristics");

		int fileCharacteristics = fileHeader.characteristics;
		CHARACTERISTICS_MAP.forEach((characteristicValue, name) -> {
			if ((fileCharacteristics & characteristicValue) > 0) {
				table.getItems().add(new TableWord("", characteristicValue, name));
			}
		});
	}


	/**
	 * @param machine
	 * 		Machine type key.
	 *
	 * @return String representation of machine type.
	 *
	 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/debug/pe-format#machine-types">
	 * PE Format - Machine Types</a>
	 */
	private static String getMachineType(int machine) {
		switch (machine) {
			case 0x0:
				return "Any machine type";
			case 0x1d3:
				return "Matsushita AM33";
			case 0x8664:
				return "x64";
			case 0x1c0:
				return "ARM little endian";
			case 0xaa64:
				return "ARM64 little endian";
			case 0x1c4:
				return "ARM Thumb-2 little endian";
			case 0xebc:
				return "EFI byte code";
			case 0x14c:
				return "Intel 386 or later processors and compatible processors";
			case 0x200:
				return "Intel Itanium processor family";
			case 0x9041:
				return "Mitsubishi M32R little endian";
			case 0x266:
				return "MIPS16";
			case 0x366:
				return "MIPS with FPU";
			case 0x466:
				return "MIPS16 with FPU";
			case 0x1f0:
				return "Power PC little endian";
			case 0x1f1:
				return "Power PC with floating point support";
			case 0x166:
				return "MIPS little endian";
			case 0x5032:
				return "RISC-V 32-bit address space";
			case 0x5064:
				return "RISC-V 64-bit address space";
			case 0x5128:
				return "RISC-V 128-bit address space";
			case 0x1a2:
				return "Hitachi SH3";
			case 0x1a3:
				return "Hitachi SH3 DSP";
			case 0x1a6:
				return "Hitachi SH4";
			case 0x1a8:
				return "Hitachi SH5";
			case 0x1c2:
				return "Thumb";
			case 0x169:
				return "MIPS little-endian WCE v2";
			default:
				return "Unknown";
		}
	}
}
