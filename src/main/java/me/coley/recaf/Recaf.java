package me.coley.recaf;

import me.coley.event.Bus;
import me.coley.recaf.ui.FxWindow;
import me.coley.recaf.util.Dependencies;
import me.coley.recaf.util.LaunchParams;

public class Recaf {
	/**
	 * Recaf version.
	 */
	public static final String VERSION = "1.15.6";
	/**
	 * Initial launch arguments.
	 */
	public static String[] args;
	/**
	 * Serialized launch arguments.
	 */
	public static LaunchParams argsSerialized;

	public static void main(String[] args) {
		Recaf.args = args;
		if (!Dependencies.check()) {
			return;
		}
		// start main window
		Bus.subscribe(new InitListener(args));
		FxWindow.init(args);
	}
}
