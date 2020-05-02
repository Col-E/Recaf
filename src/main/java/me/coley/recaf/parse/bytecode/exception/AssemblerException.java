package me.coley.recaf.parse.bytecode.exception;

import me.coley.recaf.util.struct.LineException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Exception for invalid assembler input.
 *
 * @author Matt
 */
public class AssemblerException extends Exception implements LineException {
	private final List<LineException> subExceptions = new ArrayList<>();
	private final int line;

	/**
	 * @param message
	 * 		Reason for assembler error.
	 */
	public AssemblerException(String message) {
		this(null, message, -1);
	}

	/**
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public AssemblerException(String message, int line) {
		this(null, message, line);
	}

	/**
	 * @param t
	 * 		Cause exception.
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public AssemblerException(Throwable t, String message, int line) {
		super(Objects.requireNonNull(message), t);
		this.line = line;
	}

	/**
	 * @param exceptions
	 * 		Exceptions to add.
	 * @param <T>
	 * 		Type of exception.
	 */
	public <T extends LineException> void addSubExceptions(List<T> exceptions) {
		subExceptions.addAll(exceptions);
	}

	/**
	 * Since this can be a wrapper for multiple exceptions <i>(not suppressed per-say)</i> we want
	 * to access them.
	 *
	 * @return Sub exceptions.
	 */
	public List<LineException> getSubExceptions() {
		return subExceptions;
	}

	/**
	 * @return Line number relevant to the error.
	 * May be {@code -1} if it is not specific to one line.
	 */
	@Override
	public int getLine() {
		return line;
	}
}
