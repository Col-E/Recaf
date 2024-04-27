package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.PluginException;

import java.util.List;

/**
 * Plugin discoverer.
 *
 * @author xDark
 */
public interface PluginDiscoverer {

	/**
	 * @return A list of discovered plugins.
	 * @throws PluginException If discovery fails.
	 */
	@Nonnull
	List<DiscoveredPlugin> findAll() throws PluginException;
}
