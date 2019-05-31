package me.coley.recaf.bytecode.search;

import me.coley.recaf.util.Lang;
import me.coley.recaf.util.Misc;

/**
 * How to handle string arguments in searches.
 * 
 * @author Matt
 */
public enum StringMode {
	CONTAINS,
	STARTS_WITH,
	ENDS_WITH,
	EQUALITY,
	REGEX;

	@Override
	public String toString() {
		return Lang.get(Misc.getTranslationKey("ui.search.mode", this));
	}
}