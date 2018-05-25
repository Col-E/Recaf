package me.coley.recaf.ui.component;

import me.coley.recaf.util.Lang;

public enum InsertMode {
	BEFORE("ui.edit.method.insert.before"), AFTER("ui.edit.method.insert.after");

	private final String text;

	InsertMode(String key) {
		this.text = Lang.get(key);
	}

	@Override
	public String toString() {
		return text;
	}
}