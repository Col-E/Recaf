package software.coley.recaf.config;

import jakarta.annotation.Nonnull;

/**
 * Outline of persistence for {@link ConfigContainer}
 *
 * @author Matt Coley
 */
public interface ConfigPersistence {
	/**
	 * @param container
	 * 		Container with values to save to persistent medium.
	 */
	void save(@Nonnull ConfigContainer container);

	/**
	 * @param container
	 * 		Container with values to load from persistent medium.
	 */
	void load(@Nonnull ConfigContainer container);
}
