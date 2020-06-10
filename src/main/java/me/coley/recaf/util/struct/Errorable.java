package me.coley.recaf.util.struct;

/**
 * Essentially {@link Runnable} but can throw an exception.
 *
 * @param <T>
 * 		Type of exception.
 *
 * @author Matt
 */
public interface Errorable<T extends Throwable> {
	/**
	 * @throws T
	 * 		Exception thrown.
	 */
	void run() throws T;
}
