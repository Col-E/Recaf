package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.DOSHeader;
import com.kichik.pecoff4j.PE;
import com.kichik.pecoff4j.SectionHeader;

/**
 * Table display for section headers.
 *
 * @author Wolfie / win32kbase
 */
public class SectionTableDisplayMode implements TableSectionDisplayMode {
    @Override
    public void apply(SectionHeader sectionHeader, SizedDataTypeTable table) {
        table.addDword("VirtualSize", sectionHeader.getVirtualSize(), "Virtual size");
        table.addDword("VirtualAddress", sectionHeader.getVirtualAddress(), "Virtual address");
        table.addDword("SizeOfRawData", sectionHeader.getSizeOfRawData(), "Size of raw data");
        table.addDword("PointerToRawData", sectionHeader.getPointerToRawData(), "Pointer to raw data");
        table.addDword("PointerToRelocations", sectionHeader.getPointerToRelocations(), "Pointer to relocations");
        table.addDword("PointerToLineNumbers", sectionHeader.getPointerToLineNumbers(), "Pointer to line numbers");
        table.addDword("NumberOfRelocations", sectionHeader.getNumberOfRelocations(), "Number of relocations");
        table.addDword("NumberOfLineNumbers", sectionHeader.getNumberOfLineNumbers(), "Number of line numbers");
        table.addDword("Characteristics", sectionHeader.getCharacteristics(), "Section characteristics");
    }
}
