package me.coley.recaf.ui.pane.pe;

import me.martinez.pe.ImageSectionHeader;

/**
 * Table display for section headers.
 *
 * @author Wolfie / win32kbase
 */
public class SectionTableDisplayMode implements TableSectionDisplayMode {
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
	}
}
