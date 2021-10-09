package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;

/**
 * Mode for {@link ClassView} that determines which {@link me.coley.recaf.ui.behavior.ClassRepresentation}
 * should be displayed.
 *
 * @author Matt Coley.
 */
public enum ClassViewMode {
	DECOMPILE,
	HEX;

	@Override
	public String toString() {
		switch (this) {
			case DECOMPILE:
				return Lang.get("menu.mode.class.decompile");
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
			case DECOMPILE:
				return Icons.CODE;
			case HEX:
				return Icons.NUMBERS;
			default:
				return Icons.HELP;
		}
	}
}
