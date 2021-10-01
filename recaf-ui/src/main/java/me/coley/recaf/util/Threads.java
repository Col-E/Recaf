package me.coley.recaf.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.application.Platform;
import javafx.concurrent.Task;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.concurrent.*;

/**
 * Thread utility.
 *
 * @author Matt Coley
 * @author xDark
 */
public class Threads {
	private static final Logger logger = Logging.get(Threads.class);
	private static final ScheduledExecutorService scheduledService =
			Executors.newScheduledThreadPool(threadCount(),
					new ThreadFactoryBuilder()
							.setNameFormat("Recaf Scheduler Thread #%d")
							.setDaemon(true).build());
	private static final ExecutorService service = Executors.newWorkStealingPool(threadCount());

	/**
	 * Run action in JavaFX thread.
	 *
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void runFx(Runnable action) {
		// I know "Platform.isFxApplicationThread()" exists.
		// That results in some wonky behavior in various use cases though.
		Platform.runLater(wrap(action));
	}


	/**
	 * Run action in JavaFX thread.
	 *
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in UI thread.
	 */
	public static void runFxDelayed(long delayMs, Runnable action) {
		runDelayed(delayMs, () -> Platform.runLater(wrap(action)));
	}


	/**
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Thread future.
	 */
	public static Future<?> run(Runnable action) {
		return service.submit(wrap(action));
	}

	/**
	 * @param action
	 * 		Task to start in new thread.
	 * @param <T>
	 * 		Type of task return value.
	 *
	 * @return Thread future.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Future<T> run(Task<T> action) {
		return (Future<T>) service.submit(wrap(action));
	}

	/**
	 * @param updateIntervalMs
	 * 		Time in milliseconds between each execution.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	public static ScheduledFuture<?> runRepeated(long updateIntervalMs, Runnable action) {
		return scheduledService.scheduleAtFixedRate(wrap(action), 0, updateIntervalMs,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * @param delayMs
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	public static Future<?> runDelayed(long delayMs, Runnable action) {
		return scheduledService.schedule(wrap(action), delayMs, TimeUnit.MILLISECONDS);
	}

	/**
	 * Run a given action with a timeout.
	 *
	 * @param time
	 * 		Timeout in milliseconds.
	 * @param action
	 * 		Runnable to execute.
	 *
	 * @return {@code true}
	 */
	public static boolean timeout(int time, Runnable action) {
		try {
			Future<?> future = run(action);
			future.get(time, TimeUnit.MILLISECONDS);
			return true;
		} catch (TimeoutException e) {
			// Expected: Timeout
			return false;
		} catch (Throwable t) {
			// Other error
			return true;
		}
	}

	/**
	 * Wrap action to handle error logging.
	 *
	 * @param action
	 * 		Action to run.
	 *
	 * @return Wrapper runnable.
	 */
	private static Runnable wrap(Runnable action) {
		return () -> {
			try {
				action.run();
			} catch (Throwable t) {
				logger.error("Unhandled exception on thread: " + Thread.currentThread().getName(), t);
			}
		};
	}

	/**
	 * Shutdowns executors.
	 */
	public static void shutdown() {
		logger.trace("Shutting down thread executors");
		service.shutdownNow();
		scheduledService.shutdownNow();
	}

	private static int threadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
