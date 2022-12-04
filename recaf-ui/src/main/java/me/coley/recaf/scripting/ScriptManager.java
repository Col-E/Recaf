package me.coley.recaf.scripting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.coley.recaf.util.Directories;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.ThreadPoolFactory;
import org.slf4j.Logger;
import software.coley.collections.observable.ObservableList;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Manages which scripts are available.
 *
 * @author yapht
 */
@ApplicationScoped
public class ScriptManager {
	private static final Logger logger = Logging.get(ScriptManager.class);
	private static final ExecutorService THREAD_POOL = ThreadPoolFactory.newSingleThreadExecutor("Script Manager");
	private final ObservableList<Script> scripts = new ObservableList<>();

	@Inject
	public ScriptManager() {
		scan();
	}

	public ObservableList<Script> getScripts() {
		return scripts;
	}

	/**
	 * Scan the 'scripts' directory for scripts.
	 *
	 * @return A list of scrips or {@code null} if none could be found
	 */
	private static List<Script> loadScripts() {
		try (Stream<Path> stream = Files.walk(Directories.getScriptsDirectory(), FileVisitOption.FOLLOW_LINKS)) {
			return stream.filter(f -> f.toString().endsWith(Script.EXTENSION))
					.map(Script::fromPath)
					.collect(Collectors.toList());
		} catch (IOException ex) {
			logger.error("Failed to fetch available scripts", ex);
			return null;
		}
	}

	/**
	 * Set up a watcher service to monitor changes in the scripts directory
	 */
	@SuppressWarnings("all")
	private void scan() {
		THREAD_POOL.submit(() -> {
			try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
				Directories.getScriptsDirectory().register(watchService, ENTRY_MODIFY, ENTRY_DELETE);
				while (true) {
					WatchKey wk = watchService.take();
					try {
						if (!wk.pollEvents().isEmpty()) {
							scripts.clear();
							List<Script> availableScripts = loadScripts();
							if (availableScripts != null)
								scripts.addAll(availableScripts);
						}
					} finally {
						wk.reset();
					}
				}
			} catch (IOException ex) {
				logger.error("Filesystem watch error", ex);
			} catch (InterruptedException ignored) {
			}
		});
	}
}
