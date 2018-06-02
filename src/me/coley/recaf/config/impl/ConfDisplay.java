package me.coley.recaf.config.impl;

import me.coley.logging.Level;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;

/**
 * Options for ASM reading and writing.
 * 
 * @author Matt
 */
public class ConfDisplay extends Config {
	/**
	 * Simplify descriptor displays.
	 */
	@Conf(category = "display", key = "simplfy")
	public boolean simplify = true;
	/**
	 * Show in-line hints
	 */
	@Conf(category = "display", key = "jumphelp")
	public boolean jumpHelp = true;
	/**
	 * Editor windows become topmost.
	 */
	@Conf(category = "display", key = "topmost")
	public boolean topmost = true;
	/**
	 * Editor windows become topmost.
	 */
	@Conf(category = "display", key = "showsearchmethodtype")
	public boolean showSearchMethodType;
	/**
	 * UI language.
	 */
	@Conf(category = "display", key = "language")
	public String language = "en";
	/**
	 * Logging level to display in UI.
	 */
	@Conf(category = "display", key = "loglevel")
	public Level loglevel = Level.INFO;
	
	/**
	 * Show button bar in main window. Disable and relaunch for more vertical space.
	 */
	@Conf(category = "display", key = "buttonbar")
	public boolean toolbar = true;

	public ConfDisplay() {
		super("rc_display");
		load();
	}

	/**
	 * Static getter.
	 * 
	 * @return ConfDisplay instance.
	 */
	public static ConfDisplay instance() {
		return ConfDisplay.instance(ConfDisplay.class);
	}

}
