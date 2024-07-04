package software.coley.recaf.services.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitialization;
import software.coley.recaf.config.ConfigCollectionValue;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.config.RestoreAwareConfigContainer;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.util.TestEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracker for all {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
@EagerInitialization // Eager so that all config containers will be up-to-date when injected
public class ConfigManager implements Service {
	public static final String SERVICE_ID = "config-manager";
	private static final Logger logger = Logging.get(ConfigManager.class);
	private final Map<String, ConfigContainer> containers = new TreeMap<>();
	private final List<ManagedConfigListener> listeners = new CopyOnWriteArrayList<>();
	private final ConfigManagerConfig config;
	private final RecafDirectoriesConfig fileConfig;
	private final GsonProvider gsonProvider;

	@Inject
	public ConfigManager(@Nonnull ConfigManagerConfig config, @Nonnull RecafDirectoriesConfig fileConfig,
	                     @Nonnull GsonProvider gsonProvider, @Nonnull Instance<ConfigContainer> containers) {
		this.config = config;
		this.fileConfig = fileConfig;
		this.gsonProvider = gsonProvider;
		for (ConfigContainer container : containers)
			registerContainer(container);
		load();
	}

	@PreDestroy
	private void save() {
		// Skip persisting in test environments
		if (TestEnvironment.isTestEnv())
			return;

		Gson gson = gsonProvider.getGson();
		for (ConfigContainer container : containers.values()) {
			// Skip writing empty containers
			if (container.getValues().isEmpty())
				continue;

			// Model the vales into a single object.
			JsonObject json = new JsonObject();
			for (ConfigValue<?> configValue : container.getValues().values()) {
				try {
					json.add(configValue.getId(), gson.toJsonTree(configValue.getValue()));
				} catch (IllegalArgumentException e) {
					logger.error("Could not find adapter for type: {}", configValue.getType(), e);
				} catch (Exception e) {
					logger.error("Failed to save config value: {}", configValue.getId(), e);
				}
			}

			// Write the appropriate path based on the container id.
			String key = container.getGroupAndId();
			Path containerPath = fileConfig.getConfigDirectory().resolve(key + ".json");
			try (JsonWriter writer = gson.newJsonWriter(Files.newBufferedWriter(containerPath))) {
				gson.toJson(json, writer);
			} catch (IOException e) {
				logger.error("Failed to save config container: {}", key, e);
			}
		}
	}

	@SuppressWarnings({"raw", "rawtypes"})
	private void load() {
		// Skip loading in test environments
		if (TestEnvironment.isTestEnv())
			return;

		Gson gson = gsonProvider.getGson();
		for (ConfigContainer container : containers.values()) {
			String key = container.getGroupAndId();
			Path containerPath = fileConfig.getConfigDirectory().resolve(key + ".json");
			if (!Files.exists(containerPath)) {
				if (container instanceof RestoreAwareConfigContainer listener)
					listener.onNoRestore();
				continue;
			}

			JsonObject json;
			try (JsonReader reader = gson.newJsonReader(Files.newBufferedReader(containerPath))) {
				json = Objects.requireNonNull(gson.fromJson(reader, JsonObject.class));
			} catch (Exception ex) {
				logger.error("Failed to load config container: {}", key, ex);
				continue;
			}

			for (ConfigValue value : container.getValues().values()) {
				String id = value.getId();

				// Skip loading if the file doesn't list the entry.
				if (!json.has(id))
					continue;

				try {
					loadValue(gson, value, json.get(id));
				} catch (IllegalArgumentException e) {
					logger.error("Could not find adapter for type: {}", value.getType(), e);
				} catch (Exception e) {
					logger.error("Failed to load config value: {}", id, e);
				}
			}

			// Notify the container it has restored its config values from storage.
			if (container instanceof RestoreAwareConfigContainer listener)
				listener.onRestore();
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void loadValue(Gson gson, ConfigValue value, JsonElement element) {
		if (value instanceof ConfigCollectionValue ccv) {
			List<Object> list = new ArrayList<>();
			JsonArray array = element.getAsJsonArray();
			for (JsonElement e : array) {
				list.add(gson.fromJson(e, ccv.getItemType()));
			}
			value.setValue(list);
		} else {
			value.setValue(gson.fromJson(element, value.getType()));
		}
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
		Unchecked.checkedForEach(listeners, listener -> listener.onRegister(container),
				(listener, t) -> logger.error("Exception thrown when registering container '{}'", container.getId(), t));
	}

	/**
	 * @param container
	 * 		Container to unregister.
	 */
	public void unregisterContainer(@Nonnull ConfigContainer container) {
		ConfigContainer removed = containers.remove(container.getId());

		// Alert listeners when content removed
		if (removed != null) {
			Unchecked.checkedForEach(listeners, listener -> listener.onUnregister(container),
					(listener, t) -> logger.error("Exception thrown when unregistering container '{}'", container.getId(), t));
		}
	}

	/**
	 * @param listener
	 * 		Listener to add.
	 */
	public void addManagedConfigListener(@Nonnull ManagedConfigListener listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Listener to remove.
	 *
	 * @return {@code true} when the listener was removed.
	 * {@code false} when it wasn't added in the first place.
	 */
	public boolean removeManagedConfigListener(@Nonnull ManagedConfigListener listener) {
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
