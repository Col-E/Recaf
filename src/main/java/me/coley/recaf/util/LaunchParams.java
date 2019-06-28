package me.coley.recaf.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import me.coley.recaf.Logging;
import picocli.CommandLine.Option;

public class LaunchParams implements Runnable {
	@Option(names = { "-i", "--input" }, description = "Jar file to read.")
	public File initialFile;
	@Option(names = { "-c", "--class" }, description = "Initial class to open.")
	public String initialClass;
	@Option(names = { "-s", "--skip" }, description = "Classes in the input to ignore.", arity = "0..*")
	public List<String> skipped = new ArrayList<>();

	@Override
	public void run() {
		if (initialFile != null) Logging.info("CLI file: " + initialFile);
		if (initialClass != null) Logging.info("CLI class: " + initialClass);
		if (skipped != null) Logging.info("CLI Skipped packages: " + skipped.size());
	}
}
