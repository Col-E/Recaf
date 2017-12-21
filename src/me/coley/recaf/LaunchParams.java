package me.coley.recaf;

import picocli.CommandLine.Option;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.Callable;

import me.coley.logging.Level;

/**
 * Container for launch arguments.
 * 
 * @author Matt
 */
public class LaunchParams implements Callable<Void> {
	@Option(names = { "-i", "--input" }, description = "Jar file to read.")
	public File initialFile;
	@Option(names = { "-c", "--class" }, description = "Initial class to open.")
	public String initialClass;
	@Option(names = { "-w", "--width" }, description = "GUI width.")
	public int uiWidth = 1200;
	@Option(names = { "-h", "--height" }, description = "GUI height.")
	public int uiHeight = 730;
	@Option(names = { "-lc", "--logconsole" }, description = "Logging detail level displayed on console.")
	public int logConsole = Level.INFO.ordinal();
	@Option(names = { "-lf", "--logfile" }, description = "Logging detail level saved to log file.")
	public int logFile = Level.FINE.ordinal();

	@Override
	public Void call() throws Exception {
		if (initialFile != null) Recaf.INSTANCE.logging.info("CLI file: " + initialFile);
		if (initialClass != null) Recaf.INSTANCE.logging.info("CLI class: " + initialClass);
		if (uiWidth != 1200) Recaf.INSTANCE.logging.info("CLI ui-width: " + uiWidth);
		if (uiHeight != 730) Recaf.INSTANCE.logging.info("CLI ui-height: " + uiHeight);
		if (logConsole != 1) Recaf.INSTANCE.logging.info("CLI cl-log-level: " + Level.values()[logConsole].name());
		if (logFile != 0) Recaf.INSTANCE.logging.info("CLI file-log-level: " + Level.values()[logFile].name());
		return null;
	}

	public Dimension getSize() {
		return new Dimension(uiWidth, uiHeight);
	}
}
