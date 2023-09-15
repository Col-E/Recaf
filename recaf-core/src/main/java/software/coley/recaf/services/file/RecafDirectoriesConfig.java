package software.coley.recaf.services.file;

import dev.dirs.BaseDirectories;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.util.PlatformType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Config for common paths for Recaf.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class RecafDirectoriesConfig extends BasicConfigContainer implements ConfigContainer {
	private static final Logger logger = Logging.get(RecafDirectoriesConfig.class);
	private final Path baseDirectory = createBaseDirectory();
	private final Path agentDirectory = resolveDirectory("agent");
	private final Path configDirectory = resolveDirectory("config");
	private final Path logsDirectory = resolveDirectory("logs");
	private final Path pluginDirectory = resolveDirectory("plugins");
	private final Path styleDirectory = resolveDirectory("style");
	private final Path scriptsDirectory = resolveDirectory("scripts");
	private Path currentLog;

	@Inject
	public RecafDirectoriesConfig() {
		super(ConfigGroups.SERVICE_IO, "directories" + CONFIG_SUFFIX);
	}

	/**
	 * @param currentLog
	 * 		Path to current log-file.
	 */
	public void initCurrentLogPath(Path currentLog) {
		if (this.currentLog == null) {
			this.currentLog = currentLog;
		}
	}

	/**
	 * @return Path to current log-file.
	 */
	public Path getCurrentLogPath() {
		return currentLog;
	}

	/**
	 * @return Base Recaf directory.
	 */
	public Path getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @return Directory where agent jars are stored.
	 */
	public Path getAgentDirectory() {
		return agentDirectory;
	}

	/**
	 * @return Directory where configuration is stored.
	 */
	public Path getConfigDirectory() {
		return configDirectory;
	}

	/**
	 * @return Directory where old logs are stored.
	 */
	public Path getLogsDirectory() {
		return logsDirectory;
	}

	/**
	 * @return Directory where plugins are stored.
	 */
	public Path getPluginDirectory() {
		return pluginDirectory;
	}

	/**
	 * @return Directory where disabled plugins are stored.
	 */
	public Path getDisabledPluginDirectory() {
		return getPluginDirectory().resolve("disabled");
	}

	/**
	 * @return Directory where additional stylesheets are stored.
	 */
	public Path getStyleDirectory() {
		return styleDirectory;
	}

	/**
	 * @return Directory where scripts are stored.
	 */
	public Path getScriptsDirectory() {
		return scriptsDirectory;
	}

	private Path resolveDirectory(String dir) {
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
			// And we can look up the APPDATA folder easily.
			if (PlatformType.get() == PlatformType.WINDOWS) {
				return Paths.get(System.getenv("APPDATA"), "Recaf");
			} else {
				throw new IllegalStateException("Failed to initialize Recaf directory", t);
			}
		}
	}
}
