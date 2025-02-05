package software.coley.recaf.services.script;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import regexodus.Matcher;
import org.slf4j.Logger;
import software.coley.observables.ObservableBoolean;
import software.coley.observables.ObservableCollection;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.services.Service;
import software.coley.recaf.util.RegexUtil;
import software.coley.recaf.util.StringUtil;
import software.coley.recaf.util.threading.ThreadPoolFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Manages local script files.
 *
 * @author Matt Coley
 * @see ScriptEngine Executor for scripts.
 * @see ScriptFile Local script file type.
 */
@ApplicationScoped
public class ScriptManager implements Service {
	public static final String SERVICE_ID = "script-manager";
	private static final String TAG_PATTERN = "//(\\s+)?@({key}\\S+)\\s+({value}.+)";
	private static final Logger logger = Logging.get(ScriptManager.class);
	private final ObservableCollection<ScriptFile, List<ScriptFile>> scriptFiles = new ObservableCollection<>(ArrayList::new);
	private final ScriptManagerConfig config;
	private final WatchTask watchTask;

	@Inject
	public ScriptManager(@Nonnull ScriptManagerConfig config) {
		this.config = config;
		watchTask = new WatchTask();

		// Start watching files in scripts directory
		ObservableBoolean fileWatching = config.getFileWatching();
		if (fileWatching.getValue())
			watchTask.start();

		// When the watch flag is re-enabled, re-submit the watch-pool task.
		fileWatching.addChangeListener((ob, old, cur) -> {
			if (cur)
				watchTask.start();
			else
				watchTask.stop();
		});
	}

	/**
	 * @param path
	 * 		Path to script file.
	 *
	 * @return Wrapper of script.
	 *
	 * @throws IOException
	 * 		When the script file cannot be read from.
	 */
	@Nonnull
	public ScriptFile read(@Nonnull Path path) throws IOException {
		String text = Files.readString(path);

		// Parse tags from beginning of file
		Map<String, String> tags = new HashMap<>();
		int metaEnd = Math.max(0, text.indexOf("==/Metadata=="));
		int lineMetaEnd = StringUtil.count("\n", text.substring(0, metaEnd));
		text.lines().limit(lineMetaEnd).forEach(line -> {
			if (line.startsWith("//")) {
				Matcher matcher = RegexUtil.getMatcher(TAG_PATTERN, line);
				if (matcher.matches()) {
					String key = matcher.group("key").toLowerCase();
					String value = matcher.group("value");
					tags.put(key, value);
				}
			}
		});

		return new ScriptFile(path, text, tags);
	}


	/**
	 * @param path
	 * 		Path of file newly created.
	 */
	private void onScriptCreate(@Nonnull Path path) {
		try {
			logger.debug("Script created: {}", path);
			ScriptFile file = read(path);
			scriptFiles.add(file);
		} catch (IOException ex) {
			logger.error("Could not load script from path: {}", path, ex);
		}
	}

	/**
	 * @param path
	 * 		Path of file modified.
	 */
	private void onScriptUpdated(@Nonnull Path path) {
		try {
			// Read updated script content from path.
			ScriptFile updated = read(path);

			// Replace old file wrapper with new wrapper.
			// Only do so if they are not equal. There are some odd situations where you will get duplicate
			// file-watcher events on the same file even if the contents are not modified.
			if (scriptFiles.removeIf(file -> path.equals(file.path()) && !file.equals(updated)))
				scriptFiles.add(updated);
		} catch (IOException ex) {
			logger.error("Could not load script from path: {}", path, ex);
		}
	}

	/**
	 * @param path
	 * 		Path of file removed.
	 */
	private void onScriptRemoved(@Nonnull Path path) {
		logger.debug("Script removed: {}", path);
		scriptFiles.removeIf(file -> path.equals(file.path()));
	}

	/**
	 * @return Collection of local available script files.
	 */
	@Nonnull
	public ObservableCollection<ScriptFile, List<ScriptFile>> getScriptFiles() {
		return scriptFiles;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public ScriptManagerConfig getServiceConfig() {
		return config;
	}

	/**
	 * Wrapper to manage threaded watch service on the script directory.
	 */
	private class WatchTask {
		private final Path scriptsDirectory = config.getScriptsDirectory();
		private final ExecutorService watchPool = ThreadPoolFactory.newSingleThreadExecutor(SERVICE_ID);
		private Future<?> watchFuture;
		private WatchService watchService;

		private void start() {
			scanExisting();

			// Only start a new thread when the old one is complete, or if no prior one exists.
			if (watchFuture == null || watchFuture.isDone()) {
				logger.debug("Starting script directory watch task");
				watchFuture = watchPool.submit(this::watch);
			}
		}

		private void stop() {
			// Calling 'close()' on the service will make the loop on the service event 'take()' break.
			if (watchService != null) {
				try {
					logger.debug("Stopping script directory watch task");
					watchService.close();
				} catch (IOException ex) {
					logger.error("Failed to stop script directory watch service");
				}
				watchFuture.cancel(true);
				watchService = null;
			}
		}

		private void scanExisting() {
			try {
				// Walk the directory, create or update scripts that exist.
				Set<ScriptFile> scriptsCopy = new HashSet<>(scriptFiles);
				Files.walk(scriptsDirectory).forEach(path -> {
					if (Files.isRegularFile(path)) {
						Optional<ScriptFile> matchingScript = scriptsCopy.stream()
								.filter(script -> script.path().equals(path))
								.findFirst();
						if (matchingScript.isPresent()) {
							scriptsCopy.remove(matchingScript.get());
							onScriptUpdated(path);
						} else {
							onScriptCreate(path);
						}
					}
				});

				// Any remaining items in the set do not exist in the directory, so we remove them.
				scriptFiles.removeAll(scriptsCopy);
			} catch (IOException ex) {
				logger.error("Failed to scan existing scripts in script directory", ex);
			}
		}

		private void watch() {
			try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
				this.watchService = watchService;
				WatchKey watchKey = scriptsDirectory.register(watchService,
						StandardWatchEventKinds.ENTRY_CREATE,
						StandardWatchEventKinds.ENTRY_MODIFY,
						StandardWatchEventKinds.ENTRY_DELETE);
				WatchKey key;
				while ((key = watchService.take()) != null) {
					for (WatchEvent<?> event : key.pollEvents()) {
						Path eventPath = scriptsDirectory.resolve((Path) event.context());
						WatchEvent.Kind<?> kind = event.kind();
						if (Files.isRegularFile(eventPath)) {
							try {
								// We are only interested in 'ENTRY_MODIFY' events since that is when file content is written.
								// A script file created via 'ENTRY_CREATE' will always be empty, so reading from it at
								// that point would be useless.
								if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
									Set<ScriptFile> scriptsCopy = new HashSet<>(scriptFiles);
									Optional<ScriptFile> matchingScript = scriptsCopy.stream()
											.filter(script -> script.path().equals(eventPath))
											.findFirst();
									if (matchingScript.isPresent()) {
										scriptsCopy.remove(matchingScript.get());
										onScriptUpdated(eventPath);
									} else {
										onScriptCreate(eventPath);
									}
								}
							} catch (Throwable t) {
								logger.error("Unhandled exception updating available scripts", t);
							}
						} else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
							onScriptRemoved(eventPath);
						}
					}
					if (!key.reset())
						logger.warn("Key was unregistered: {}", key);
				}
				watchKey.cancel();
				logger.info("Stopped watching script directory for updates");
			} catch (IOException ex) {
				logger.error("IO exception when handling file watch on scripts directory", ex);
			} catch (InterruptedException ex) {
				logger.error("File watch on scripts directory was interrupted", ex);
			} catch (ClosedWatchServiceException ignored) {
				// expected when watch service is closed
			}
		}
	}
}
