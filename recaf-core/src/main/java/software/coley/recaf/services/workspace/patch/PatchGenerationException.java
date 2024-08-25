package software.coley.recaf.services.workspace.patch;

import jakarta.annotation.Nonnull;

/**
 * Exception to outline various patch generation and serialization problems.
 *
 * @author Matt Coley
 */
public class PatchGenerationException extends Exception {
	public PatchGenerationException(@Nonnull Throwable cause, @Nonnull String message) {
		super(message, cause);
	}

	public PatchGenerationException(@Nonnull Throwable cause) {
		super(cause);
	}

	public PatchGenerationException(@Nonnull String message) {
		super(message);
	}
}
