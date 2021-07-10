package dev.xdark.recaf.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Wrapper for argument parsing.
 *
 * @author xDark
 * @author Matt Coley
 */
public class Arguments implements Callable<Void> {
	private static final Logger logger = LoggerFactory.getLogger("Launcher");
	@CommandLine.Option(names = {"-h", "--home"},
			description = "Recaf jar file to launch",
			required = true)
	private Path jarOption;
	@CommandLine.Option(names = {"-u", "--update"},
			description = "Should the update be done automatically?",
			required = true)
	private boolean autoUpdateOption;

	/**
	 * @param args
	 * 		Program arguments.
	 *
	 * @return Instance.
	 */
	public static Arguments from(String[] args) {
		Arguments parsed = new Arguments();
		new CommandLine(parsed).execute(args);
		return parsed;
	}

	@Override
	public Void call() throws Exception {
		logger.debug("Home path:{}", getJarPath());
		logger.debug("Do update:{}", doAutoUpdate());
		return null;
	}

	/**
	 * @return Option value for jar path.
	 */
	public Path getJarPath() {
		return jarOption;
	}

	/**
	 * @return Option flag for auto-updating.
	 */
	public boolean doAutoUpdate() {
		return autoUpdateOption;
	}
}
