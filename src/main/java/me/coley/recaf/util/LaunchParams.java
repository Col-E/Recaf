package me.coley.recaf.util;

import java.io.File;
import java.util.concurrent.Callable;

import me.coley.recaf.Logging;
import picocli.CommandLine.Option;

public class LaunchParams implements Callable<Void> {
	@Option(names = { "-i", "--input" }, description = "Jar file to read.")
	public File initialFile;
	@Option(names = { "-c", "--class" }, description = "Initial class to open.")
	public String initialClass;

	@Override
	public Void call() throws Exception {
		if (initialFile != null) Logging.info("CLI file: " + initialFile);
		if (initialClass != null) Logging.info("CLI class: " + initialClass);
		return null;
	}
}
