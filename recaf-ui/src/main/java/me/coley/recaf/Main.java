package me.coley.recaf;

import dev.xdark.recaf.plugin.RecafRootPlugin;
import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.PresentationType;
import me.coley.recaf.scripting.ScriptEngine;
import me.coley.recaf.scripting.ScriptResult;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Entry point.
 *
 * @author Matt Coley
 */
public class Main {
	private static final Logger logger = Logging.get(Main.class);

	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		setupLogging();
		// Initialization plugin
		RecafRootPlugin.initialize();
		// Read application parameters
		InitializerParameters parameters = InitializerParameters.fromArgs(args);
		new Recaf().initialize(parameters);
		// run script from parameters if found
		if (parameters.getScriptPath() != null) {
			Path scriptPath = parameters.getScriptPath().toPath();
			if (Files.isRegularFile(scriptPath)) {
				Runnable r = () -> {
					ScriptResult result = ScriptEngine.execute(scriptPath);
					if (result.wasSuccess()) {
						logger.info("Script execute result: {}", result.getResult());
					} else {
						logger.error("Script encountered error: ", result.getException());
					}
				};
				if (parameters.getPresentationType() == PresentationType.GUI) {
					// Run the script on a delay, giving time to for the GUI to populate
					ThreadUtil.runDelayed(500, r);
				} else {
					// Run the script on the main thread
					r.run();
				}
			} else {
				logger.error("No script found: {}", scriptPath);
			}
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
