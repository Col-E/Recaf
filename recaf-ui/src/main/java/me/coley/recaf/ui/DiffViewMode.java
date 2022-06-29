package me.coley.recaf.ui;

import me.coley.recaf.util.Translatable;

public enum DiffViewMode implements Translatable {

	CLASS,
	BYTECODE;


	@Override
	public String getTranslationKey() {
		switch (this) {
			case CLASS:
				return "menu.mode.diff.class";
			case BYTECODE:
				return "menu.mode.diff.bytecode";
			default:
				return "?";
		}
	}
}
