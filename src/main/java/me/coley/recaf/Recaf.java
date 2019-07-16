package me.coley.recaf;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import picocli.CommandLine;

/**
 * Entry point &amp; version constant.
 *
 * @author Matt
 */
public class Recaf {
	/**
	 * Recaf version.
	 */
	public static final String VERSION = "2.0.0";

	public static void main(String[] args) {
		setupLogging();
		Logger.info("Starting Recaf-{}...", VERSION);
		// Invoke
		new CommandLine(new Initializer()).execute(args);
	}

	public static void setupLogging() {
		Configurator.defaultConfig()
				.formatPattern("{level}-{date}: {message|indent=4}")
				.writingThread(true)
				.addWriter(new FileWriter("rclog.txt"))
				.activate();
	}
}
