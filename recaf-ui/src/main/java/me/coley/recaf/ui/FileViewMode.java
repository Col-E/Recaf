package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Translatable;

/**
 * Mode for {@link FileView} that determines which {@link me.coley.recaf.ui.behavior.FileRepresentation}
 * should be displayed.
 *
 * @author Matt Coley.
 */
public enum FileViewMode implements Translatable {
	AUTO,
	TEXT,
	HEX;

	@Override
	public String getTranslationKey() {
		switch (this) {
			case AUTO:
				return "menu.mode.file.auto";
			case TEXT:
				return "menu.mode.file.text";
			case HEX:
				return "menu.mode.file.hex";
			default:
				return "?";
		}
	}

	@Override
	public String toString() {
		return Lang.get(getTranslationKey());
	}

	/**
	 * @return Image path to represent the mode.
	 */
	public String image() {
		switch (this) {
			case AUTO:
				return Icons.SMART;
			case TEXT:
				return Icons.QUOTE;
			case HEX:
				return Icons.NUMBERS;
			default:
				return Icons.HELP;
		}
	}
}
