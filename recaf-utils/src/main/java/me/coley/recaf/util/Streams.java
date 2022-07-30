package me.coley.recaf.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Stream utilities.
 *
 * @author xDark
 */
public final class Streams {
	private Streams() {
	}

	/**
	 * Accepts all elements in the stream in the given executor.
	 *
	 * @param stream
	 * 		Stream to accept.
	 * @param consumer
	 * 		Element consumer.
	 * @param executor
	 * 		Executor to use.
	 * @param <T>
	 * 		Element type.
	 */
	public static <T> void forEachOn(Stream<T> stream, Consumer<? super T> consumer, Executor executor) {
		AtomicLong count = new AtomicLong(1L);
		AtomicReference<Throwable> throwable = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);
		stream.forEach(x -> {
			Throwable thrown = throwable.get();
			if (thrown != null) {
				ReflectUtil.propagate(thrown);
			}
			count.incrementAndGet();
			executor.execute(() -> {
				try {
					if (throwable.get() == null) {
						try {
							consumer.accept(x);
						} catch (Throwable t) {
							throwable.compareAndSet(null, t);
						}
					}
				} finally {
					if (count.decrementAndGet() == 0L) {
						latch.countDown();
					}
				}
			});
		});
		if (count.decrementAndGet() != 0L) {
			try {
				latch.await();
			} catch (InterruptedException ignored) {
			}
		}
		Throwable thrown = throwable.get();
		if (thrown != null) {
			ReflectUtil.propagate(thrown);
		}
	}

	/**
	 * Recursively traversed {@literal seed}.
	 *
	 * @param seed
	 * 		Initial seed.
	 * @param fn
	 * 		Transforming function.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(T seed, Function<? super T, Stream<? extends T>> fn) {
		return Stream.concat(Stream.of(seed), Stream.of(seed).flatMap(fn).flatMap(x -> recurse(x, fn)));
	}

	/**
	 * Recursively traversed {@literal seed}.
	 *
	 * @param seed
	 * 		Initial stream.
	 * @param fn
	 * 		Transforming function.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(Stream<? extends T> seed, Function<? super T, Stream<? extends T>> fn) {
		return seed.flatMap(x -> recurse(x, fn));
	}
}
