package me.coley.recaf.ui.plugin.item;

import dev.xdark.recaf.plugin.PluginInformation;
import dev.xdark.recaf.plugin.RecafPluginManager;
import dev.xdark.recaf.plugin.repository.PluginRepositoryItem;
import me.coley.recaf.config.Configs;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Local plugin information, used for UI display
 *
 * @author xtherk
 */
public class InstalledPluginItem extends PluginRepositoryItem {
	private static final Logger logger = Logging.get(InstalledPluginItem.class);

	/**
	 * @param uri
	 * 		The path where the plugin is located.
	 * @param info
	 * 		Plugin information wrapper.
	 */
	public InstalledPluginItem(URI uri, PluginInformation info) {
		this(uri, info.getName(), info.getVersion(), info.getAuthor(), info.getDescription());
	}

	/**
	 * @param uri
	 * 		The path where the plugin is located.
	 * @param name
	 * 		name of the plugin.
	 * @param version
	 * 		plugin version.
	 * @param author
	 * 		author of the plugin.
	 * @param description
	 * 		plugin description.
	 */
	public InstalledPluginItem(URI uri, String name, String version, String author, String description) {
		super(uri, name, version, author, description);
	}

	/**
	 * @return Plugin enabled status.
	 */
	public boolean isEnabled() {
		return RecafPluginManager.getInstance()
				.optionalGetPlugin(name)
				.filter(pc -> Configs.plugin().isEnabled(pc.getInformation()))
				.isPresent();
	}

	/**
	 * Disable the plugin.
	 */
	public void disable() {
		RecafPluginManager.getInstance()
				.optionalGetPlugin(name)
				.ifPresent(pc -> {
					pc.getLoader().disablePlugin(pc);
					Configs.plugin().setEnabled(pc.getInformation(), false);
				});
	}

	/**
	 * Enable the plugin.
	 */
	public void enable() {
		RecafPluginManager.getInstance()
				.optionalGetPlugin(name)
				.ifPresent(pc -> {
					pc.getLoader().enablePlugin(pc);
					Configs.plugin().setEnabled(pc.getInformation(), true);
				});
	}

	/**
	 * Uninstall the plugin.
	 */
	public void uninstall() {
		RecafPluginManager.getInstance().unloadPlugin(name);
		Path path = Paths.get(uri);
		try {
			Configs.plugin().enabledState.remove(name);
			Files.deleteIfExists(path);
			logger.info("Uninstalled plugin '{}'", name);
		} catch (IOException ex) {
			logger.error("Failed to delete plugin '{}'", name, ex);
		}
	}
}
