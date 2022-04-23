package me.coley.recaf.ui;

import me.coley.recaf.ui.util.Icons;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.Translatable;

/**
 * Mode for {@link ClassView} that determines which {@link me.coley.recaf.ui.behavior.ClassRepresentation}
 * should be displayed for {@link me.coley.recaf.code.DexClassInfo} values.
 *
 * @author Matt Coley.
 */
public enum AndroidClassViewMode implements Translatable {
	DECOMPILE,
	SMALI;

	@Override
	public String getTranslationKey() {
		switch (this) {
			case DECOMPILE:
				return "menu.mode.class.decompile";
			case SMALI:
				return "menu.mode.class.smali";
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
			case SMALI:
				return Icons.COMPILE;
			default:
				return Icons.HELP;
		}
	}
}
