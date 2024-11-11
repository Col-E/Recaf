package software.coley.recaf.util.analysis;

import jakarta.annotation.Nonnull;

/**
 * Nullability state.
 *
 * @author Matt Coley
 */
public enum Nullness {
	UNKNOWN, NULL, NOT_NULL;

	/**
	 * @param other
	 * 		Other value to merge with.
	 *
	 * @return Common nullability state.
	 */
	@Nonnull
	public Nullness mergeWith(@Nonnull Nullness other) {
		if (this != other) return UNKNOWN;
		return this;
	}
}
