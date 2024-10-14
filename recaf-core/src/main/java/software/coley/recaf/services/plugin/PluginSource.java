package software.coley.recaf.services.plugin;

import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

/**
 * A functional mapping of internal paths <i>(Like paths in a ZIP file)</i> to the contents of the plugin.
 *
 * @author xDark
 */
public interface PluginSource {

	/**
	 * @param name
	 * 		Resource path name.
	 *
	 * @return Resource content or {@code null}, if not found.
	 */
	@Nullable
	ByteSource findResource(String name);
}