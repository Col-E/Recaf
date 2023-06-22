package software.coley.recaf.config;

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
	void save(ConfigContainer container);

	/**
	 * @param container
	 * 		Container with values to load from persistent medium.
	 */
	void load(ConfigContainer container);
}
