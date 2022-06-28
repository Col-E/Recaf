package me.coley.recaf.util;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Recursion utilities.
 *
 * @author xDark
 */
public final class Recurse {
	private Recurse() {
	}

	/**
	 * Recursively traversed {@literal seed}.
	 *
	 * @param seed Initial seed.
	 * @param fn Transforming function.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(T seed, Function<? super T, Stream<? extends T>> fn) {
		return Stream.concat(Stream.of(seed), Stream.of(seed).flatMap(fn).flatMap(x -> recurse(x, fn)));
	}

	/**
	 * Recursively traversed {@literal seed}.
	 *
	 * @param seed Initial stream.
	 * @param fn Transforming function.
	 *
	 * @return Stream containing all traversed elements.
	 */
	public static <T> Stream<T> recurse(Stream<? extends T> seed, Function<? super T, Stream<? extends T>> fn) {
		return seed.flatMap(x -> recurse(x, fn));
	}
}
