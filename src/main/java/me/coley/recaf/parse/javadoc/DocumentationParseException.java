package me.coley.recaf.parse.javadoc;

import org.jsoup.parser.ParseErrorList;

/**
 * Wrapper for jsoup parse error list.
 *
 * @author Matt
 */
public class DocumentationParseException extends Exception {
	private final String html;
	private final ParseErrorList errors;

	/**
	 * @param html The offending HTML.
	 * @param errors JSoup error list for the given html.
	 */
	public DocumentationParseException(String html, ParseErrorList errors) {
		this.html = html;
		this.errors = errors;
	}

	/**
	 * @return The offending HTML.
	 */
	public String getHtml() {
		return html;
	}

	/**
	 * @return JSoup error list for the given html.
	 */
	public ParseErrorList getErrors() {
		return errors;
	}
}
