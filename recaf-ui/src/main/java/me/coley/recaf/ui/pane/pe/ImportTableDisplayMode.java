package me.coley.recaf.ui.pane.pe;

import me.coley.recaf.ui.pane.table.SizedDataTypeTable;
import me.martinez.pe.CachedImportEntry;
import me.martinez.pe.CachedLibraryImports;

/**
 * Table display for optional headers.
 *
 * @author Wolfie / win32kbase
 */
public class ImportTableDisplayMode implements ImportDisplayMode {
    @Override
    public void apply(CachedLibraryImports cachedLibraryImports, SizedDataTypeTable table) {
        // Remove the 'Meaning' column, we don't need it
        if (table.getColumns().get(PEExplorerPane.MEANING_COLUMN_INDEX) != null) {
            table.getColumns().remove(PEExplorerPane.MEANING_COLUMN_INDEX);
        }
        table.getColumns().get(PEExplorerPane.MEMBER_COLUMN_INDEX).setText("Name");
        table.getColumns().get(PEExplorerPane.VALUE_COLUMN_INDEX).setText("Ordinal");

        // No import directory
        if (cachedLibraryImports == null) {
            return;
        }

        for (int i = 0; i < cachedLibraryImports.getNumEntries(); i++) {
            CachedImportEntry cachedImportEntry = cachedLibraryImports.getEntry(i);
            String name = cachedImportEntry.getName() == null ? "" : cachedImportEntry.getName();
            int ordinal = cachedImportEntry.getOrdinal() == null ? -1 : cachedImportEntry.getOrdinal();
            table.addWord(name, ordinal, "");
        }
    }
}
