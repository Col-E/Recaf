package me.coley.recaf.plugin.api;

import me.coley.recaf.workspace.EntryLoader;

/**
 * Allow plugins to provide entry loader for archives.
 *
 * @author Matt
 */
public interface EntryLoaderProviderPlugin extends BasePlugin {
	/**
	 * @return Created loader.
	 */
	EntryLoader create();
}
