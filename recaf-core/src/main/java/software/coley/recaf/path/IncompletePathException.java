package software.coley.recaf.path;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Used by methods when handling input {@link PathNode} content indicates an element in the path is missing.
 *
 * @author Matt Coley
 */
public class IncompletePathException extends Exception {
	private final Class<?> missingType;

	/**
	 * @param missingType
	 * 		Missing type in the path.
	 */
	public IncompletePathException(@Nonnull Class<?> missingType) {
		this(missingType, null);
	}

	/**
	 * @param missingType
	 * 		Missing type in the path.
	 * @param message
	 * 		Problem message.
	 */
	public IncompletePathException(@Nonnull Class<?> missingType, @Nullable String message) {
		super(message);
		this.missingType = missingType;
	}

	/**
	 * @return Missing type in the path.
	 */
	@Nonnull
	public Class<?> getMissingType() {
		return missingType;
	}
}
