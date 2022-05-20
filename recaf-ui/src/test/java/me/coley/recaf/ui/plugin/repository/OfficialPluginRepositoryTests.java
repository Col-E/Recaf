package me.coley.recaf.ui.plugin.repository;

import me.coley.recaf.ui.plugin.item.MarketplacePluginItem;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OfficialPluginRepositoryTests {

    @Test
    public void tests() {
        OfficialPluginRepository repository = new OfficialPluginRepository();
        List<MarketplacePluginItem> list = repository.pluginItems();
        for (MarketplacePluginItem item : list) {
            System.out.printf("plugin: %s-%s, description: %s%n", item.getName(), item.getAuthor(), item.getDescription());
        }
    }
}
