package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a string match.
 *
 * @author Matt Coley
 */
public class StringResult extends Result<String> {
	private final String value;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param value
	 * 		Matched value.
	 */
	public StringResult(@Nonnull PathNode<?> path, @Nonnull String value) {
		super(path);
		this.value = value;
	}

	@Nonnull
	@Override
	protected String getValue() {
		return value;
	}
}
