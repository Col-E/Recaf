package me.coley.recaf.plugin;

/**
 * Interface applied to plugins that have access to the launch arguments.
 * 
 * @author Matt
 */
public interface Launchable {
	/**
	 * Called before the paramaters are parsed internally by Recaf.
	 */
	void preparse(String[] args);

	/**
	 * Called after the paramaters are parsed internally by Recaf.
	 */
	void postparse(String[] args);
}
