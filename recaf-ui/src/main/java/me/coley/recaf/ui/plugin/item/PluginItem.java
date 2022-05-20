package me.coley.recaf.ui.plugin.item;

import java.net.URI;

/**
 * Basic information
 *
 * @author xtherk
 */
public abstract class PluginItem {

    protected final URI uri;
    protected final String name;
    protected final String version;
    protected final String author;
    protected final String description;

    /**
     * @param uri         The path where the plugin is located
     * @param name        name of the plugin.
     * @param version     plugin version.
     * @param author      author of the plugin.
     * @param description plugin description.
     */
    public PluginItem(URI uri, String name, String version, String author, String description) {
        this.uri = uri;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
    }

    public URI getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getDescription() {
        return description;
    }

    /**
     *
     * @return Whether the plugin has been installed
     */
    public abstract boolean isInstalled();

}
