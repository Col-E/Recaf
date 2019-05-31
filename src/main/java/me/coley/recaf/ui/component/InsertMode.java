package me.coley.recaf.ui.component;

import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Misc;

public enum InsertMode {
	BEFORE, AFTER;

	@Override
	public String toString() {
		return Lang.get(Misc.getTranslationKey("ui.edit.method.insert", this));
	}
}