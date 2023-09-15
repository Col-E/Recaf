package software.coley.recaf.services.config;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.ServiceConfig;

import java.util.*;

/**
 * Tracker for all {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigManager implements Service {
	public static final String SERVICE_ID = "config-manager";
	private final Map<String, ConfigContainer> containers = new TreeMap<>();
	private final List<ManagedConfigListener> listeners = new ArrayList<>();
	private final ConfigManagerConfig config;

	@Inject
	public ConfigManager(ConfigManagerConfig config, Instance<ConfigContainer> containers) {
		this.config = config;
		for (ConfigContainer container : containers)
			registerContainer(container);
	}

	/**
	 * @return All registered containers.
	 */
	@Nonnull
	public Collection<ConfigContainer> getContainers() {
		return containers.values();
	}

	/**
	 * @param container
	 * 		Container to register.
	 */
	public void registerContainer(@Nonnull ConfigContainer container) {
		String id = container.getId();
		if (containers.containsKey(id))
			throw new IllegalStateException("Container by ID '" + id + "' already registered");
		containers.put(id, container);

		// Alert listeners when content added
		for (ManagedConfigListener listener : listeners)
			listener.onRegister(container);
	}

	/**
	 * @param container
	 * 		Container to unregister.
	 */
	public void unregisterContainer(@Nonnull ConfigContainer container) {
		ConfigContainer removed = containers.remove(container.getId());

		// Alert listeners when content removed
		if (removed != null) {
			for (ManagedConfigListener listener : listeners)
				listener.onUnregister(removed);
		}
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addManagedConfigListener(ManagedConfigListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when the listener was removed.
	 * {@code false} when it wasn't added in the first place.
	 */
	public boolean removeManagedConfigListener(ManagedConfigListener listener) {
		return listeners.remove(listener);
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ServiceConfig getServiceConfig() {
		return config;
	}
}
