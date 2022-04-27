package me.coley.recaf.util;

/**
 * Its {@link java.util.function.Consumer} but can throw an exception.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface ErrorableConsumer<T> {
	/**
	 * @param input
	 * 		Consumer input.
	 *
	 * @throws Throwable
	 * 		Whenever.
	 */
	void accept(T input) throws Throwable;
}
