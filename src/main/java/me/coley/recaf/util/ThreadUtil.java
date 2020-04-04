package me.coley.recaf.util;

import javafx.application.Platform;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Threading utils.
 *
 * @author Matt
 */
public class ThreadUtil {
	/**
	 * @param supplier Value generator, run on a non-jfx thread.
	 * @param consumer JavaFx consumer thread, takes the supplied value.
	 * @param <T> Type of value.
	 */
	public static <T> void runJfx(Supplier<T> supplier, Consumer<T> consumer) {
		new Thread(() -> {
			T value = supplier.get();
			Platform.runLater(() -> consumer.accept(value));
		}).start();
	}

	/**
	 * @param time Delay to wait in milliseconds.
	 * @param consumer JavaFx action thread.
	 */
	public static void runJfxDelayed(long time, Runnable consumer) {
		new Thread(() -> {
			try {
				Thread.sleep(time);
			} catch(InterruptedException e) { /* ignored */ }
			Platform.runLater(consumer);
		}).start();
	}
}
