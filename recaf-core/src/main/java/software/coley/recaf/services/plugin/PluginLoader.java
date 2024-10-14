package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.services.plugin.PreparedPlugin;
import software.coley.recaf.util.io.ByteSource;

/**
 * The plugin loader is responsible for loading plugins from different sources.
 *
 * @author xDark
 */
public interface PluginLoader {

	/**
	 * @param source Content to read from.
	 * @return Prepared plugin or {@code null}, if source is not supported.
	 * @throws PluginException When plugin cannot be prepared.
	 */
	@Nullable
	PreparedPlugin prepare(@Nonnull ByteSource source)
			throws PluginException;
}
