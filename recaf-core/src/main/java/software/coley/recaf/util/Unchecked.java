package software.coley.recaf.util;

import java.util.function.*;

/**
 * Convenience calls for the error-able lambda types.
 *
 * @author Matt Coley
 */
public class Unchecked {
	/**
	 * @param value
	 * 		Value to cast.
	 * @param <T>
	 * 		Target type.
	 *
	 * @return Value casted.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object value) {
		return (T) value;
	}

	/**
	 * @param runnable
	 * 		Runnable.
	 */
	public static void run(UncheckedRunnable runnable) {
		runnable.run();
	}

	/**
	 * @param supplier
	 * 		Supplier.
	 * @param <T>
	 * 		Supplier type.
	 *
	 * @return Supplied value.
	 */
	public static <T> T get(UncheckedSupplier<T> supplier) {
		return supplier.get();
	}

	/**
	 * @param supplier
	 * 		Supplier.
	 * @param fallback
	 * 		Value to return if supplier fails.
	 * @param <T>
	 * 		Supplier type.
	 *
	 * @return Supplied value, or fallback if supplier failed.
	 */
	public static <T> T getOr(UncheckedSupplier<T> supplier, T fallback) {
		try {
			return supplier.get();
		} catch (Throwable t) {
			return fallback;
		}
	}

	/**
	 * @param consumer
	 * 		Consumer.
	 * @param value
	 * 		Consumed value.
	 * @param <T>
	 * 		Consumer type.
	 */
	public static <T> void accept(UncheckedConsumer<T> consumer, T value) {
		consumer.accept(value);
	}

	/**
	 * @param fn
	 * 		Function.
	 * @param value
	 * 		Function value.
	 * @param <T>
	 * 		Input type.
	 * @param <R>
	 * 		Output type.
	 */
	public static <T, R> R map(UncheckedFunction<T, R> fn, T value) {
		return fn.apply(value);
	}

	/**
	 * @param fn
	 * 		Function.
	 * @param t
	 * 		First function value.
	 * @param u
	 * 		Second function value.
	 * @param <T>
	 * 		First input type.
	 * @param <U>
	 * 		Second input type.
	 * @param <R>
	 * 		Output type.
	 */
	public static <T, U, R> R bmap(UncheckedBiFunction<T, U, R> fn, T t, U u) {
		return fn.apply(t, u);
	}

	/**
	 * @param consumer
	 * 		Consumer.
	 * @param t
	 * 		First value.
	 * @param u
	 * 		Second value.
	 * @param <T>
	 * 		First type.
	 * @param <U>
	 * 		Second type.
	 */
	public static <T, U> void baccept(UncheckedBiConsumer<T, U> consumer, T t, U u) {
		consumer.accept(t, u);
	}

	/**
	 * Helper method to created unchecked runnable.
	 *
	 * @param runnable
	 * 		Unchecked runnable.
	 *
	 * @return Unchecked runnable.
	 */
	public static Runnable runnable(UncheckedRunnable runnable) {
		return runnable;
	}

	/**
	 * Helper method to created unchecked supplier.
	 *
	 * @param supplier
	 * 		Unchecked supplier.
	 *
	 * @return Unchecked supplier.
	 */
	public static <T> Supplier<T> supply(UncheckedSupplier<T> supplier) {
		return supplier;
	}

	/**
	 * Helper method to created unchecked consumer.
	 *
	 * @param consumer
	 * 		Unchecked consumer.
	 *
	 * @return Unchecked consumer.
	 */
	public static <T> Consumer<T> consumer(UncheckedConsumer<T> consumer) {
		return consumer;
	}

	/**
	 * Helper method to created unchecked function.
	 *
	 * @param fn
	 * 		Unchecked function.
	 *
	 * @return Unchecked function.
	 */
	public static <T, R> Function<T, R> function(UncheckedFunction<T, R> fn) {
		return fn;
	}

	/**
	 * Helper method to created unchecked function.
	 *
	 * @param fn
	 * 		Unchecked function.
	 *
	 * @return Unchecked function.
	 */
	public static <T, U, R> BiFunction<T, U, R> bfunction(UncheckedBiFunction<T, U, R> fn) {
		return fn;
	}

	/**
	 * Helper method to created unchecked consumer.
	 *
	 * @param consumer
	 * 		Unchecked consumer.
	 *
	 * @return Unchecked consumer.
	 */
	public static <T, U> BiConsumer<T, U> bconsumer(UncheckedBiConsumer<T, U> consumer) {
		return consumer;
	}
}
