package dev.xdark.recaf.cli;

import dev.xdark.recaf.util.PathUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Wrapper for argument parsing.
 *
 * @author xDark
 * @author Matt Coley
 */
public class Arguments {
	private static final Logger logger = LoggerFactory.getLogger("Launcher");
	private final OptionSet options;
	private final OptionSpec<Path> jarOption;
	private final OptionSpec<Boolean> autoUpdateOption;

	/**
	 * @param args
	 * 		Program arguments.
	 */
	public Arguments(String[] args) {
		OptionParser parser = new OptionParser();
		jarOption = parser.accepts("home", "Recaf jar file")
				.withRequiredArg()
				.withValuesConvertedBy(new PathValueConverter())
				.defaultsTo(PathUtils.getRecafDirectory().resolve("Recaf.jar"));
		autoUpdateOption = parser
				.accepts("autoUpdate", "Should the update be done automatically?")
				.withRequiredArg()
				.ofType(Boolean.class)
				.defaultsTo(true);
		parser.allowsUnrecognizedOptions();
		options = parser.parse(args);
		// Log unknown arguments
		List<?> unknown = options.nonOptionArguments();
		if (!unknown.isEmpty()) {
			logger.warn("Ignoring unknown options: {}", unknown);
		}
	}

	/**
	 * @return Option value for jar path.
	 */
	public Path getJarPath() {
		return options.valueOf(jarOption);
	}

	/**
	 * @return Option flag for auto-updating.
	 */
	public boolean doAutoUpdate() {
		return options.valueOf(autoUpdateOption);
	}
}
