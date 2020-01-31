package me.coley.recaf.util.struct;

import java.util.function.Supplier;

/**
 * A value that expires after a given threshold, prompting a new value to be supplied.
 *
 * @param <T>
 * 		Type to store.
 */
public class Expireable<T> {
	private final Supplier<T> getter;
	private long threshold;
	private long lastGet;
	private T value;

	/**
	 * Create an expirable value.
	 *
	 * @param threshold
	 * 		Time until the current value is invalidated.
	 * @param getter
	 * 		Supplier function for the value.
	 */
	public Expireable(long threshold, Supplier<T> getter) {
		this.threshold = threshold;
		this.getter = getter;
		this.value = getter.get();
		this.lastGet = System.currentTimeMillis();
	}

	/**
	 * @return Current value.
	 */
	public T get() {
		if(System.currentTimeMillis() - lastGet > threshold) {
			value = getter.get();
			lastGet = System.currentTimeMillis();
		}
		return value;
	}
}
