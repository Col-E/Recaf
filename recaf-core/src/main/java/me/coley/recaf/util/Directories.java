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
	private static final Path agentDirectory = resolveDirectory("agent");
	private static final Path classpathDirectory = resolveDirectory("classpath");
	private static final Path configDirectory = resolveDirectory("config");
	private static final Path dependenciesDirectory = resolveDirectory("dependencies");
	private static final Path logsDirectory = resolveDirectory("logs");
	private static final Path pluginDirectory = resolveDirectory("plugins");
	private static final Path styleDirectory = resolveDirectory("style");
	private static final Path scriptsDirectory = resolveDirectory("scripts");

	/**
	 * @return Base Recaf directory.
	 */
	public static Path getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @return Directory where agent jars are stored.
	 */
	public static Path getAgentDirectory() {
		return agentDirectory;
	}

	/**
	 * @return Directory where extensions for the compiler classpath are stored.
	 */
	public static Path getClasspathDirectory() {
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
	 * @return Directory where old logs are stored.
	 */
	public static Path getLogsDirectory() {
		return logsDirectory;
	}

	/**
	 * @return Directory where plugins are stored.
	 */
	public static Path getPluginDirectory() {
		// TODO: We can use a "disabled" subfolder to mark a plugin as disabled
		return pluginDirectory;
	}

	/**
	 * @return Directory where additional stylesheets are stored.
	 */
	public static Path getStyleDirectory() {
		return styleDirectory;
	}

	/**
	 * @return Directory where scripts are stored.
	 */
	public static Path getScriptsDirectory() { return scriptsDirectory; }

	private static Path resolveDirectory(String dir) {
		Path path = baseDirectory.resolve(dir);
		try {
			Files.createDirectories(path);
		} catch (IOException ex) {
			logger.error("Could not create Recaf directory: " + dir, ex);
		}
		return path;
	}

	private static Path createBaseDirectory() {
		// Try environment variable first
		String recafDir = System.getenv("RECAF");
		if (recafDir != null) {
			return Paths.get(recafDir);
		}
		// Use generic data/config location
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
			if (PlatformType.get() == PlatformType.WINDOWS) {
				return Paths.get(System.getenv("APPDATA"), "Recaf");
			} else {
				throw new IllegalStateException("Failed to initialize Recaf directory", t);
			}
		}
	}
}
