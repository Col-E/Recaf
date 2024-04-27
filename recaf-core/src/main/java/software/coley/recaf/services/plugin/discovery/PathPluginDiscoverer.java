package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.PluginException;
import software.coley.recaf.util.io.ByteSources;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public abstract class PathPluginDiscoverer implements PluginDiscoverer {

	@Nonnull
	@Override
	public final List<DiscoveredPlugin> findAll() throws PluginException {
		try (Stream<Path> s = stream()) {
			return s
					.map(path -> (DiscoveredPlugin) () -> ByteSources.forPath(path))
					.toList();
		}
	}

	/**
	 * @return A stream of paths.
	 * @throws PluginException If discovery fails.
	 */
	@Nonnull
	protected abstract Stream<Path> stream() throws PluginException;
}
