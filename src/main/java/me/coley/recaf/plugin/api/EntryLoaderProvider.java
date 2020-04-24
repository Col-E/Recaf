package me.coley.recaf.plugin.api;

import me.coley.recaf.workspace.EntryLoader;

/**
 * Allow plugins to provide entry loader for archives.
 *
 * @author Matt
 */
public interface EntryLoaderProvider extends PluginBase {
	/**
	 * @return Created loader.
	 */
	EntryLoader create();
}
