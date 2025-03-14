package software.coley.recaf.util;

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
	public static String shortenEscape(String string) {
		if (string == null)
			return null;
		return EscapeUtil.escapeStandardAndUnicodeWhitespace(StringUtil.shortenPath(string));
	}

	/**
	 * @param string
	 * 		Input.
	 * @param limit
	 * 		Limit length.
	 *
	 * @return Output, shortened by path name and then by config display length, escaping common ascii values.
	 */
	public static String escapeLimit(String string, int limit) {
		if (string == null)
			return null;
		String text = EscapeUtil.escapeStandardAndUnicodeWhitespace(string);
		int max = Math.min(text.length(), limit);
		text = StringUtil.limit(text, "...", max);
		return text;
	}

	/**
	 * @param string
	 * 		Input.
	 * @param limit
	 * 		Limit length.
	 *
	 * @return Output, shortened by path name and then by config display length, escaping common ascii values.
	 */
	public static String shortenEscapeLimit(String string, int limit) {
		if (string == null)
			return null;
		String text = shortenEscape(string);
		int max = Math.min(text.length(), limit);
		text = StringUtil.limit(text, "...", max);
		return text;
	}

}
