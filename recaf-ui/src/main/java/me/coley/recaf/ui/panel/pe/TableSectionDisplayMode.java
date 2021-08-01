package me.coley.recaf.ui.panel.pe;

import com.kichik.pecoff4j.SectionHeader;

/**
 * Generic outline for section header table display modes.
 *
 * @author Wolfie / win32kbase
 */
public interface TableSectionDisplayMode {
    /**
     * @param sectionHeader The section header to pull info from.
     * @param table The table to populate with information.
     */
    void apply(SectionHeader sectionHeader, SizedDataTypeTable table);
}