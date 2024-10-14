package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.services.plugin.PluginException;

import java.util.List;

/**
 * Plugin source discoverer.
 *
 * @author xDark
 */
public interface PluginDiscoverer {
	/**
	 * @return A list of discovered plugin sources.
	 *
	 * @throws PluginException
	 * 		If discovery fails.
	 */
	@Nonnull
	List<DiscoveredPluginSource> findSources() throws PluginException;
}
