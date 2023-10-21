package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.martinez.pe.ExportEntry;
import me.martinez.pe.LibraryExport;

/**
 * Table display for export directory.
 *
 * @author Wolfie / win32kbase
 */
public class ExportTableDisplayMode implements TableDisplayMode<LibraryExport> {
	@Override
	public void apply(LibraryExport libraryExport, SizedDataTypeTable table) {
		// Remove the 'Meaning' column, we don't need it
		if (table.getColumns().get(PEExplorerPane.MEANING_COLUMN_INDEX) != null) {
			table.getColumns().remove(PEExplorerPane.MEANING_COLUMN_INDEX);
		}
		table.getColumns().get(PEExplorerPane.MEMBER_COLUMN_INDEX).setText("Name");
		table.getColumns().get(PEExplorerPane.VALUE_COLUMN_INDEX).setText("Ordinal");

		// No export directory
		if (libraryExport == null||libraryExport.entries == null) {
			return;
		}

		for (int i = 0; i < libraryExport.entries.length; i++) {
			ExportEntry exportEntry = libraryExport.entries[i];
			String name = exportEntry.name == null ? "" : exportEntry.name;
			int ordinal = exportEntry.ordinal;
			table.addWord(name, ordinal, "");
		}
	}
}
