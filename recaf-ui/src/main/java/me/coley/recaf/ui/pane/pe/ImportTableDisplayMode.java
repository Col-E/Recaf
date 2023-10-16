package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.martinez.pe.ExportEntry;
import me.martinez.pe.ImportEntry;
import me.martinez.pe.LibraryExport;
import me.martinez.pe.LibraryImports;

import java.util.List;

/**
 * Table display for optional headers.
 *
 * @author Wolfie / win32kbase
 */
public class ImportTableDisplayMode implements TableDisplayMode<LibraryImports> {
	@Override
	public void apply(LibraryImports imports, SizedDataTypeTable table) {
		// Remove the 'Meaning' column, we don't need it
		if (table.getColumns().get(PEExplorerPane.MEANING_COLUMN_INDEX) != null) {
			table.getColumns().remove(PEExplorerPane.MEANING_COLUMN_INDEX);
		}
		table.getColumns().get(PEExplorerPane.MEMBER_COLUMN_INDEX).setText("Name");
		table.getColumns().get(PEExplorerPane.VALUE_COLUMN_INDEX).setText("Ordinal");

		// No import directory
		if (imports == null) {
			return;
		}

		for (int i = 0; i < imports.entries.size(); i++) {
			ImportEntry importEntry = imports.entries.get(i);
			String name = importEntry.name == null ? "" : importEntry.name;
			String ordinal = importEntry.ordinal == null ? "": importEntry.ordinal.toString();
			table.addWord(name, ordinal, "");
		}
	}
}
