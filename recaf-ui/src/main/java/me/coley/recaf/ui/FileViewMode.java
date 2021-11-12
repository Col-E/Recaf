package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;

/**
 * Mode for {@link FileView} that determines which {@link me.coley.recaf.ui.behavior.FileRepresentation}
 * should be displayed.
 *
 * @author Matt Coley.
 */
public enum FileViewMode {
	AUTO,
	TEXT,
	HEX;

	@Override
	public String toString() {
		switch (this) {
			case AUTO:
				return Lang.get("menu.mode.file.auto");
			case TEXT:
				return Lang.get("menu.mode.file.text");
			case HEX:
				return Lang.get("menu.mode.file.hex");
			default:
				return "?";
		}
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
