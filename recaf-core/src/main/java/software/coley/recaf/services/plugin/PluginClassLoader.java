package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.util.io.ByteSource;

/**
 * Plugin class loader interface.
 */
public interface PluginClassLoader {

	/**
	 * @param name
	 * 		Resource path.
	 *
	 * @return Resource source or {@code null} if not found.
	 */
	@Nullable
	ByteSource lookupResource(@Nonnull String name);

	/**
	 * @param name
	 * 		Class name.
	 *
	 * @return Class.
	 *
	 * @throws ClassNotFoundException
	 * 		If class was not found.
	 */
	@Nonnull
	Class<?> lookupClass(@Nonnull String name) throws ClassNotFoundException;
}
