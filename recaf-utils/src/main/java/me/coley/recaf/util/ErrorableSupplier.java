package me.coley.recaf.util;

/**
 * Its {@link java.util.function.Supplier} but can throw an exception.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface ErrorableSupplier<T> {
	/**
	 * @return Supplier output.
	 *
	 * @throws Throwable
	 * 		Whenever.
	 */
	T get() throws Throwable;
}
