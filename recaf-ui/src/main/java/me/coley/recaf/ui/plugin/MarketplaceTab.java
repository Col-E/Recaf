package me.coley.recaf.ui.plugin;

import dev.xdark.recaf.plugin.repository.PluginRepoItem;
import javafx.scene.control.Tab;
import me.coley.recaf.ui.plugin.item.RemotePluginItem;
import dev.xdark.recaf.plugin.repository.CommonPluginRepository;
import dev.xdark.recaf.plugin.repository.PluginRepository;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.List;

/**
 * @author xtherk
 */
public class MarketplaceTab extends Tab {

    private static final Logger logger = Logging.get(MarketplaceTab.class);

    private static final PluginRepository REPOSITORY = new CommonPluginRepository();

    public MarketplaceTab() {
        super(Lang.get("menu.plugin.marketplace"));
    }

    private List<PluginRepoItem> createPluginItems() {
        return REPOSITORY.pluginItems();
    }
}
