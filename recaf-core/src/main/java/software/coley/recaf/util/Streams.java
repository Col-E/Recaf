package software.coley.recaf.util;

import software.coley.recaf.util.threading.CountDown;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Stream utilities.
 *
 * @author xDark
 */
public final class Streams {
	private Streams() {
	}

	/**
	 * Combines the given streams.
	 *
	 * @param streams
	 * 		Streams to combine.
	 * @param <T>
	 * 		Stream element type.
	 *
	 * @return Combined stream.
	 */
	public static <T> Stream<? extends T> of(Stream<? extends T>... streams) {
		Stream<? extends T> merged = null;
		for (Stream<? extends T> stream : streams) {
			if (merged == null) merged = stream;
			else merged = Stream.concat(merged, stream);
		}
		return merged == null ? Stream.empty() : merged;
	}

	/**
	 * Makes stream interruptable.
	 *
	 * @param stream
	 * 		Stream to make interruptable.
	 * @param <T>
	 * 		Stream element type.
	 *
	 * @return Interruptable stream.
	 */
	public static <T> Stream<T> interruptable(Stream<? extends T> stream) {
		Spliterator<? extends T> spliterator = stream.spliterator();
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(spliterator.estimateSize(), spliterator.characteristics()) {
			@Override
			public boolean tryAdvance(Consumer<? super T> action) {
				if (Thread.interrupted()) {
					return false;
				}
				return spliterator.tryAdvance(action);
			}

			@Override
			public void forEachRemaining(Consumer<? super T> action) {
				if (Thread.interrupted()) {
					return;
				}
				spliterator.forEachRemaining(action);
			}
		}, stream.isParallel());
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
		AtomicReference<Throwable> throwable = new AtomicReference<>();
		CountDown countDown = new CountDown();
		stream.forEach(x -> {
			Throwable thrown = throwable.get();
			if (thrown != null) {
				ReflectUtil.propagate(thrown);
			}
			countDown.register();
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
					countDown.release();
				}
			});
		});
		try {
			countDown.await();
		} catch (InterruptedException ignored) {
		}
		Throwable thrown = throwable.get();
		if (thrown != null) {
			ReflectUtil.propagate(thrown);
		}
	}

	/**
	 * Recursively traversed {@code seed}.
	 *
	 * @param seed
	 * 		Initial seed.
	 * @param fn
	 * 		Transforming function.
	 * @param <T>
	 * 		Element type.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(T seed, Function<? super T, Stream<? extends T>> fn) {
		return Stream.concat(
				Stream.of(seed),
				Stream.of(seed)
						.flatMap(fn)
						.flatMap(x -> recurse(x, fn)));
	}

	/**
	 * Recursively traversed {@code seed}.
	 *
	 * @param seed
	 * 		Initial stream.
	 * @param fn
	 * 		Transforming function.
	 * @param <T>
	 * 		Element type.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(Stream<? extends T> seed, Function<? super T, Stream<? extends T>> fn) {
		return seed.flatMap(x -> recurse(x, fn));
	}

	/**
	 * @param flatMap
	 * 		Transforming function.
	 * @param <T>
	 * 		Element type.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurseWithoutCycles(T seed, Function<T, Set<T>> flatMap) {
		Deque<Iterator<T>> vertices = new ArrayDeque<>();
		Set<T> visited = new HashSet<>();
		vertices.push(Collections.singletonList(seed).iterator());
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.IMMUTABLE | Spliterator.NONNULL) {
			@Override
			public boolean tryAdvance(Consumer<? super T> action) {
				while (true) {
					Iterator<T> iterator = vertices.peek();
					if (iterator == null) {
						return false;
					}
					if (!iterator.hasNext()) {
						vertices.poll();
						continue;
					}
					T vertex = iterator.next();
					if (visited.add(vertex)) {
						action.accept(vertex);
						vertices.push(flatMap.apply(vertex).iterator());
						return true;
					}
				}
			}
		}, false);
	}
}
