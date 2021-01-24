package me.coley.recaf.util;

import dev.dirs.BaseDirectories;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Common Recaf directories.
 *
 * @author Matt Coley
 */
public class Directories {
	private static final Logger logger = Logging.get(Directories.class);
	private static final Path baseDirectory = createBaseDirectory();
	private static final Path classpathDirectory = baseDirectory.resolve("classpath");
	private static final Path configDirectory = baseDirectory.resolve("config");
	private static final Path dependenciesDirectory = baseDirectory.resolve("dependencies");
	private static final Path phantomsDirectory = baseDirectory.resolve("phantoms");
	private static final Path pluginDirectory = baseDirectory.resolve("plugins");
	private static final Path styleDirectory = baseDirectory.resolve("style");

	/**
	 * @return Base Recaf directory.
	 */
	public static Path getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @return Directory where extensions for the compiler classpath are stored.
	 */
	public static Path getClasspathDirectory() {
		// TODO: If we want to save space when "copying" resources into this folder we can
		//       use ASM to strip out method bodies. Keep debug info for signatures though.
		//       Can also exclude any non-classes from the jar.
		//
		// TODO: Do we want to split this into two subdirs?
		//        - Jars     - Point to list of contained jars
		//        - Classes  - Point to root
		return classpathDirectory;
	}

	/**
	 * @return Directory where configuration is stored.
	 */
	public static Path getConfigDirectory() {
		return configDirectory;
	}

	/**
	 * @return Directory where additional Recaf dependencies are stored to be injected at runtime.
	 */
	public static Path getDependenciesDirectory() {
		return dependenciesDirectory;
	}

	/**
	 * @return Directory where plugins are stored.
	 */
	public static Path getPluginDirectory() {
		// TODO: We can use a "disabled" subfolder to mark a plugin as disabled
		return pluginDirectory;
	}

	/**
	 * @return Directory where generated phantom classes are stored.
	 */
	public static Path getPhantomsDirectory() {
		// TODO: Allow an opt-in feature where phantom classes are not deleted.
		//  - Means if you re-open the same resource, it looks for the cached first
		return phantomsDirectory;
	}

	/**
	 * @return Directory where additional stylesheets are stored.
	 */
	public static Path getStyleDirectory() {
		return styleDirectory;
	}

	private static Path createBaseDirectory() {
		try {
			// Windows: %APPDATA%/
			// Mac:     $HOME/Library/Application Support/
			// Linux:   $XDG_CONFIG_HOME/   or   $HOME/.config
			String dir = BaseDirectories.get().configDir;
			if (dir == null)
				throw new NullPointerException("BaseDirectories did not yield an initial directory");
			return Paths.get(dir).resolve("Recaf");
		} catch (Throwable t) {
			// The lookup only seems to fail on windows.
			// And we can lookup the APPDATA folder easily.
			if (OperatingSystem.get() == OperatingSystem.WINDOWS) {
				return Paths.get(System.getenv("APPDATA"), "Recaf");
			} else {
				throw new IllegalStateException("Failed to initialize Recaf directory");
			}
		}
	}

	static {
		try {
			Files.createDirectories(classpathDirectory);
			Files.createDirectories(configDirectory);
			Files.createDirectories(dependenciesDirectory);
			Files.createDirectories(phantomsDirectory);
			Files.createDirectories(pluginDirectory);
			Files.createDirectories(styleDirectory);
		} catch (IOException ex) {
			logger.error("Failed to create Recaf directories", ex);
		}
	}
}
