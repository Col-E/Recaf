package dev.xdark.recaf.plugin;

import dev.xdark.recaf.plugin.repository.OfficialPluginRepository;
import dev.xdark.recaf.plugin.repository.PluginRepositoryItem;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Simple test to fetch all items from the {@link OfficialPluginRepository}.
 * The test is {@link Disabled} so that the CI service does not bombard github with API requests.
 * You can manually run the tests if you wish.
 *
 * @author xtherk
 */
@Disabled
public class OfficialPluginRepositoryTests {
    @Test
    public void test() {
        OfficialPluginRepository repository = new OfficialPluginRepository();
        List<PluginRepositoryItem> list = repository.pluginItems();
        for (PluginRepositoryItem item : list) {
            System.out.printf("plugin: %s-%s, description: %s%n", item.getName(), item.getVersion(), item.getDescription());
        }
    }
}
