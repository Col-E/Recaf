package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.OptionalHeader;
import com.kichik.pecoff4j.PE;

/**
 * Table display for optional headers.
 *
 * @author Wolfie / win32kbase
 */
public class OptionalTableDisplayMode implements TableDisplayMode {
	@Override
	public void apply(PE pe, SizedDataTypeTable table) {
		OptionalHeader optionalHeader = pe.getOptionalHeader();
		table.addWord("Magic", optionalHeader.getMagic(), "Magic number");
		table.addByte("MajorLinkerVersion", optionalHeader.getMajorLinkerVersion(), "Major linker version");
		table.addByte("MinorLinkerVersion", optionalHeader.getMinorLinkerVersion(), "Minor linker version");
		table.addDword("SizeOfCode", optionalHeader.getSizeOfCode(), "Size of code");
		table.addDword("SizeOfInitializedData", optionalHeader.getSizeOfInitializedData(), "Size of initialized data");
		table.addDword("SizeOfUninitializedData", optionalHeader.getSizeOfUninitializedData(), "Size of uninitialized data");
		table.addDword("AddressOfEntryPoint", optionalHeader.getAddressOfEntryPoint(), "Address of entry point (.unkn)");
		//new TableDword("BaseOfCode")
		// TODO: The rest of the optional header info
	}
}
