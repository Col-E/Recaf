package software.coley.recaf.services.config;

import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.config.ConfigContainer;

/**
 * Listenr for {@link ConfigManager#registerContainer(ConfigContainer)} and
 * {@link ConfigManager#unregisterContainer(ConfigContainer)} calls.
 *
 * @author Matt Coley
 */
public interface ManagedConfigListener extends PrioritySortable {
	/**
	 * @param container
	 * 		Registered config.
	 */
	void onRegister(@Nonnull ConfigContainer container);

	/**
	 * @param container
	 * 		Unregistered config.
	 */
	void onUnregister(@Nonnull ConfigContainer container);
}
