package me.coley.recaf.ui.pane.elf;

import me.coley.recaf.ui.behavior.Updatable;
import me.coley.recaf.ui.pane.table.TableDisplayMode;
import net.fornwall.jelf.ElfFile;

/**
 * Generic outline for table display modes for ELF files.
 *
 * @author Matt Coley
 */
public interface ElfTableDisplayMode<T> extends TableDisplayMode<T>, Updatable<ElfFile> {
}
