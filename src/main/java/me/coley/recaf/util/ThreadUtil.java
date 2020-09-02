package me.coley.recaf.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.coley.recaf.util.Log.*;

/**
 * Threading utils.
 *
 * @author Matt
 */
public class ThreadUtil {
	private static final ScheduledExecutorService scheduledService =
			Executors.newScheduledThreadPool(threadCount(),
					new ThreadFactoryBuilder()
							.setNameFormat("Recaf Scheduler Thread #%d")
							.setDaemon(true).build());
	private static final ExecutorService service = Executors.newWorkStealingPool(threadCount());

	/**
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Thread future.
	 */
	public static Future<?> run(Runnable action) {
		return service.submit(action);
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
		return (Future<T>) service.submit(action);
	}

	/**
	 * @param updateInterval
	 * 		Time in milliseconds between each execution.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	public static ScheduledFuture<?> runRepeated(long updateInterval, Runnable action) {
		return scheduledService.scheduleAtFixedRate(action, 0, updateInterval,
				TimeUnit.MILLISECONDS);
	}

	/**
	 * @param time
	 * 		Delay to wait in milliseconds.
	 * @param action
	 * 		Runnable to start in new thread.
	 *
	 * @return Scheduled future.
	 */
	public static Future<?> runDelayed(long time, Runnable action) {
		return scheduledService.schedule(action, time, TimeUnit.MILLISECONDS);
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
		} catch(TimeoutException e) {
			// Expected: Timeout
			return false;
		} catch(Throwable t) {
			// Other error
			return true;
		}
	}

	/**
	 * @param supplier
	 * 		Value generator, run on a non-jfx thread.
	 * @param consumer
	 * 		JavaFx consumer thread, takes the supplied value.
	 * @param <T>
	 * 		Type of value.
	 */
	public static <T> void runSupplyConsumer(Supplier<T> supplier, Consumer<T> consumer) {
		runSupplyConsumer(supplier, Long.MAX_VALUE, null, consumer, null);
	}

	/**
	 * @param supplier
	 * 		Value generator, run on a non-jfx thread.
	 * @param supplierTimeout
	 * 		Time to wait on the supplier generating a value before aborting the task.
	 * @param timeoutAction
	 * 		Action to run when timeout is reached.
	 * @param consumer
	 * 		JavaFx consumer thread, takes the supplied value.
	 * @param handler
	 * 		Error handling.
	 * @param <T>
	 * 		Type of value.
	 */
	public static <T> void runSupplyConsumer(Supplier<T> supplier, long supplierTimeout, Runnable timeoutAction,
											 Consumer<T> consumer, Consumer<Throwable> handler) {
		new Thread(() -> {
			try {
				// Attempt to compute value within given time
				Future<T> future = service.submit(supplier::get);
				T value = future.get(supplierTimeout, TimeUnit.MILLISECONDS);
				// Execute action with value
				Platform.runLater(() -> consumer.accept(value));
			} catch(CancellationException | InterruptedException | TimeoutException r) {
				// Timed out
				if (timeoutAction != null)
					timeoutAction.run();
			} catch(ExecutionException e) {
				// Supplier encountered an error
				// - Actual cause is wrapped twice
				Throwable cause = e.getCause().getCause();
				if(handler != null)
					handler.accept(cause);
			}
			catch(Throwable t) {
				// Unknown error
				if(handler != null)
					handler.accept(t);
			}
		}).start();
	}

	/**
	 * @param time
	 * 		Delay to wait in milliseconds.
	 * @param consumer
	 * 		JavaFx runnable action.
	 *
	 * @return Scheduled future.
	 */
	public static Future<?> runJfxDelayed(long time, Runnable consumer) {
		return scheduledService.schedule(() -> Platform.runLater(consumer), time, TimeUnit.MILLISECONDS);
	}

	/**
	 * @param consumer
	 * 		JavaFx runnable action.
	 */
	public static void checkJfxAndEnqueue(Runnable consumer) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(consumer);
		} else {
			consumer.run();
		}
	}

	/**
	 * Shutdowns executors.
	 */
	public static void shutdown() {
		trace("Shutting down thread executors");
		service.shutdownNow();
		scheduledService.shutdownNow();
	}

	private static int threadCount() {
		return Runtime.getRuntime().availableProcessors();
	}
}
