package software.coley.recaf;

import org.slf4j.Logger;
import picocli.CommandLine;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.launch.LaunchArguments;
import software.coley.recaf.launch.LaunchCommand;
import software.coley.recaf.plugin.Plugin;
import software.coley.recaf.plugin.PluginContainer;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.plugin.PluginManager;
import software.coley.recaf.services.script.ScriptEngine;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.util.JFXValidation;
import software.coley.recaf.util.Lang;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.io.ResourceImporter;
import software.coley.recaf.workspace.model.BasicWorkspace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Application entry-point for Recaf's UI.
 *
 * @author Matt Coley
 */
public class Main {
	private static final Logger logger = Logging.get(Main.class);
	private static LaunchArguments launchArgs;
	private static Recaf recaf;

	/**
	 * @param args
	 * 		Application arguments.
	 */
	public static void main(String[] args) {
		// Add a class reference for our UI module.
		Bootstrap.setWeldConsumer(weld -> weld.addPackage(true, Main.class));

		// Handle arguments.
		LaunchCommand launchArgValues = new LaunchCommand();
		try {
			CommandLine.populateCommand(launchArgValues, args);
			if (launchArgValues.call())
				return;
		} catch (Exception ex) {
			CommandLine.usage(launchArgValues, System.out);
			return;
		}

		// Validate the JFX environment is available if not running in headless mode.
		// Abort if not available.
		if (!launchArgValues.isHeadless()) {
			int validationCode = JFXValidation.validateJFX();
			if (validationCode != 0) {
				System.exit(validationCode);
				return;
			}
		}

		// Invoke the bootstrapper, initializing the UI once the container is built.
		recaf = Bootstrap.get();
		launchArgs = recaf.get(LaunchArguments.class);
		launchArgs.setCommand(launchArgValues);
		launchArgs.setRawArgs(args);
		initialize();
	}

	/**
	 * Initialize the UI application.
	 */
	private static void initialize() {
		initLogging();
		if (launchArgs.isHeadless()) {
			initPlugins();
			initHandleInputs();
		} else {
			initTranslations();
			initPlugins();
			FxThreadUtil.delayedRun(500, Main::initHandleInputs);
			RecafApplication.launch(RecafApplication.class, launchArgs.getArgs());
		}
	}

	/**
	 * Configure file logging appender and compress old logs.
	 */
	private static void initLogging() {
		RecafDirectoriesConfig directories = recaf.get(RecafDirectoriesConfig.class);

		// Setup appender
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Path logFile = directories.getBaseDirectory().resolve("log-" + date + ".txt");
		directories.initCurrentLogPath(logFile);
		Logging.addFileAppender(logFile);

		// Set default error handler
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			logger.error("Uncaught exception on thread '{}'", t.getName(), e);
		});

		// Archive old logs
		try {
			Files.createDirectories(directories.getLogsDirectory());
			List<Path> oldLogs = Files.list(directories.getBaseDirectory())
					.filter(p -> p.getFileName().toString().matches("log-\\d+-\\d+-\\d+\\.txt"))
					.collect(Collectors.toList());

			// Do not treat the current log file as an old log file
			oldLogs.remove(logFile);

			// Handling old entries
			logger.trace("Compressing {} old log files", oldLogs.size());
			for (Path oldLog : oldLogs) {
				String originalFileName = oldLog.getFileName().toString();
				String archiveFileName = originalFileName.replace(".txt", ".zip");
				Path archivedLog = directories.getLogsDirectory().resolve(archiveFileName);

				// Compress the log into a zip
				try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivedLog.toFile()))) {
					zos.putNextEntry(new ZipEntry(originalFileName));
					Files.copy(oldLog, zos);
					zos.closeEntry();
				}

				// Remove the old file
				Files.delete(oldLog);
			}
		} catch (IOException ex) {
			logger.warn("Failed to compress old logs", ex);
		}
	}

	/**
	 * Load translations.
	 */
	private static void initTranslations() {
		Lang.initialize();
	}

	/**
	 * Load plugins.
	 */
	private static void initPlugins() {
		// Plugin loading is handled in the implementation's @PostConstruct handler
		PluginManager pluginManager = recaf.get(PluginManager.class);

		// Log the discovered plugins
		Collection<PluginContainer<? extends Plugin>> plugins = pluginManager.getPlugins();
		if (plugins.isEmpty()) {
			logger.info("Initialization: No plugins found");
		} else {
			String split = "\n - ";
			logger.info("Initialization: {} plugins found:" + split + "{}",
					plugins.size(),
					plugins.stream().map(PluginContainer::getInformation)
							.map(info -> info.getName() + " - " + info.getVersion())
							.collect(Collectors.joining(split)));
		}
	}

	private static void initHandleInputs() {
		// Open initial file if found.
		try {
			File input = launchArgs.getInput();
			if (input != null && input.isFile()) {
				ResourceImporter importer = recaf.get(ResourceImporter.class);
				WorkspaceManager workspaceManager = recaf.get(WorkspaceManager.class);
				workspaceManager.setCurrent(new BasicWorkspace(importer.importResource(input)));
			}
		} catch (Throwable t) {
			logger.error("Error handling loading of launch workspace content.", t);
		}

		// Run startup script.
		try {
			File script = launchArgs.getScript();
			if (script != null && !script.isFile())
				script = launchArgs.getScriptInScriptsDirectory();
			if (script != null && script.isFile())
				recaf.get(ScriptEngine.class)
						.run(Files.readString(script.toPath()));
		} catch (Throwable t) {
			logger.error("Error handling execution of launch script.", t);
		}
	}
}
