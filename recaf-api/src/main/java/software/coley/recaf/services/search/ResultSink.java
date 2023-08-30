package software.coley.recaf.services.search;

import jakarta.annotation.Nonnull;
import software.coley.recaf.path.PathNode;

/**
 * Outline of a sink for {@link SearchVisitor} implementations to feed data into.
 *
 * @author Matt Coley
 */
public interface ResultSink {
	/**
	 * @param path
	 * 		Path of found value.
	 * @param value
	 * 		Found value.
	 */
	void accept(@Nonnull PathNode<?> path, @Nonnull Object value);
}
