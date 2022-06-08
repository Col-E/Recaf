package me.coley.recaf.ui.plugin;

import dev.xdark.recaf.plugin.Plugin;
import dev.xdark.recaf.plugin.PluginContainer;
import dev.xdark.recaf.plugin.PluginInformation;
import dev.xdark.recaf.plugin.RecafPlugin;
import javafx.scene.control.Tab;
import me.coley.recaf.ui.plugin.item.InstalledPluginItem;
import me.coley.recaf.ui.util.Lang;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xtherk
 */
public class InstalledTab extends Tab {

    private static final Logger logger = Logging.get(InstalledTab.class);

    public InstalledTab() {
        super(Lang.get("menu.plugin.installed"));
    }

    /**
     * Create PluginItem from loaded plugins
     * @return Loaded plugins
     */
    private List<InstalledPluginItem> createPluginItems() {
        return RecafPlugin.getInstance().getPluginContainerPathMap().entrySet().stream()
                .map(entry -> {
                    PluginContainer<? extends Plugin> value = entry.getKey();
                    PluginInformation info = value.getInformation();
                    URI uri = entry.getValue().toUri();
                    return new InstalledPluginItem(uri, info.getName(), info.getVersion(),
                            info.getAuthor(), info.getDescription());
                }).collect(Collectors.toList());
    }

}
