package me.coley.recaf;

import picocli.CommandLine.Option;

import java.awt.Dimension;
import java.io.File;
import java.util.concurrent.Callable;

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

	@Override
	public Void call() throws Exception {
		if (initialFile != null) Recaf.INSTANCE.logging.info("CLI file: " + initialFile);
		if (initialClass != null) Recaf.INSTANCE.logging.info("CLI class: " + initialClass);
		if (uiWidth != 1200) Recaf.INSTANCE.logging.info("CLI ui-width: " + uiWidth);
		if (uiHeight != 730) Recaf.INSTANCE.logging.info("CLI ui-height: " + uiHeight);
		return null;
	}

	public Dimension getSize() {
		return new Dimension(uiWidth, uiHeight);
	}
}
