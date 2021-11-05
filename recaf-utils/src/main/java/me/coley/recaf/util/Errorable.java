package me.coley.recaf.util;

/**
 * Its {@link Runnable} but can throw an exception.
 *
 * @author Matt Coley
 */
@FunctionalInterface
public interface Errorable {
	/**
	 * @throws Throwable
	 * 		Whenever.
	 */
	void run() throws Throwable;
}
