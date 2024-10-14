package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import software.coley.recaf.plugin.Plugin;

/**
 * Object that holds reference to a plugin and it's information.
 *
 * @param <P>
 * 		Plugin instance type.
 *
 * @author xDark
 * @see PluginInfo
 * @see PluginLoader
 */
public interface PluginContainer<P extends Plugin> {
	/**
	 * @return Plugin information.
	 */
	@Nonnull
	PluginInfo info();

	/**
	 * @return Plugin instance.
	 */
	@Nonnull
	P plugin();
}
