package me.coley.recaf;

import me.coley.recaf.cdi.RecafContainer;
import me.coley.recaf.plugin.RecafPluginManager;
import me.coley.recaf.launch.InitializerParameters;
import me.coley.recaf.presentation.PresentationType;
import me.coley.recaf.scripting.ScriptEngine;
import me.coley.recaf.scripting.ScriptResult;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadUtil;
import org.slf4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Entry point.
 *
 * @author Matt Coley
 */
public class Main {
	private static final Logger logger = Logging.get(Main.class);

	/**
	 * Main entry point.
	 *
	 * @param args
	 * 		Program arguments.
	 */
	public static void main(String[] args) {
		setupLogging();
		// Initialize container
		RecafContainer.initialize();
		// Initialization plugin
		RecafPluginManager.initialize();
		// Read application parameters
		InitializerParameters parameters = InitializerParameters.fromArgs(args);
		Recaf.initialize(parameters);
		// run script from parameters if found
		if (parameters.getScriptPath() != null) {
			Path scriptPath = parameters.getScriptPath().toPath();
			if (Files.isRegularFile(scriptPath)) {
				Runnable r = () -> {
					try {
						ScriptResult result = ScriptEngine.execute(scriptPath);
						if (result.wasSuccess()) {
							logger.info("Script execute complete");
						} else if (result.wasCompileFailure()) {
							logger.error("Script has compile errors: {}", result.getCompileDiagnostics().stream()
									.map(Object::toString)
									.collect(Collectors.joining(", ")));
						} else if (result.wasRuntimeError()) {
							logger.error("Script encountered error while running", result.getRuntimeThrowable());
						}
					} catch (IOException ex) {
						logger.error("Failed to read script: {}", scriptPath);
					}
				};
				if (parameters.getPresentationType() == PresentationType.GUI) {
					// Run the script on a delay, giving time to for the GUI to populate
					ThreadUtil.runDelayed(500, r);
				} else {
					// Run the script on the main thread
					r.run();
				}
			} else {
				logger.error("No script found: {}", scriptPath);
			}
		}
	}

	/**
	 * Setup file logging appender and compress old logs.
	 */
	private static void setupLogging() {
		// Setup appender
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Path logFile = Directories.getBaseDirectory().resolve("log-" + date + ".txt");
		Logging.addFileAppender(logFile);
		// Archive old logs
		try {
			Files.createDirectories(Directories.getLogsDirectory());
			List<Path> oldLogs = Files.list(Directories.getBaseDirectory())
					.filter(p -> p.getFileName().toString().matches("log-\\d+-\\d+-\\d+\\.txt"))
					.collect(Collectors.toList());
			// Do not treat the current log file as an old log file
			oldLogs.remove(logFile);
			logger.trace("Compressing {} old log files", oldLogs.size());
			for (Path oldLog : oldLogs) {
				String originalFileName = oldLog.getFileName().toString();
				String archiveFileName = originalFileName.replace(".txt", ".zip");
				Path archivedLog = Directories.getLogsDirectory().resolve(archiveFileName);
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
}
