package me.coley.recaf;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

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
		// Setup tinylog instance
		Configurator.defaultConfig()
				.formatPattern("{level}-{date}: {message|indent=4}")
				.writingThread(true)
				.addWriter(new FileWriter("rclog.txt"))
				.activate();
		// Disable Slf4j (Configured by Aether depenency)
		try {
			Field target = LoggerFactory.getLogger("ROOT").getClass().getDeclaredField("TARGET_STREAM");
			target.setAccessible(true);
			target.set(null, new PrintStream(new ByteArrayOutputStream()));
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
}
