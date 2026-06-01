package software.coley.recaf.launch;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.inject.spi.Bean;
import org.slf4j.Logger;
import software.coley.recaf.Bootstrap;
import software.coley.recaf.Recaf;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.EagerInitializationExtension;
import software.coley.recaf.cdi.InitializationEvent;
import software.coley.recaf.services.compile.CompilerDiagnostic;
import software.coley.recaf.services.file.RecafDirectoriesConfig;
import software.coley.recaf.services.plugin.PluginContainer;
import software.coley.recaf.services.plugin.PluginException;
import software.coley.recaf.services.plugin.PluginManager;
import software.coley.recaf.services.plugin.discovery.DirectoryPluginDiscoverer;
import software.coley.recaf.services.script.ScriptEngine;
import software.coley.recaf.services.script.ScriptResult;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.services.workspace.io.ResourceImporter;
import software.coley.recaf.util.threading.ThreadUtil;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Common launch bootstrap shared by core-headless and UI startup flows.
 *
 * @author Matt Coley
 */
public class LaunchBootstrap {
	private static final Logger logger = Logging.get(LaunchBootstrap.class);
	private final LaunchCommand launchCommand;
	private final String[] args;
	private Recaf recaf;
	private LaunchArguments launchArgs;

	public LaunchBootstrap(@Nonnull LaunchCommand launchCommand, @Nonnull String[] args) {
		this.launchCommand = launchCommand;
		this.args = args;
	}

	/**
	 * Create the Recaf container and expose launch arguments to CDI.
	 *
	 * @return Recaf instance.
	 */
	@Nonnull
	public Recaf bootstrap() {
		recaf = Bootstrap.get();
		launchArgs = recaf.get(LaunchArguments.class);
		launchArgs.setCommand(launchCommand);
		launchArgs.setRawArgs(args);
		return recaf;
	}

	/**
	 * Register the startup input/script handler bean for the current launch mode.
	 *
	 * @param headless
	 *        {@code true} when running without the UI.
	 */
	public void setupLaunchHandler(boolean headless) {
		// Set up the launch-handler bean to load inputs if specified by the launch arguments.
		Bean<?> bean = recaf.getContainer().getBeanContainer().getBeans(LaunchHandler.class).iterator().next();
		if (headless) {
			LaunchHandler.task = this::handleInputs;
			addIfMissing(EagerInitializationExtension.getApplicationScopedEagerBeans(), bean);
		} else {
			// Run input handling in the background so that it does not block the UI thread.
			LaunchHandler.task = () -> CompletableFuture.runAsync(this::handleInputs, ThreadUtil.executor());
			addIfMissing(EagerInitializationExtension.getApplicationScopedEagerBeansForUi(), bean);
		}
	}

	/**
	 * Initialize the headless runtime.
	 */
	public void initializeHeadless() {
		initLogging();
		initPlugins();
		fireInitEvent();
		blockIfIdle();
	}

	/**
	 * Publishes the initialization event to trigger eager services.
	 */
	public void fireInitEvent() {
		recaf.getContainer().getBeanContainer().getEvent().fire(new InitializationEvent());
	}

	/**
	 * Configure file logging appender and compress old logs.
	 */
	public void initLogging() {
		RecafDirectoriesConfig directories = recaf.get(RecafDirectoriesConfig.class);

		// Setup appender
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Path logFile = directories.getBaseDirectory().resolve("log-" + date + ".txt");
		directories.initCurrentLogPath(logFile);
		Logging.addFileAppender(logFile);

		// Set default error handler
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> logger.error("Uncaught exception on thread '{}'", t.getName(), e));

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
	 * Load plugins from the configured plugin directories.
	 */
	public void initPlugins() {
		PluginManager pluginManager = recaf.get(PluginManager.class);

		// Load from the plugin directory
		try {
			RecafDirectoriesConfig dirConfig = recaf.get(RecafDirectoriesConfig.class);
			Path pluginDirectory = dirConfig.getPluginDirectory();
			Path extraPluginDirectory = dirConfig.getExtraPluginDirectory();
			pluginManager.loadPlugins(new DirectoryPluginDiscoverer(pluginDirectory));
			if (extraPluginDirectory != null)
				pluginManager.loadPlugins(new DirectoryPluginDiscoverer(extraPluginDirectory));
		} catch (PluginException ex) {
			logger.error("Failed to initialize plugins", ex);
		}

		// Log the discovered plugins
		Collection<PluginContainer<?>> plugins = pluginManager.getPlugins();
		if (plugins.isEmpty()) {
			logger.info("Initialization: No plugins found");
		} else {
			String split = "\n - ";
			logger.info("Initialization: {} plugins found:" + split + "{}",
					plugins.size(),
					plugins.stream().map(PluginContainer::info)
							.map(info -> info.name() + " - " + info.version())
							.collect(Collectors.joining(split)));
		}
	}

	/**
	 * @return Launch arguments bean populated during {@link #bootstrap()}.
	 */
	@Nonnull
	public LaunchArguments getLaunchArguments() {
		return launchArgs;
	}

	/**
	 * @return Recaf instance created during {@link #bootstrap()}.
	 */
	@Nonnull
	public Recaf getRecaf() {
		return recaf;
	}

	private void blockIfIdle() {
		if (!launchCommand.isIdle())
			return;

		logger.info("Headless idle mode enabled, waiting for shutdown");
		try {
			awaitIdleShutdown();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			logger.warn("Headless idle wait interrupted", ex);
		}
	}

	private void handleInputs() {
		try {
			File input = launchArgs.getInput();
			if (input != null && input.exists()) {
				ResourceImporter importer = recaf.get(ResourceImporter.class);
				WorkspaceManager workspaceManager = recaf.get(WorkspaceManager.class);
				workspaceManager.setCurrent(new BasicWorkspace(importer.importResource(input)));
			}
		} catch (Throwable t) {
			logger.error("Error handling loading of launch workspace content.", t);
		}

		try {
			File script = launchArgs.getScript();
			if (script != null && !script.isFile())
				script = launchArgs.getScriptInScriptsDirectory();
			if (script != null && script.isFile()) {
				ScriptResult result = recaf.get(ScriptEngine.class)
						.run(Files.readString(script.toPath()))
						.get();
				if (!result.wasSuccess()) {
					if (result.wasRuntimeError()) {
						logger.error("Error encountered when executing script '{}'", script.getName());
					} else if (result.wasCompileFailure()) {
						logger.error("Error compiling script:\n{}", result.getCompileDiagnostics().stream()
								.map(CompilerDiagnostic::toString)
								.collect(Collectors.joining("\n")));
					}
				}
			}
		} catch (Throwable t) {
			logger.error("Error handling execution of launch script.", t);
		}
	}

	private static void addIfMissing(@Nonnull List<Bean<?>> beans, @Nonnull Bean<?> bean) {
		if (!beans.contains(bean))
			beans.add(bean);
	}

	private static void awaitIdleShutdown() throws InterruptedException {
		// Just block the main thread indefinitely until the process is killed.
		new CountDownLatch(1).await();
	}
}
