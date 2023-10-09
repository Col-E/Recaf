package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.martinez.pe.headers.ImageSectionHeader;

import java.util.Map;
import java.util.TreeMap;

/**
 * Table display for section headers.
 *
 * @author Wolfie / win32kbase
 */
public class SectionTableDisplayMode implements TableDisplayMode<ImageSectionHeader> {
	/**
	 * Section flags.
	 */
	private final Map<Integer, String> SF_MAP = new TreeMap<Integer, String>() {{
		put(0x00000000, "Reserved");
		put(0x00000001, "Reserved");
		put(0x00000002, "Reserved");
		put(0x00000004, "Reserved");
		put(0x00000008, "IMAGE_SCN_TYPE_NO_PAD");
		put(0x00000010, "Reserved");
		put(0x00000020, "IMAGE_SCN_CNT_CODE");
		put(0x00000040, "IMAGE_SCN_CNT_INITIALIZED_DATA");
		put(0x00000080, "IMAGE_SCN_CNT_UNINITIALIZED_DATA");
		put(0x00000100, "Reserved");
		put(0x00000200, "IMAGE_SCN_LNK_INFO");
		put(0x00000400, "Reserved");
		put(0x00000800, "IMAGE_SCN_LNK_REMOVE");
		put(0x00001000, "IMAGE_SCN_LNK_COMDAT");
		put(0x00008000, "IMAGE_SCN_GPREL");
		put(0x00020000, "Reserved");
		put(0x00040000, "Reserved");
		put(0x00080000, "Reserved");
		put(0x00100000, "IMAGE_SCN_ALIGN_1BYTES");
		put(0x00200000, "IMAGE_SCN_ALIGN_2BYTES");
		put(0x00300000, "IMAGE_SCN_ALIGN_4BYTES");
		put(0x00400000, "IMAGE_SCN_ALIGN_8BYTES");
		put(0x00500000, "IMAGE_SCN_ALIGN_16BYTES");
		put(0x00600000, "IMAGE_SCN_ALIGN_32BYTES");
		put(0x00700000, "IMAGE_SCN_ALIGN_64BYTES");
		put(0x00800000, "IMAGE_SCN_ALIGN_128BYTES");
		put(0x00900000, "IMAGE_SCN_ALIGN_256BYTES");
		put(0x00A00000, "IMAGE_SCN_ALIGN_512BYTES");
		put(0x00B00000, "IMAGE_SCN_ALIGN_1024BYTES");
		put(0x00C00000, "IMAGE_SCN_ALIGN_2048BYTES");
		put(0x00D00000, "IMAGE_SCN_ALIGN_4096BYTES");
		put(0x00E00000, "IMAGE_SCN_ALIGN_8192BYTES");
		put(0x01000000, "IMAGE_SCN_LNK_NRELOC_OVFL");
		put(0x02000000, "IMAGE_SCN_MEM_DISCARDABLE");
		put(0x04000000, "IMAGE_SCN_MEM_NOT_CACHED");
		put(0x08000000, "IMAGE_SCN_MEM_NOT_PAGED");
		put(0x10000000, "IMAGE_SCN_MEM_SHARED");
		put(0x20000000, "IMAGE_SCN_MEM_EXECUTE");
		put(0x40000000, "IMAGE_SCN_MEM_READ");
		put(0x80000000, "IMAGE_SCN_MEM_WRITE");
	}};

	@Override
	public void apply(ImageSectionHeader sectionHeader, SizedDataTypeTable table) {
		table.addDword("VirtualSize", (int) sectionHeader.getVirtualSize(), "Virtual size");
		table.addDword("VirtualAddress", (int) sectionHeader.virtualAddress, "Virtual address");
		table.addDword("SizeOfRawData", (int) sectionHeader.sizeOfRawData, "Size of raw data");
		table.addDword("PointerToRawData", (int) sectionHeader.pointerToRawData, "Pointer to raw data");
		table.addDword("PointerToRelocations", (int) sectionHeader.pointerToRelocations, "Pointer to relocations");
		table.addDword("PointerToLineNumbers", (int) sectionHeader.pointerToLinenumbers, "Pointer to line numbers");
		table.addDword("NumberOfRelocations", sectionHeader.numberOfRelocations, "Number of relocations");
		table.addDword("NumberOfLineNumbers", sectionHeader.numberOfLinenumbers, "Number of line numbers");
		table.addDword("Characteristics", (int) sectionHeader.characteristics, "Section characteristics");

		long characteristics = sectionHeader.characteristics;
		SF_MAP.forEach((sectionFlag, name) -> {
			if ((characteristics & sectionFlag) > 0) {
				table.addDword("", sectionFlag, name);
			}
		});
	}
}
