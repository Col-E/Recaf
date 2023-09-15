package software.coley.recaf.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Memoized functions.
 *
 * @author Amejonah
 */
public class MemoizedFunctions {
	public static <Key, Value> Function<Key, Value> memoize(Function<Key, Value> function) {
		return new MemoizedFunction<>(function);
	}

	public static <KeyA, KeyB, Value> BiFunction<KeyA, KeyB, Value> memoize(BiFunction<KeyA, KeyB, Value> function) {
		return new BiMemoizedFunction<>(function);
	}

	private static class MemoizedFunction<Key, Value> implements Function<Key, Value> {
		private final Map<Key, Value> cache = new HashMap<>();
		private final Function<Key, Value> function;

		private MemoizedFunction(Function<Key, Value> function) {
			this.function = function;
		}

		@Override
		public Value apply(Key key) {
			return cache.computeIfAbsent(key, function);
		}
	}

	private static class BiMemoizedFunction<KeyA, KeyB, Value> implements BiFunction<KeyA, KeyB, Value> {
		private final Map<KeyA, Map<KeyB, Value>> cache = new HashMap<>();
		private final BiFunction<KeyA, KeyB, Value> function;

		private BiMemoizedFunction(BiFunction<KeyA, KeyB, Value> function) {
			this.function = function;
		}


		@Override
		public Value apply(KeyA keyA, KeyB keyB) {
			return cache.computeIfAbsent(keyA, __ -> new HashMap<>()).computeIfAbsent(keyB, k -> function.apply(keyA, keyB));
		}
	}
}