package me.coley.recaf;

import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.ui.Windows;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Main entry point for running Recaf with a UI.
 * Includes singleton references to core UI components.
 *
 * @author Matt Coley
 */
public class RecafUI {
	private static boolean initialized;
	private static Windows windows;
	private static Controller controller;

	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		setupLogging();
		InitializerParameters parameters = InitializerParameters.fromArgs(args);
		new Recaf().initialize(parameters);
	}

	/**
	 * @return Window manager.
	 */
	public static Windows getWindows() {
		return windows;
	}

	/**
	 * @return Controller instance.
	 */
	public static Controller getController() {
		return controller;
	}

	/**
	 * Setup UI components.
	 *
	 * @param controller
	 * 		Controller instance.
	 */
	public static void initialize(Controller controller) {
		if (!initialized) {
			RecafUI.controller = controller;
			windows = new Windows();
			windows.initialize();
			initialized = true;
		}
	}

	/**
	 * Setup file logging appender.
	 */
	private static void setupLogging() {
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Logging.addFileAppender(Directories.getBaseDirectory().resolve("log-" + date + ".txt"));
	}
}
