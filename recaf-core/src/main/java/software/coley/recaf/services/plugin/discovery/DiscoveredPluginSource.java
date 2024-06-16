package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.io.ByteSource;

/**
 * Provider for a {@link ByteSource} to point to a newly discovered plugin file.
 *
 * @author xDark
 */
public interface DiscoveredPluginSource {
	/**
	 * @return Source to load from the plugin file.
	 */
	@Nonnull
	ByteSource source();
}
