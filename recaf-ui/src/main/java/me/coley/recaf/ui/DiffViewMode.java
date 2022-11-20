package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Translatable;

/**
 * Display options for {@link me.coley.recaf.ui.pane.DiffViewPane}.
 *
 * @author Justus Garbe
 */
public enum DiffViewMode implements Translatable {
	DECOMPILE,
	DISASSEMBLE;

	@Override
	public String getTranslationKey() {
		switch (this) {
			case DECOMPILE:
				return "menu.mode.diff.decompile";
			case DISASSEMBLE:
				return "menu.mode.diff.disassemble";
			default:
				return "?";
		}
	}

	@Override
	public String toString() {
		return Lang.get(getTranslationKey());
	}
}
