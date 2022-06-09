package dev.xdark.recaf.plugin.repository;

import java.util.List;

/**
 * Remote host for plugins.
 *
 * @author xtherk
 */
public interface PluginRepository {
	/**
	 * @return Plugin information in the remote repo
	 */
	List<PluginRepositoryItem> pluginItems();
}
