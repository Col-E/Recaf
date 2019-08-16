package me.coley.recaf.parse.assembly;

/**
 * Assembly exception for when a line of text could not be parsed.
 *
 * @author Matt
 */
public class LineParseException extends Exception {
	private final String text;
	private int line;

	/**
	 * Creates a parse exception without the line.
	 *
	 * @param text
	 * 		The violating text.
	 * @param message
	 * 		Additional message.
	 */
	public LineParseException(String text, String message) {
		this(-1, text, message);
	}


	/**
	 * @param line
	 * 		Line number containing the violating text.
	 * @param text
	 * 		The violating text.
	 * @param message
	 * 		Additional message.
	 */
	public LineParseException(int line, String text, String message) {
		super(message);
		this.line = line;
		this.text = text;
	}

	/**
	 * @return Line number containing the violating text.
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @param line
	 * 		Line number containing the violating text.
	 */
	public void setLine(int line) {
		this.line = line;
	}

	/**
	 * @return The violating text.
	 */
	public String getText() {
		return text;
	}
}
