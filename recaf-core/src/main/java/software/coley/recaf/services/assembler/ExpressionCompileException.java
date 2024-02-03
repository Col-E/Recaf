package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;

/**
 * Exception encompassing problems ocurring during the compilation of an expression all via {@link ExpressionCompiler}.
 *
 * @author Matt Coley
 */
public class ExpressionCompileException extends Exception {
	/**
	 * @param message
	 * 		Error message.
	 */
	public ExpressionCompileException(@Nonnull String message) {
		super(message);
	}

	/**
	 * @param cause
	 * 		The cause of the exception.
	 * @param message
	 * 		Error message.
	 */
	public ExpressionCompileException(@Nonnull Throwable cause, @Nonnull String message) {
		super(message, cause);
	}
}
