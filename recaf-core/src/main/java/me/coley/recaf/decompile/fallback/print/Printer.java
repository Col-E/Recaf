package me.coley.recaf.decompile.fallback.print;

/**
 * String printing wrapper of {@link StringBuilder}.
 * Helps with indentation and line-based print calls.
 *
 * @author Matt Coley
 */
public class Printer {
	public static final char FORCE_NEWLINE = '\u0003';
	private final StringBuilder out = new StringBuilder();
	private String indent;

	/**
	 * @param indent
	 * 		New indentation prefix.
	 */
	public void setIndent(String indent) {
		this.indent = indent;
	}

	/**
	 * Appends a line with a {@link #setIndent(String) configurable indent}.
	 *
	 * @param line
	 * 		Line to print.
	 */
	public void appendLine(String line) {
		if (indent != null)
			out.append(indent);
		out.append(line.replace(FORCE_NEWLINE, '\n')).append("\n");
	}

	/**
	 * Appends all lines in the multi-line text.
	 *
	 * @param text
	 * 		Multi-line text to append.
	 *
	 * @see #appendLine(String)
	 */
	public void appendMultiLine(String text) {
		String[] lines = text.split("[\n\r]");
		for (String line : lines)
			appendLine(line);
	}

	/**
	 * Append blank new line.
	 */
	public void newLine() {
		out.append('\n');
	}

	@Override
	public String toString() {
		return out.toString();
	}
}
