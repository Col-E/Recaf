package software.coley.recaf.services.plugin.discovery;

import jakarta.annotation.Nonnull;
import software.coley.recaf.util.io.ByteSource;

/**
 * Discovered plugin.
 */
public interface DiscoveredPlugin {

	/**
	 * @return Plugin file.
	 */
	@Nonnull
	ByteSource source();
}
