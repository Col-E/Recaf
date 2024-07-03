package software.coley.recaf.plugin;

/**
 * Base interface that all plugins must inherit from.
 * Classes that implement this type should also be annotated with {@link PluginInformation}.
 *
 * @author xDark
 */
public interface Plugin {
	/**
	 * Called when plugin is being enabled.
	 */
	void onEnable();

	/**
	 * Called when plugin is being disabled.
	 */
	void onDisable();
}
