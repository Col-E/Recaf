package me.coley.recaf.ui.plugin.item;

import dev.xdark.recaf.plugin.RecafRootPlugin;
import dev.xdark.recaf.plugin.repository.PluginRepoItem;
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
public class InstalledPluginItem extends PluginRepoItem {
	private static final Logger logger = Logging.get(InstalledPluginItem.class);

	/**
	 * @param uri
	 * 		The path where the plugin is located
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
	 * Disable the plugin
	 */
	public void disable() {
		RecafRootPlugin.getInstance()
				.getPlugin(name)
				.ifPresent(pc -> {
					pc.getLoader().disablePlugin(pc);
					Configs.plugin().setEnabled(pc.getInformation(), false);
				});
	}

	/**
	 * Enable the plugin.
	 */
	public void enable() {
		RecafRootPlugin.getInstance()
				.getPlugin(name)
				.ifPresent(pc -> {
					pc.getLoader().enablePlugin(pc);
					Configs.plugin().setEnabled(pc.getInformation(), true);
				});
	}

	/**
	 * Uninstall the plugin.
	 */
	public void uninstall() {
		RecafRootPlugin.getInstance().unloadPlugin(name);
		Path path = Paths.get(uri.getPath());
		try {
			Files.deleteIfExists(path);
			Configs.plugin().enabledState.remove(name);
		} catch (IOException ex) {
			logger.error("Delete plugin failed", ex);
		}
	}
}
