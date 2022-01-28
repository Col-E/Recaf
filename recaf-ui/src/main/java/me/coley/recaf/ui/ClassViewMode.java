package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Translatable;

/**
 * Mode for {@link ClassView} that determines which {@link me.coley.recaf.ui.behavior.ClassRepresentation}
 * should be displayed.
 *
 * @author Matt Coley.
 */
public enum ClassViewMode implements Translatable {
	DECOMPILE,
	HEX;

	@Override
	public String getTranslationKey() {
		switch (this) {
			case DECOMPILE:
				return "menu.mode.class.decompile";
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
			case DECOMPILE:
				return Icons.CODE;
			case HEX:
				return Icons.NUMBERS;
			default:
				return Icons.HELP;
		}
	}
}
