package software.coley.recaf.info;

import jakarta.annotation.Nonnull;

/**
 * Outline of a type that can be identified by name.
 *
 * @author Matt Coley
 */
public interface Named {
	/**
	 * @return Identifying name.
	 */
	@Nonnull
	String getName();
}
