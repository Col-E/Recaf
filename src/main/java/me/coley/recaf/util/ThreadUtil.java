package me.coley.recaf.util;

import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Threading utils.
 *
 * @author Matt
 */
public class ThreadUtil {
	private static final ScheduledExecutorService scheduledService =
			Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
	private static final ExecutorService service = Executors.newWorkStealingPool();

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
	 */
	public static void runJfxDelayed(long time, Runnable consumer) {
		scheduledService.schedule(() -> Platform.runLater(consumer), time, TimeUnit.MILLISECONDS);
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			service.shutdownNow();
			scheduledService.shutdownNow();
		}));
	}
}
