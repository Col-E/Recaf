package software.coley.recaf.plugin;

import jakarta.annotation.Nonnull;

/**
 * Object containing necessary information about a plugin.
 *
 * @author xDark
 * @see PluginInformation Annotation containing this information applied to {@link Plugin} implementations.
 */
public final class PluginInfo {
	private final String name;
	private final String version;
	private final String author;
	private final String description;

	/**
	 * @param name
	 * 		Name of the plugin.
	 * @param version
	 * 		Plugin version.
	 * @param author
	 * 		Author of the plugin.
	 * @param description
	 * 		Plugin description.
	 */
	public PluginInfo(@Nonnull String name, @Nonnull String version,
					  @Nonnull String author, @Nonnull String description) {
		this.name = name;
		this.version = version;
		this.author = author;
		this.description = description;
	}

	/**
	 * @return Plugin name.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Plugin version.
	 */
	@Nonnull
	public String getVersion() {
		return version;
	}

	/**
	 * @return Author of the plugin.
	 */
	@Nonnull
	public String getAuthor() {
		return author;
	}

	/**
	 * @return Plugin description.
	 */
	@Nonnull
	public String getDescription() {
		return description;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		PluginInfo info = (PluginInfo) o;

		if (!name.equals(info.name)) return false;
		if (!version.equals(info.version)) return false;
		if (!author.equals(info.author)) return false;
		return description.equals(info.description);
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + version.hashCode();
		result = 31 * result + author.hashCode();
		result = 31 * result + description.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "PluginInfo{" +
				"name='" + name + '\'' +
				", version='" + version + '\'' +
				", author='" + author + '\'' +
				", description='" + description + '\'' +
				'}';
	}
}
