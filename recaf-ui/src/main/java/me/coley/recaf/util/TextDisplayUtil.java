package me.coley.recaf.util;

import me.coley.recaf.config.Configs;

/**
 * Combines {@link EscapeUtil} and {@link StringUtil}.
 *
 * @author Matt Coley
 */
public class TextDisplayUtil {
	/**
	 * @param string
	 * 		Input.
	 *
	 * @return Output, shortened by path name, escaping common ascii values.
	 */
	public static String escapePath(String string) {
		if (string == null)
			return null;
		return EscapeUtil.escape(StringUtil.shortenPath(string));
	}

	/**
	 * @param string
	 * 		Input.
	 *
	 * @return Output, shortened by path name and then by config display length, escaping common ascii values.
	 */
	public static String escapeShorten(String string) {
		if (string == null)
			return null;
		String text = EscapeUtil.escape(string);
		int max = Math.min(text.length(), Configs.display().maxTreeTextLength);
		text = StringUtil.limit(text, "...", max);
		return text;
	}

	/**
	 * @param string
	 * 		Input.
	 *
	 * @return Output, shortened by path name and then by config display length, escaping common ascii values.
	 */
	public static String escapeShortenPath(String string) {
		if (string == null)
			return null;
		String text = escapePath(string);
		int max = Math.min(text.length(), Configs.display().maxTreeTextLength);
		text = StringUtil.limit(text, "...", max);
		return text;
	}

}
