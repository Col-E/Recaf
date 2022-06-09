package dev.xdark.recaf.plugin;

/**
 * Object containing necessary information about a plugin.
 *
 * @author xDark
 */
public final class PluginInformation {
	private final String name;
	private final String version;
	private final String author;
	private final String description;

	/**
	 * @param name
	 * 		name of the plugin.
	 * @param version
	 * 		plugin version.
	 * @param author
	 * 		author of the plugin.
	 * @param description
	 * 		plugin description.
	 */
	public PluginInformation(String name, String version, String author, String description) {
		this.name = name;
		this.version = version;
		this.author = author;
		this.description = description;
	}

	/**
	 * @return plugin name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return plugin version.
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return author of the plugin.
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @return plugin description.
	 */
	public String getDescription() {
		return description;
	}
}
