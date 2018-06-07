package me.coley.recaf.plugins;

import javafx.application.Application.Parameters;

/**
 * Interface applied to plugins that can create custom launch arguments.
 * 
 * @author Matt
 */
public interface Launchable {
	/**
	 * Called twice, once before the params are parsed internally, and once
	 * after they are parsed internally.
	 * 
	 * @param params
	 *            Launch arguments.
	 * @param argumentsParsed
	 *            Indication of internal parsing completion.
	 */
	void call(Parameters params, boolean argumentsParsed);
}
