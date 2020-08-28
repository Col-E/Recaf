package me.coley.recaf.config;

import me.coley.recaf.plugin.PluginsManager;
import me.coley.recaf.plugin.api.ConfigurablePlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static me.coley.recaf.util.Log.*;

/**
 * Config manager.
 *
 * @author Matt
 */
public class ConfigManager {
	private static final String KEY_DISPLAY = "display";
	private static final String KEY_KEYBINDING = "keybinding";
	private static final String KEY_DECOMPILE = "decompile";
	private static final String KEY_ASSEMBLER = "assembler";
	private static final String KEY_UPDATE = "update";
	private static final String KEY_BACKEND = "backend";
	private final Map<String, Config> configs = new HashMap<>();
	private final Map<String, ConfigurablePlugin> pluginConfigs = new HashMap<>();
	private final Path configDirectory;

	/**
	 * Creates new configuration manager instance
	 *
	 * @param configDirectory
	 *     directory where configuration files are stored
	 */
	public ConfigManager(Path configDirectory) {
		this.configDirectory = Objects.requireNonNull(configDirectory, "configDirectory");
	}

	/**
	 * Setup config instances.
	 *
	 * @throws IOException
	 *     if any I/O error occurs
	 */
	public void initialize() throws IOException {
		// Setup each instance
		configs.put(KEY_DISPLAY, new ConfDisplay());
		configs.put(KEY_KEYBINDING, new ConfKeybinding());
		configs.put(KEY_DECOMPILE, new ConfDecompile());
		configs.put(KEY_ASSEMBLER, new ConfAssembler());
		configs.put(KEY_UPDATE, new ConfUpdate());
		configs.put(KEY_BACKEND, new ConfBackend());
		// Plugin instances
		PluginsManager.getInstance().ofType(ConfigurablePlugin.class)
				.forEach(plugin -> pluginConfigs.put(getPluginConfigName(plugin), plugin));
		if (!Files.isDirectory(configDirectory)) {
			Files.createDirectories(configDirectory);
		} else {
			// Load initial values
			load();
		}
	}
	// =============================================================- //

	/**
	 * @return Display configuration.
	 */
	public ConfDisplay display() {
		return (ConfDisplay) configs.get(KEY_DISPLAY);
	}

	/**
	 * @return Keybinding configuration.
	 */
	public ConfKeybinding keys() {
		return (ConfKeybinding) configs.get(KEY_KEYBINDING);
	}

	/**
	 * @return Decompiler configuration.
	 */
	public ConfDecompile decompile() {
		return (ConfDecompile) configs.get(KEY_DECOMPILE);
	}

	/**
	 * @return Assembler configuration.
	 */
	public ConfAssembler assembler() {
		return (ConfAssembler) configs.get(KEY_ASSEMBLER);
	}

	/**
	 * @return Updates configuration.
	 */
	public ConfUpdate update() {
		return (ConfUpdate) configs.get(KEY_UPDATE);
	}

	/**
	 * @return Private configuration.
	 */
	public ConfBackend backend() {
		return (ConfBackend) configs.get(KEY_BACKEND);
	}

	// ============================================================== //

	private void load() {
		trace("Loading configuration");
		for (Configurable c : getConfigs()) {
			Path path = c instanceof Config ?
					resolveConfigPath((Config) c) : resolvePluginConfigPath((ConfigurablePlugin) c);
			try {
				if(Files.exists(path))
					c.load(path);
			} catch(Throwable t) {
				error(t, "Failed to load config: {}", path);
			}
		}
	}

	/**
	 * Saves configuration files.
	 */
	public void save() {
		trace("Saving configuration");
		for (Configurable c : getConfigs()) {
			Path path = c instanceof Config ?
					resolveConfigPath((Config) c) : resolvePluginConfigPath((ConfigurablePlugin) c);
			try {
				c.save(path);
			} catch(IOException ex) {
				error(ex, "Failed to save config: {}", path);
			}
		}
	}

	private Collection<Configurable> getConfigs() {
		Set<Configurable> set = new HashSet<>();
		set.addAll(configs.values());
		set.addAll(pluginConfigs.values());
		return set;
	}

	private Path resolveConfigPath(Config config) {
		return configDirectory.resolve(config.getName() + ".json");
	}

	private Path resolvePluginConfigPath(ConfigurablePlugin plugin) {
		return configDirectory.resolve("plugins").resolve(getPluginConfigName(plugin) + ".json");
	}

	private static String getPluginConfigName(ConfigurablePlugin plugin) {
		return plugin.getName().toLowerCase().replaceAll("\\s+", "_");
	}
}
