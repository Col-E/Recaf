package software.coley.recaf.util.kotlin.model;

import jakarta.annotation.Nonnull;

/**
 * Enum model of the kotlin metadata {@code "k"} field.
 *
 * @author Matt Coley
 */
public enum KtClassKind {
	CLASS,
	FILE,
	SYNTHETIC_CLASS,
	MULTI_FILE_CLASS_FACADE,
	MULTI_FILE_CLASS_PART;

	/**
	 * @param kind
	 * 		Value of {@code "k"} field in the kotlin metadata.
	 *
	 * @return Kind enum for the value.
	 */
	@Nonnull
	public static KtClassKind fromKindInt(int kind) {
		if (kind > 0 && kind < values().length)
			return values()[kind];

		// Fallback
		return CLASS;
	}
}
