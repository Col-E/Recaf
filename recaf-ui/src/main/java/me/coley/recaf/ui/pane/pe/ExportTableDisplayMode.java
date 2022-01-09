package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import me.martinez.pe.CachedExportEntry;
import me.martinez.pe.CachedImageExports;

/**
 * Table display for export directory.
 *
 * @author Wolfie / win32kbase
 */
public class ExportTableDisplayMode implements TableDisplayMode<CachedImageExports> {
	@Override
	public void apply(CachedImageExports cachedImageExports, SizedDataTypeTable table) {
		// Remove the 'Meaning' column, we don't need it
		if (table.getColumns().get(PEExplorerPane.MEANING_COLUMN_INDEX) != null) {
			table.getColumns().remove(PEExplorerPane.MEANING_COLUMN_INDEX);
		}
		table.getColumns().get(PEExplorerPane.MEMBER_COLUMN_INDEX).setText("Name");
		table.getColumns().get(PEExplorerPane.VALUE_COLUMN_INDEX).setText("Ordinal");

		// No export directory
		if (cachedImageExports == null) {
			return;
		}

		for (int i = 0; i < cachedImageExports.getNumEntries(); i++) {
			CachedExportEntry cachedExportEntry = cachedImageExports.getEntry(i);
			String name = cachedExportEntry.getName() == null ? "" : cachedExportEntry.getName();
			int ordinal = cachedExportEntry.getOrdinal();
			table.addWord(name, ordinal, "");
		}
	}
}
