package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableGeneric;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfStringTable;

/**
 * Table display for ELF string table.
 *
 * @author Wolfie / win32kbase
 */
public class StringTableDisplayMode implements ElfTableDisplayMode<ElfStringTable> {
	@Override
	public void apply(ElfStringTable stringTable, SizedDataTypeTable table) {
		table.getColumns().get(ElfExplorerPane.MEMBER_COLUMN_INDEX).setText("Offset");
		table.getColumns().get(ElfExplorerPane.VALUE_COLUMN_INDEX).setText("Length");
		table.getColumns().get(ElfExplorerPane.MEANING_COLUMN_INDEX).setText("Data");

		if (stringTable == null) {
			return;
		}

		// The way the ELF parsing library handles this is weird... the index argument is **not** an index, it's an offset
		for (int i = 1; i < stringTable.numStrings; ) {
			String str = stringTable.get(i);
			table.getItems().add(new TableGeneric(String.format("%08X", i), String.format("%d", str.length()), str));
			i += str.length() + 1;
		}
	}

	@Override
	public void onUpdate(ElfFile elf) {
		// Not needed in this case
	}
}
