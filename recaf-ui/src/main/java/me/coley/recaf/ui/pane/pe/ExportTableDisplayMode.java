package me.coley.recaf.ui.pane.pe;

import me.martinez.pe.CachedExportEntry;
import me.martinez.pe.CachedImageExports;
import me.martinez.pe.CachedImportEntry;
import me.martinez.pe.ImagePeHeaders;

/**
 * Table display for export directory.
 *
 * @author Wolfie / win32kbase
 */
public class ExportTableDisplayMode implements ExportDisplayMode {
    @Override
    public void apply(CachedImageExports cachedImageExports, SizedDataTypeTable table) {
        // No export directory
        if (cachedImageExports == null) {
            return;
        }

        for (int i = 0; i < cachedImageExports.getNumEntries(); i++) {
            CachedExportEntry cachedExportEntry = cachedImageExports.getEntry(i);
            String name = cachedExportEntry.getName() == null ? "No name" : cachedExportEntry.getName();
            int ordinal = cachedExportEntry.getOrdinal();
            table.addWord(name, ordinal, "Exported by both");
        }
    }
}
