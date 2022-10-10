package me.coley.recaf.decompile.fallback.print;

import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.StringUtil;

/**
 * Common print interface for utility methods.
 *
 * @author Matt Coley
 */
public interface PrintBase {
	/**
	 * Primarily used for unqualified names.
	 *
	 * @param name
	 * 		Name to filter.
	 *
	 * @return Filtered name.
	 */
	static String filterName(String name) {
		return EscapeUtil.escapeNonValid(name);
	}

	/**
	 * Primarily used for internal names.
	 *
	 * @param name
	 * 		Name to filter and shorten.
	 *
	 * @return Shortened and filtered name.
	 */
	static String filterShortenName(String name) {
		return filterName(StringUtil.shortenPath(name));
	}
}
