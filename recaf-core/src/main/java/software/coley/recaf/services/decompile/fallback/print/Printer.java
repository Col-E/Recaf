package software.coley.recaf.services.decompile.fallback.print;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.StringUtil;

/**
 * String printing wrapper of {@link StringBuilder}.
 * Helps with indentation and line-based print calls.
 *
 * @author Matt Coley
 */
public class Printer {
	private final StringBuilder out = new StringBuilder();
	private String indent;

	/**
	 * @param indent
	 * 		New indentation prefix.
	 */
	public void setIndent(@Nonnull String indent) {
		this.indent = indent;
	}

	/**
	 * Appends a line with a {@link #setIndent(String) configurable indent}.
	 *
	 * @param line
	 * 		Line to print.
	 */
	public void appendLine(@Nonnull String line) {
		if (indent != null)
			out.append(indent);
		out.append(line).append("\n");
	}

	/**
	 * Appends all lines in the multi-line text.
	 *
	 * @param text
	 * 		Multi-line text to append.
	 *
	 * @see #appendLine(String)
	 */
	public void appendMultiLine(@Nonnull String text) {
		String[] lines = StringUtil.splitNewline(text);
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