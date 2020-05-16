package me.coley.recaf.util.struct;

import javafx.application.Platform;
import me.coley.recaf.util.ThreadUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utility to run consumers on a given thread.
 *
 * @param <T>
 * 		Type of content.
 *
 * @author Matt
 */
public class ThreadAction<T> {
	private Supplier<T> supplier;
	private Consumer<T> consumer;
	private boolean consumerFx;

	/**
	 * @param <T>
	 * 		Type of content.
	 *
	 * @return Built action.
	 */
	public static <T> ThreadAction<T> create() {
		return new ThreadAction<>();
	}

	/**
	 * @param supplier
	 * 		Content source.
	 *
	 * @return Built action.
	 */
	public ThreadAction<T> supplier(Supplier<T> supplier) {
		this.supplier = supplier;
		return this;
	}

	/**
	 * @param consumer
	 * 		Action to run on content.
	 *
	 * @return Built action.
	 */
	public ThreadAction<T> consumer(Consumer<T> consumer) {
		this.consumer = consumer;
		return this;
	}

	/**
	 * Set consumer action to run on JavaFx thread.
	 *
	 * @return Built action.
	 */
	public ThreadAction<T> onUi() {
		this.consumerFx = true;
		return this;
	}

	/**
	 * Run 'em.
	 */
	public void run() {
		ThreadUtil.run(() -> {
			T value = supplier.get();
			if(consumer != null) {
				if(consumerFx) {
					Platform.runLater(() -> consumer.accept(value));
				} else {
					consumer.accept(value);
				}
			}
		});
	}
}