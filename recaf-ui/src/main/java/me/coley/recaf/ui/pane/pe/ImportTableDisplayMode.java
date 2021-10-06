package me.coley.recaf.ui.pane.pe;

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
        // No import directory
        if (cachedLibraryImports == null) {
            return;
        }

        for (int i = 0; i < cachedLibraryImports.getNumEntries(); i++) {
            CachedImportEntry cachedImportEntry = cachedLibraryImports.getEntry(i);
            String name = cachedImportEntry.getName() == null ? "No name" : cachedImportEntry.getName();
            int ordinal = cachedImportEntry.getOrdinal() == null ? 0 : cachedImportEntry.getOrdinal();
            String desc = cachedImportEntry.getName() == null ? "Imported by ordinal" : "Imported by name";
            table.addWord(name, ordinal, desc);
        }
    }
}
