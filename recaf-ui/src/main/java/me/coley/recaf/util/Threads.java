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
							.setNameFormat("Recaf Thread #%d")
							.setDaemon(true).build());
	private static final Executor jfxExecutor = Threads::runFx;

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
		return scheduledService.submit(wrap(action));
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
		return (Future<T>) scheduledService.submit(wrap(action));
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
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int time, Runnable action) {
		try {
			Future<?> future = run(action);
			return timeout(time, future);
		} catch (Throwable t) {
			// Can be thrown by execution timeout
			return false;
		}
	}

	/**
	 * Give a thread future a time limit.
	 *
	 * @param time
	 * 		Timeout in milliseconds.
	 * @param future
	 * 		Thread future being run.
	 *
	 * @return {@code true} When thread completed before time.
	 */
	public static boolean timeout(int time, Future<?> future) {
		try {
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
	 * Submits a periodic action that becomes enabled first after the given initial delay,
	 * and subsequently with the given period.
	 *
	 * @param task
	 * 		Task to execute.
	 * @param initialDelay
	 * 		The time to delay first execution.
	 * @param period
	 * 		The period between successive executions.
	 * @param unit
	 * 		The time unit of the initialDelay
	 * 		and period parameters.
	 *
	 * @return future representing completion of the tasks.
	 *
	 * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
	 */
	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay,
														 long period, TimeUnit unit) {
		return scheduledService.scheduleAtFixedRate(task, initialDelay, period, unit);
	}

	/**
	 * @return that executes it's tasks
	 * in JavaFX thread.
	 */
	public static Executor jfxExecutor() {
		return jfxExecutor;
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
		scheduledService.shutdownNow();
	}

	private static int threadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
