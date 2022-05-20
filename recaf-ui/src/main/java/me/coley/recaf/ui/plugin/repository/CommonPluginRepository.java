package me.coley.recaf.ui.plugin.repository;

import me.coley.recaf.ui.plugin.item.MarketplacePluginItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xtherk
 */
public class CommonPluginRepository implements PluginRepository {

    private static final List<PluginRepository> repositories = new ArrayList<>();

    static {
        repositories.add(new OfficialPluginRepository());
    }


    @Override
    public List<MarketplacePluginItem> pluginItems() {
        return repositories.stream().flatMap(it -> it.pluginItems().stream()).collect(Collectors.toList());
    }
}
