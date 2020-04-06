package me.coley.recaf.util;

import javafx.application.Platform;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Threading utils.
 *
 * @author Matt
 */
public class ThreadUtil {
	private static ExecutorService service = Executors.newWorkStealingPool();

	/**
	 * @param supplier
	 * 		Value generator, run on a non-jfx thread.
	 * @param consumer
	 * 		JavaFx consumer thread, takes the supplied value.
	 * @param <T>
	 * 		Type of value.
	 */
	public static <T> void runJfx(Supplier<T> supplier, Consumer<T> consumer) {
		runJfx(supplier, Long.MAX_VALUE, null, consumer, null);
	}

	/**
	 * @param supplier
	 * 		Value generator, run on a non-jfx thread.
	 * @param supplierTimeout
	 * 		Time to wait on the supplier generating a value before aborting the task.
	 * @param consumer
	 * 		JavaFx consumer thread, takes the supplied value.
	 * @param handler
	 * 		Error handling.
	 * @param <T>
	 * 		Type of value.
	 */
	public static <T> void runJfx(Supplier<T> supplier, long supplierTimeout,  Runnable timeoutAction, Consumer<T> consumer, Consumer<Throwable> handler) {
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
	 * 		JavaFx action thread.
	 */
	public static void runJfxDelayed(long time, Runnable consumer) {
		new Thread(() -> {
			try {
				Thread.sleep(time);
			} catch(InterruptedException e) { /* ignored */ }
			Platform.runLater(consumer);
		}).start();
	}

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			service.shutdownNow();
		}));
	}
}
