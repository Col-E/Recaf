package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	public static <T> T cast(@Nullable Object value) {
		return (T) value;
	}

	/**
	 * @param runnable
	 * 		Runnable.
	 */
	public static void run(@Nonnull UncheckedRunnable runnable) {
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
	public static <T> T get(@Nonnull UncheckedSupplier<T> supplier) {
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
	public static <T> T getOr(@Nullable UncheckedSupplier<T> supplier, T fallback) {
		if (supplier == null)
			return fallback;
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
	public static <T> void accept(@Nonnull UncheckedConsumer<T> consumer, T value) {
		consumer.accept(value);
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
	public static <T, U> void baccept(@Nonnull UncheckedBiConsumer<T, U> consumer, T t, U u) {
		consumer.accept(t, u);
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
	public static <T, R> R map(@Nonnull UncheckedFunction<T, R> fn, T value) {
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
	public static <T, U, R> R bmap(@Nonnull UncheckedBiFunction<T, U, R> fn, T t, U u) {
		return fn.apply(t, u);
	}

	/**
	 * Helper method to created unchecked runnable.
	 *
	 * @param runnable
	 * 		Unchecked runnable.
	 *
	 * @return Unchecked runnable.
	 */
	@Nonnull
	public static Runnable runnable(@Nonnull UncheckedRunnable runnable) {
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
	@Nonnull
	public static <T> Supplier<T> supply(@Nonnull UncheckedSupplier<T> supplier) {
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
	@Nonnull
	public static <T> Consumer<T> consumer(@Nonnull UncheckedConsumer<T> consumer) {
		return consumer;
	}

	/**
	 * Helper method to created unchecked consumer.
	 *
	 * @param consumer
	 * 		Unchecked consumer.
	 *
	 * @return Unchecked consumer.
	 */
	@Nonnull
	public static <T, U> BiConsumer<T, U> bconsumer(@Nonnull UncheckedBiConsumer<T, U> consumer) {
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
	@Nonnull
	public static <T, R> Function<T, R> function(@Nonnull UncheckedFunction<T, R> fn) {
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
	@Nonnull
	public static <T, U, R> BiFunction<T, U, R> bfunction(@Nonnull UncheckedBiFunction<T, U, R> fn) {
		return fn;
	}
}
