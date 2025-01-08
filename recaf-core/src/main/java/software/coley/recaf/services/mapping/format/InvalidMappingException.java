package software.coley.recaf.services.mapping.format;

import jakarta.annotation.Nonnull;

/**
 * Wrapper to encompass any error encountered during mapping format reading / writing.
 *
 * @author Matt Coley
 */
public class InvalidMappingException extends Exception {
	/**
	 * @param cause
	 * 		Cause for mapping parse/write failure.
	 */
	public InvalidMappingException(@Nonnull Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * 		Detail message for why the mappings are invalid.
	 */
	public InvalidMappingException(@Nonnull String message) {
		super(message);
	}
}
