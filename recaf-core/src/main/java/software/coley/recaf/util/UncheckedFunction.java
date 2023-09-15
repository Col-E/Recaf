package software.coley.recaf.util;

import java.util.function.Function;

/**
 * Its {@link Function} but can throw an exception.
 *
 * @author xDark
 */
@FunctionalInterface
public interface UncheckedFunction<T, R> extends Function<T, R> {
	@Override
	default R apply(T t) {
		try {
			return uncheckedApply(t);
		} catch (Throwable th) {
			ReflectUtil.propagate(th);
			return null;
		}
	}

	/**
	 * @param input
	 * 		Function input.
	 *
	 * @return The function result.
	 *
	 * @throws Throwable
	 * 		Whenever.
	 */
	R uncheckedApply(T input) throws Throwable;
}
