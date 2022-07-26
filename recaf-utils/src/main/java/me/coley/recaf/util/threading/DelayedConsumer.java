package me.coley.recaf.util.threading;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link Consumer} implementation of {@link DelayedExecutor}.
 *
 * @author Matt Coley
 */
public class DelayedConsumer<T> extends DelayedExecutor {
	private final Consumer<T> consumer;
	private final Supplier<T> supplier;

	/**
	 * @param duration
	 * 		Delay duration.
	 * @param consumer
	 * 		Task to run after the delay.
	 * @param supplier
	 * 		Argument supplier for the consumer.
	 */
	public DelayedConsumer(long duration, Supplier<T> supplier, Consumer<T> consumer) {
		super(duration);
		this.consumer = consumer;
		this.supplier = supplier;
	}

	@Override
	protected void execute() {
		consumer.accept(supplier.get());
	}
}
