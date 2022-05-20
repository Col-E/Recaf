package me.coley.recaf.ui.plugin.repository;

import com.google.gson.*;
import me.coley.recaf.ui.plugin.item.MarketplacePluginItem;
import me.coley.recaf.ui.util.HttpUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Default plugin repository
 *
 * @author xtherk
 */
public class OfficialPluginRepository implements PluginRepository {

    private static final Logger logger = Logging.get(OfficialPluginRepository.class);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Now it's all 2.x version of plugin
     */
    private static final String REPOS = "https://api.github.com/orgs/Recaf-Plugins/repos";

    private static final String RELEASE_TEMPLATE = "https://api.github.com/repos/Recaf-Plugins/%s/releases/latest";

    /**
     *
     * @return Return to the plugin in the Recaf-Plugin repo
     */
    @Override
    public List<MarketplacePluginItem> pluginItems() {
        List<MarketplacePluginItem> pluginItems = new ArrayList<>();
        try {
            HttpResponse<String> httpResponse = HttpUtil.get(REPOS);
            if (HTTP_OK == httpResponse.statusCode()) {
                JsonArray pluginJsonArray = gson.fromJson(httpResponse.body(), JsonArray.class);
                // todo: Can be optimized into async execution
                for (JsonElement element : pluginJsonArray) {
                    JsonObject po = element.getAsJsonObject();
                    String name = getName(po);
                    String description = getDescription(po);
                    HttpResponse<String> releaseResponse = HttpUtil.get(String.format(RELEASE_TEMPLATE, name));
                    JsonObject releases = gson.fromJson(releaseResponse.body(), JsonObject.class);
                    String version = getVersion(releases);
                    String author = getAuthor(releases);
                    // todo: If the download link does not exist, is there still the need for display ?
                    URI uri = getDownloadUrl(releases).map(URI::create).orElse(null);
                    pluginItems.add(new MarketplacePluginItem(uri, name, version, author, description));
                }
            } else {
                logger.error("Bad request with code {} and message for [{}]", httpResponse.statusCode(), httpResponse.body());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Can't finish download", e);
        }
        return pluginItems;
    }

    /**
     *
     * @return name of the plugin
     */
    private String getName(JsonObject po) {
        return po.get("name").getAsString();
    }

    /**
     *
     * @return description of the plugin
     */
    private String getDescription(JsonObject po) {
        JsonElement element = po.get("description");
        return isNull(element) ? "" : element.getAsString();
    }

    /**
     *
     * @return version of the plugin
     */
    private String getVersion(JsonObject po) {
        JsonElement element = po.get("tag_name");
        return isNull(element) ? "Unknown" : element.getAsString();
    }

    /**
     *
     * @return author of the plugin
     */
    private String getAuthor(JsonObject po) {
        JsonElement element = po.get("author");
        return isNull(element) ? "Unknown" : element.getAsJsonObject().get("login").getAsString();
    }

    /**
     *
     * @return download url of the plugin
     */
    private Optional<String> getDownloadUrl(JsonObject po) {
        JsonElement element = po.get("assets");
        if (isNull(element)) {
            return Optional.empty();
        }
        JsonArray assets = element.getAsJsonArray();
        if (assets.isEmpty()) {
            return Optional.empty();
        }
        // Only one asset should exist in the release assets
        return Optional.of(assets.get(0).getAsJsonObject().get("browser_download_url").getAsString());
    }

    /**
     * There may be an empty warehouse, so I have to conduct some inspections
     */
    private boolean isNull(JsonElement element) {
        return null == element || element.isJsonNull();
    }

}
