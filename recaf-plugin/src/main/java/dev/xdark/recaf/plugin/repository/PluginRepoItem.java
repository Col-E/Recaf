package dev.xdark.recaf.plugin.repository;

import java.net.URI;

/**
 * Basic information about a remote plugin item contained in a {@link PluginRepository}.
 *
 * @author xtherk
 */
public class PluginRepoItem {
	protected final URI uri;
	protected final String name;
	protected final String version;
	protected final String author;
	protected final String description;

	/**
	 * @param uri
	 * 		The path where the plugin is located
	 * @param name
	 * 		name of the plugin.
	 * @param version
	 * 		plugin version.
	 * @param author
	 * 		author of the plugin.
	 * @param description
	 * 		plugin description.
	 */
	public PluginRepoItem(URI uri, String name, String version, String author, String description) {
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
}
