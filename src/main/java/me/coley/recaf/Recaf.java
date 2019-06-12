package me.coley.recaf;

import me.coley.event.Bus;
import me.coley.recaf.ui.FxWindow;
import me.coley.recaf.util.Dependencies;

public class Recaf {
	public static final String VERSION = "1.15.1";
	public static String[] args;

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
