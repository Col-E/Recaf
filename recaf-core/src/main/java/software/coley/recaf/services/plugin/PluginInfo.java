package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginInformation;

import java.util.Set;

/**
 * Object containing necessary information about a plugin.
 *
 * @param id               ID of the plugin.
 * @param name             Name of the plugin.
 * @param version          Plugin version.
 * @param author           Author of the plugin.
 * @param description      Plugin description.
 * @param dependencies     Plugin dependencies.
 * @param softDependencies Plugin soft dependencies.
 * @author xDark
 * @see PluginInformation Annotation containing this information applied to {@link Plugin} implementations.
 */
public record PluginInfo(
		@Nonnull String id,
		@Nonnull String name,
		@Nonnull String version,
		@Nonnull String author,
		@Nonnull String description,
		@Nonnull Set<String> dependencies,
		@Nonnull Set<String> softDependencies
) {

	@Nonnull
	public static PluginInfo empty() {
		return new PluginInfo("", "", "", "", "", Set.of(), Set.of());
	}

	@Nonnull
	public PluginInfo withId(@Nonnull String id) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withName(@Nonnull String name) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withVersion(@Nonnull String version) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withAuthor(@Nonnull String author) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withDescription(@Nonnull String description) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withDependencies(@Nonnull Set<String> dependencies) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}

	@Nonnull
	public PluginInfo withSoftDependencies(@Nonnull Set<String> softDependencies) {
		return new PluginInfo(id, name, version, author, description, dependencies, softDependencies);
	}
}
