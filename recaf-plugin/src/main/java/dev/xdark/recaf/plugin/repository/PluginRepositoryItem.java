package dev.xdark.recaf.plugin.repository;

import java.net.URI;

/**
 * Basic information about a remote plugin item contained in a {@link PluginRepository}.
 *
 * @author xtherk
 */
public class PluginRepositoryItem {
	protected final URI uri;
	protected final String name;
	protected final String version;
	protected final String author;
	protected final String description;

	/**
	 * @param uri
	 * 		The path where the plugin is located.
	 * @param name
	 * 		Name of the plugin.
	 * @param version
	 * 		Plugin version.
	 * @param author
	 * 		Author of the plugin.
	 * @param description
	 * 		Plugin description.
	 */
	public PluginRepositoryItem(URI uri, String name, String version, String author, String description) {
		this.uri = uri;
		this.name = name;
		this.version = version;
		this.author = author;
		this.description = description;
	}

	/**
	 * @return Path where the plugin is located.
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * @return Name of the plugin.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Plugin version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return Author of the plugin.
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @return Plugin description.
	 */
	public String getDescription() {
		return description;
	}
}
