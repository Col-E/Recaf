package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

import java.util.Collection;
import java.util.List;

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

	/**
	 * @return Collection of class names in the plugin source.
	 */
	@Nonnull
	default Collection<String> classNames() {
		return List.of();
	}
}