package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.util.io.ByteSources;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * A common base for {@link Path} backed plugin discoverers.
 *
 * @author xDark
 */
public abstract class PathPluginDiscoverer implements PluginDiscoverer {
	@Nonnull
	@Override
	public final List<DiscoveredPluginSource> findSources() throws PluginException {
		try (Stream<Path> s = stream()) {
			return s.map(path -> (DiscoveredPluginSource) () -> ByteSources.forPath(path))
					.toList();
		}
	}

	/**
	 * @return A stream of paths to treat as plugin sources.
	 *
	 * @throws PluginException
	 * 		If discovery fails.
	 */
	@Nonnull
	protected abstract Stream<Path> stream() throws PluginException;
}
