package software.coley.recaf.config;

import software.coley.recaf.services.config.ConfigManager;

/**
 * Outline of a config container that is aware of when its contents are restored.
 *
 * @author Matt Coley
 */
public interface RestoreAwareConfigContainer extends ConfigContainer {
	/**
	 * Called when this container has its contents restored via {@link ConfigManager}.
	 */
	default void onRestore() {}

	/**
	 * Called when this container was checked for contents via {@link ConfigManager} for restoration,
	 * but no local files were found and thus there was nothing to restore.
	 */
	default void onNoRestore() {}
}
