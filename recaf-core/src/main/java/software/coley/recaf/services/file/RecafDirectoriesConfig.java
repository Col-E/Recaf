package software.coley.recaf.services.file;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.config.BasicConfigContainer;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.util.ExcludeFromJacocoGeneratedReport;
import software.coley.recaf.util.IOUtil;
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
@ExcludeFromJacocoGeneratedReport(justification = "We do not access the config directories in tests (avoiding IO is preferred anyways)")
public class RecafDirectoriesConfig extends BasicConfigContainer implements ConfigContainer {
	private static final Logger logger = Logging.get(RecafDirectoriesConfig.class);
	private final Path baseDirectory = createBaseDirectory();
	private final Path agentDirectory = resolveDirectory("agent");
	private final Path configDirectory = resolveDirectory("config");
	private final Path logsDirectory = resolveDirectory("logs");
	private final Path pluginDirectory = resolveDirectory("plugins");
	private final Path styleDirectory = resolveDirectory("style");
	private final Path scriptsDirectory = resolveDirectory("scripts");
	private final Path tempDirectory = resolveDirectory("temp");
	private Path currentLog;

	@Inject
	public RecafDirectoriesConfig() {
		super(ConfigGroups.SERVICE_IO, "directories" + CONFIG_SUFFIX);
		setupLocalTempDir();
	}

	/**
	 * @param currentLog
	 * 		Path to current log-file.
	 */
	public void initCurrentLogPath(@Nonnull Path currentLog) {
		if (this.currentLog == null)
			this.currentLog = currentLog;
	}

	/**
	 * @return Path to current log-file.
	 */
	@Nonnull
	public Path getCurrentLogPath() {
		return currentLog;
	}

	/**
	 * @return Base Recaf directory.
	 */
	@Nonnull
	public Path getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * @return Directory where agent jars are stored.
	 */
	@Nonnull
	public Path getAgentDirectory() {
		return agentDirectory;
	}

	/**
	 * @return Directory where configuration is stored.
	 */
	@Nonnull
	public Path getConfigDirectory() {
		return configDirectory;
	}

	/**
	 * @return Directory where old logs are stored.
	 */
	@Nonnull
	public Path getLogsDirectory() {
		return logsDirectory;
	}

	/**
	 * @return Directory where plugins are stored.
	 */
	@Nonnull
	public Path getPluginDirectory() {
		return pluginDirectory;
	}

	/**
	 * Set via launch arguments to facilitate plugin development. Usually not set otherwise.
	 *
	 * @return Directory where extra plugins are stored. Can be {@code null}.
	 */
	@Nullable
	public Path getExtraPluginDirectory() {
		String pathProperty = System.getProperty("RECAF_EXTRA_PLUGINS");
		if (pathProperty == null)
			return null;
		Path path = Paths.get(pathProperty);
		if (Files.isDirectory(path))
			return path;
		return null;
	}

	/**
	 * @return Directory where disabled plugins are stored.
	 */
	@Nonnull
	public Path getDisabledPluginDirectory() {
		return getPluginDirectory().resolve("disabled");
	}

	/**
	 * @return Directory where additional stylesheets are stored.
	 */
	@Nonnull
	public Path getStyleDirectory() {
		return styleDirectory;
	}

	/**
	 * @return Directory where scripts are stored.
	 */
	@Nonnull
	public Path getScriptsDirectory() {
		return scriptsDirectory;
	}

	/**
	 * @return Directory where temporary files are stored.
	 */
	@Nonnull
	public Path getTempDirectory() {
		return tempDirectory;
	}

	@Nonnull
	private Path resolveDirectory(@Nonnull String dir) {
		Path path = baseDirectory.resolve(dir);
		try {
			Files.createDirectories(path);
		} catch (IOException ex) {
			logger.error("Could not create Recaf directory: " + dir, ex);
		}
		return path;
	}

	private void setupLocalTempDir() {
		// If it does not exist yet, make it.
		if (!Files.isDirectory(tempDirectory)) {
			try {
				Files.createDirectories(tempDirectory);
			} catch (IOException ex) {
				logger.error("Failed creating temp directory", ex);
			}
		}

		// When we shut down, remove all files inside of it.
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (Files.isDirectory(tempDirectory))
					IOUtil.cleanDirectory(tempDirectory);
			} catch (IOException ex) {
				logger.error("Failed clearing temp directory", ex);
			}
		}));
	}

	/**
	 * @return Root directory for storing Recaf data.
	 */
	@Nonnull
	public static Path createBaseDirectory() {
		// Try system property first
		String recafDir = System.getProperty("RECAF_DIR");
		if (recafDir == null) // Next try looking for an environment variable
			recafDir = System.getenv("RECAF");
		if (recafDir != null)
			return Paths.get(recafDir);

		// Otherwise put it in the system's config directory
		Path dir = getSystemConfigDir();
		if (dir == null)
			throw new IllegalStateException("Failed to determine config directory for: " + System.getProperty("os.name"));
		return dir.resolve("Recaf");
	}

	/**
	 * @return Root config directory for the current OS.
	 */
	@Nullable
	private static Path getSystemConfigDir() {
		if (PlatformType.isWindows()) {
			return Paths.get(System.getenv("APPDATA"));
		} else if (PlatformType.isMac()) {
			// Mac-OS paths:
			//  https://developer.apple.com/library/archive/qa/qa1170/_index.html
			return Paths.get(System.getProperty("user.home") + "/Library/Application Support");
		} else if (PlatformType.isLinux()) {
			// $XDG_CONFIG_HOME or $HOME/.config
			String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
			if (xdgConfigHome != null)
				return Paths.get(xdgConfigHome);
			return Paths.get(System.getProperty("user.home") + "/.config");
		}
		return null;
	}
}
