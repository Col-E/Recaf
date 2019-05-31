package me.coley.recaf.parse.assembly.exception;

/**
 * Wrapper for assembler exceptions with the lines that caused them.
 *
 * @author Matt
 */
public class ExceptionWrapper {
	public final int line;
	public final Exception exception;

	public ExceptionWrapper(int line, Exception exception) {
		this.line = line;
		this.exception = exception;
	}

	public void printStackTrace() {
		exception.printStackTrace();
	}
}