package me.coley.recaf.plugin;

import javafx.application.Application.Parameters;

/**
 * Interface applied to plugins that have access to the launch arguments.
 * 
 * @author Matt
 */
public interface Launchable {
	/**
	 * Called before the paramaters are parsed internally by Recaf.
	 */
	void preparse(Parameters params);

	/**
	 * Called after the paramaters are parsed internally by Recaf.
	 */
	void postparse(Parameters params);
}
