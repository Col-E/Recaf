package software.coley.recaf.plugin;

import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

/**
 * Plugin source.
 *
 * @author xDark
 */
public interface PluginSource {

	/**
	 * @param name Resource name.
	 * @return Resource content or {@code null}, if not found.
	 */
	@Nullable
	ByteSource findResource(String name);
}