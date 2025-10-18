package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;

/**
 * Branching behavior for jumps.
 *
 * @author Matt Coley
 */
public enum Branching {
	UNKNOWN, TAKEN, NOT_TAKEN;

	/**
	 * @return Inverted branching behavior.
	 */
	@Nonnull
	public Branching invert() {
		return switch (this) {
			case NOT_TAKEN -> TAKEN;
			case TAKEN -> NOT_TAKEN;
			default -> UNKNOWN;
		};
	}
}
