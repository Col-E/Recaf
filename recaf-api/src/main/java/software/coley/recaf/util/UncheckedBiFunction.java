package software.coley.recaf.util;

import java.util.function.BiFunction;

/**
 * Its {@link BiFunction} but can throw an exception.
 *
 * @author xDark
 */
@FunctionalInterface
public interface UncheckedBiFunction<T, U, R> extends BiFunction<T, U, R> {
	@Override
	default R apply(T t, U u) {
		try {
			return uncheckedApply(t, u);
		} catch (Throwable th) {
			ReflectUtil.propagate(th);
			return null;
		}
	}

	/**
	 * @param t
	 * 		First input.
	 * @param u
	 * 		Second input.
	 *
	 * @return The function result.
	 *
	 * @throws Throwable
	 * 		Whenever.
	 */
	R uncheckedApply(T t, U u) throws Throwable;
}
