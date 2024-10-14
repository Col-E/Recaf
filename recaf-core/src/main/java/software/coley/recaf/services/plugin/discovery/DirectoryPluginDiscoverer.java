package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.plugin.PluginException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A plugin discoverer that searches a directory for plugin sources.
 * Subdirectories are not searched.
 *
 * @author xDark
 */
public final class DirectoryPluginDiscoverer extends PathPluginDiscoverer {
	private final Path directory;

	/**
	 * @param directory Directory to iterate over for plugins.
	 */
	public DirectoryPluginDiscoverer(@Nonnull Path directory) {
		this.directory = directory;
	}

	@Nonnull
	@Override
	protected Stream<Path> stream() throws PluginException {
		try {
			return Files.find(directory, 1,
					(path, attributes) -> attributes.isRegularFile() && path.toString().toLowerCase().endsWith(".jar"));
		} catch (IOException ex) {
			throw new PluginException(ex);
		}
	}
}
