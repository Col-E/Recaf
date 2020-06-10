package me.coley.recaf.parse.bytecode.exception;

import java.util.Objects;

/**
 * Extension of {@link AssemblerException} as an identifier for verification-related assembler problems.
 */
public class VerifierException extends AssemblerException {
	/**
	 * @param message
	 * 		Reason for assembler error.
	 */
	public VerifierException(String message) {
		this(null, message, -1);
	}

	/**
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public VerifierException(String message, int line) {
		this(null, message, line);
	}

	/**
	 * @param ex
	 * 		Cause exception.
	 * @param message
	 * 		Reason for assembler error.
	 * @param line
	 * 		Line number relevant to the error.
	 */
	public VerifierException(Exception ex, String message, int line) {
		super(ex, Objects.requireNonNull(message), line);
	}
}
