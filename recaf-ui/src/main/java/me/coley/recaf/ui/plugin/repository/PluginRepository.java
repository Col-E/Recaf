package me.coley.recaf.ui.plugin.repository;

import me.coley.recaf.ui.plugin.item.MarketplacePluginItem;

import java.util.List;

/**
 * Support custom plugin source
 * @author xtherk
 */
public interface PluginRepository {

    /**
     *
     * @return Plugin information in the remote repo
     */
    List<MarketplacePluginItem> pluginItems();
}
