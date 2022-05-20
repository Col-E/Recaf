package me.coley.recaf.ui.plugin;

import javafx.scene.control.Tab;
import me.coley.recaf.ui.plugin.item.MarketplacePluginItem;
import me.coley.recaf.ui.plugin.repository.CommonPluginRepository;
import me.coley.recaf.ui.plugin.repository.PluginRepository;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author xtherk
 */
public class MarketplaceTab extends Tab {

    private final Logger logger = Logging.get(this.getClass());

    private static final PluginRepository REPOSITORY = new CommonPluginRepository();

    public MarketplaceTab() {
        super("Marketplace");
    }

    private List<MarketplacePluginItem> createPluginItems() {
        return REPOSITORY.pluginItems();
    }
}
