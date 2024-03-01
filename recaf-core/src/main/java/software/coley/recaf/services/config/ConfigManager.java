package software.coley.recaf.services.config;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.ConfigCollectionValue;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.util.TestEnvironment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Tracker for all {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class ConfigManager implements Service {
	public static final String SERVICE_ID = "config-manager";
	private static final Logger logger = Logging.get(ConfigManager.class);
	private static final Gson gson = createGson();
	private final Map<String, ConfigContainer> containers = new TreeMap<>();
	private final List<ManagedConfigListener> listeners = new ArrayList<>();
	private final ConfigManagerConfig config;
	private final RecafDirectoriesConfig fileConfig;
	private boolean triedSaving = false;

	@Inject
	public ConfigManager(@Nonnull ConfigManagerConfig config, @Nonnull RecafDirectoriesConfig fileConfig,
						 @Nonnull Instance<ConfigContainer> containers) {
		this.config = config;
		this.fileConfig = fileConfig;
		for (ConfigContainer container : containers)
			registerContainer(container);

		this.load();
	}

	@PreDestroy
	private void save() {
		if(TestEnvironment.isTestEnv())
			return;

		for (ConfigContainer value : containers.values()) {
			String key = value.getGroupAndId();
			Path containerPath = fileConfig.getConfigDirectory().resolve(key + ".json");

			JsonObject json = new JsonObject();
			for (ConfigValue<?> configValue : value.getValues().values()) {
				try {
					json.add(configValue.getId(), gson.toJsonTree(configValue.getValue()));
				} catch (IllegalArgumentException e) {
					logger.error("Could not find adapter for type: {}", configValue.getType(), e);
				} catch (Exception e) {
					logger.error("Failed to save config value: {}", configValue.getId(), e);
				}
			}

			try {
				BufferedWriter writer = Files.newBufferedWriter(containerPath);
				gson.toJson(json, writer);
				writer.close();
			} catch (IOException e) {
				logger.error("Failed to save config container: {}", key, e);
			}
		}
	}

	@SuppressWarnings({"raw", "rawtypes"})
    private void load() {
		if (TestEnvironment.isTestEnv())
			return;

		for (ConfigContainer container : containers.values()) {
			String key = container.getGroupAndId();
			Path containerPath = fileConfig.getConfigDirectory().resolve(key + ".json");
			if (!Files.exists(containerPath)) {
				if(triedSaving) {
					logger.warn("Config container not found: {}", key);
					continue;
				}

				triedSaving = true;
				save();
			}

			JsonObject json;
			try {
				JsonReader reader = new JsonReader(Files.newBufferedReader(containerPath));
				json = gson.fromJson(reader, JsonObject.class);
			} catch (IOException e) {
				logger.error("Failed to load config container: {}", key, e);
				continue;
			}

			if (json == null) {
				logger.warn("Config container is empty: {}", key);
				continue;
			}

			for (ConfigValue value : container.getValues().values()) {
				String id = value.getId();
				if (!json.has(id)) {
					logger.warn("Config value not found: {}", id);
					continue;
				}

				try {
					loadValue(value, json.get(id));
				} catch (IllegalArgumentException e) {
					logger.error("Could not find adapter for type: {}", value.getType(), e);
				} catch (Exception e) {
					logger.error("Failed to load config value: {}", id, e);
				}
			}
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void loadValue(ConfigValue value, JsonElement element) {
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

	private static Gson createGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();

		return builder.create();
	}
}
