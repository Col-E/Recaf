package software.coley.recaf.services.config;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.cdi.InitializationEvent;
import software.coley.recaf.config.ConfigCollectionValue;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.config.RestoreAwareConfigContainer;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.ServiceConfig;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.json.GsonProvider;
import software.coley.recaf.util.TestEnvironment;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Tracker for all {@link ConfigContainer} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
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
	}

	private void init(@Observes InitializationEvent event) {
		load();
	}

	/**
	 * Save all current config values to disk.
	 */
	@PreDestroy
	public void save() {
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
			Path containerPath = getContainerPath(container);
			try (JsonWriter writer = gson.newJsonWriter(Files.newBufferedWriter(containerPath))) {
				gson.toJson(json, writer);
			} catch (IOException e) {
				logger.error("Failed to save config container: {}", key, e);
			}
		}
	}

	/**
	 * Load all current config values from disk.
	 */
	@SuppressWarnings({"raw", "rawtypes"})
	public void load() {
		Gson gson = gsonProvider.getGson();
		for (ConfigContainer container : containers.values()) {
			String key = container.getGroupAndId();
			Path containerPath = getContainerPath(container);
			if (!Files.exists(containerPath)) {
				if (container instanceof RestoreAwareConfigContainer listener)
					listener.onNoRestore();
				continue;
			}

			// Sanity check the contents are valid JSON.
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
					loadValue(gson, container, value, json.get(id));
				} catch (IllegalArgumentException e) {
					logger.error("Could not find adapter for type: {}", value.getType(), e);
				} catch (Exception e) {
					logger.error("Failed to load config value: {}.{}", key, id, e);
				}
			}

			// Notify the container it has restored its config values from storage.
			if (container instanceof RestoreAwareConfigContainer listener)
				listener.onRestore();
		}
	}

	/**
	 * Export all currently managed config files to the given ZIP file.
	 *
	 * @param zipPath
	 * 		Path to write the profile ZIP to.
	 *
	 * @throws IOException
	 * 		When the ZIP cannot be written.
	 */
	public void exportProfile(@Nonnull Path zipPath) throws IOException {
		// Ensure all current values are saved to disk before exporting.
		save();

		// Ensure the parent directory exists before writing the ZIP file.
		Path parent = zipPath.getParent();
		if (parent != null)
			Files.createDirectories(parent);

		// Write all config files to the ZIP, using the container group and id as the file name.
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
			for (ConfigContainer container : containers.values()) {
				Path containerPath = getContainerPath(container);
				if (!Files.isRegularFile(containerPath))
					continue;

				zip.putNextEntry(new ZipEntry(getContainerFileName(container)));
				Files.copy(containerPath, zip);
				zip.closeEntry();
			}
		}
	}

	/**
	 * Import all recognized config files from the given ZIP file and reload current values.
	 *
	 * @param zipPath
	 * 		Path to read the profile ZIP from.
	 *
	 * @throws IOException
	 * 		When the ZIP cannot be read or does not contain valid config files.
	 */
	public void importProfile(@Nonnull Path zipPath) throws IOException {
		// Map config files to their respective containers.
		Map<String, ConfigContainer> fileNameToContainer = new TreeMap<>();
		for (ConfigContainer container : containers.values())
			fileNameToContainer.put(getContainerFileName(container), container);

		// Read zip contents and import any recognized config files.
		Gson gson = gsonProvider.getGson();
		Map<Path, byte[]> validatedConfigContents = new HashMap<>();
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				String name = entry.getName();
				try {
					// Skip directories and invalid entry names.
					if (entry.isDirectory())
						continue;
					if (isInvalidProfileEntryName(name))
						throw new IOException("Config profile contains non-root entry: " + name);

					// Skip unrecognized config names.
					ConfigContainer container = fileNameToContainer.get(name);
					if (container == null)
						continue;

					// Sanity check that the file contents are valid JSON before queuing them for import.
					byte[] content = zip.readAllBytes();
					try (JsonReader reader = gson.newJsonReader(new InputStreamReader(new ByteArrayInputStream(content), UTF_8))) {
						Objects.requireNonNull(gson.fromJson(reader, JsonObject.class));
					} catch (Exception ex) {
						throw new IOException("Config profile contains malformed JSON: " + name, ex);
					}
					validatedConfigContents.put(getContainerPath(container), content);
				} finally {
					zip.closeEntry();
				}
			}
		}

		if (validatedConfigContents.isEmpty())
			throw new IOException("Config profile does not contain any recognized config files");

		// Extract the validated config files to their respective paths, overwriting any existing files.
		Files.createDirectories(fileConfig.getConfigDirectory());
		for (Map.Entry<Path, byte[]> entry : validatedConfigContents.entrySet())
			Files.write(entry.getKey(), entry.getValue());

		// Regular input now. Updated config files are in-place.
		load();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void loadValue(Gson gson, ConfigContainer container, ConfigValue value, JsonElement element) {
		// Validate that the value type matches the element type before attempting to load it.
		// This can happen if the config file is manually edited improperly, or if the config value type was changed between saves.
		Class<?> valueType = value.getType();
		if (element.isJsonPrimitive()) {
			JsonPrimitive primitive = element.getAsJsonPrimitive();
			if ((valueType == String.class && !primitive.isString())
					|| (Number.class.isAssignableFrom(valueType) && !primitive.isNumber())
					|| (int.class == valueType && !primitive.isNumber())
					|| (long.class == valueType && !primitive.isNumber())
					|| (float.class == valueType && !primitive.isNumber())
					|| (double.class == valueType && !primitive.isNumber())
					|| (valueType == Character.class && !primitive.isString())
					|| (valueType == Boolean.class && !primitive.isBoolean())) {
				logger.warn("Type mismatch for config value '{}.{}'. Expected {}, but found {}. Skipping value.",
						container.getGroupAndId(), value.getId(), valueType.getSimpleName(), primitive);
				return;
			}
		} else if (element.isJsonArray() && !valueType.isArray() && !Collection.class.isAssignableFrom(valueType)) {
			logger.warn("Type mismatch for config value '{}.{}'. Expected {}, but found array. Skipping value.",
					container.getGroupAndId(), value.getId(), valueType.getSimpleName());
			return;
		} else if (element.isJsonObject() && valueType.isPrimitive()) {
			logger.warn("Type mismatch for config value '{}.{}'. Expected {}, but found object. Skipping value.",
					container.getGroupAndId(), value.getId(), valueType.getSimpleName());
			return;
		}

		// Now that we know the types are compatible, attempt to load the value.
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

	@Nonnull
	private Path getContainerPath(@Nonnull ConfigContainer container) {
		return fileConfig.getConfigDirectory().resolve(getContainerFileName(container));
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
		PrioritySortable.add(listeners, listener);
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
	private static String getContainerFileName(@Nonnull ConfigContainer container) {
		return container.getGroupAndId() + ".json";
	}

	private static boolean isInvalidProfileEntryName(@Nonnull String name) {
		return name.isBlank() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..");
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
