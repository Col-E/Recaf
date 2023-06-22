package software.coley.recaf.services.search.result;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Result of a string match.
 *
 * @author Matt Coley
 */
public class NumberResult extends Result<Number> {
	private final Number value;

	/**
	 * @param path
	 * 		Path to item containing the result.
	 * @param value
	 * 		Matched value.
	 */
	public NumberResult(@Nonnull PathNode<?> path, @Nonnull Number value) {
		super(path);
		this.value = value;
	}

	@Nonnull
	@Override
	protected Number getValue() {
		return value;
	}
}
