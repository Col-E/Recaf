package software.coley.recaf.util;

import java.util.function.Consumer;

/**
 * Its {@link Consumer} but can throw an exception.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface UncheckedConsumer<T> extends Consumer<T> {
	@Override
	default void accept(T t) {
		try {
			uncheckedAccept(t);
		} catch (Throwable th) {
			ReflectUtil.propagate(th);
		}
	}

	/**
	 * @param input
	 * 		Consumer input.
	 *
	 * @throws Throwable
	 * 		Whenever.
	 */
	void uncheckedAccept(T input) throws Throwable;
}
