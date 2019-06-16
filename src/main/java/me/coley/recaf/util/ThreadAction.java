package me.coley.recaf.util;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ThreadAction<T> {
	private Supplier<T> supplier;
	private Consumer<T> consumer;
	private boolean consumerFx;

	public static <T> ThreadAction<T> create() {
		return new ThreadAction<>();
	}

	public ThreadAction<T> supplier(Supplier<T> supplier) {
		this.supplier = supplier;
		return this;
	}

	public ThreadAction<T> consumer(Consumer<T> consumer) {
		this.consumer = consumer;
		return this;
	}

	public ThreadAction<T> onUi() {
		this.consumerFx = true;
		return this;
	}

	public void run() {
		Threads.run(() -> {
			T value = supplier.get();
			if(consumer != null) {
				if (consumerFx) {
					Threads.runFx(() -> consumer.accept(value));
				} else {
					consumer.accept(value);
				}
			}
		});
	}
}
