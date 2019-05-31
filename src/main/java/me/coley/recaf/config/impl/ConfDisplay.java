package me.coley.recaf.config.impl;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import me.coley.logging.Level;
import me.coley.recaf.config.Conf;
import me.coley.recaf.config.Config;
import me.coley.recaf.util.Lang;

/**
 * Options for user-interface.
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
	public boolean topmost;
	/**
	 * UI language.
	 */
	@Conf(category = "display", key = "language")
	public String language = Lang.DEFAULT_LANGUAGE;
	/**
	 * Stylesheet group to use. 
	 */
	@Conf(category = "display", key = "style")
	public String style = "flat";
	/**
	 * Logging level to display in UI.
	 */
	@Conf(category = "display", key = "loglevel")
	public Level loglevel = Level.INFO;
	/**
	 * Show button bar in main window. Disable and relaunch for more vertical
	 * space.
	 */
	@Conf(category = "display", key = "buttonbar")
	public boolean toolbar = true;
	/**
	 * Max length of class-name shown in class tree. Helpful for obfuscated
	 * programs with long names.
	 */
	@Conf(category = "display", key = "maxlength.tree")
	public int maxLengthTree = 75;
	/**
	 * Show the source-file name next to files in the file tree.
	 */
	@Conf(category = "display", key = "treesourcename")
	public boolean treeSourceNames = false;
	/**
	 * Show the source-file name next to files in the file tree.
	 */
	@Conf(category = "display", key = "exitwarning")
	public boolean warnOnExit = true;

	public ConfDisplay() {
		super("rc_display");
		load();
	}

	@Override
	protected JsonValue convert(Class<?> type, Object value) {
		if (type.equals(Level.class)) {
			return Json.value(((Level) value).name());
		}
		return null;
	}

	@Override
	protected Object parse(Class<?> type, JsonValue value) {
		if (type.equals(Level.class)) {
			return Level.valueOf(value.asString());
		}
		return null;
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
