package me.coley.recaf.ui.component.constructor;

/**
 * Component to use in editor list-views.
 * 
 * @author Matt
 *
 * @param <T>
 */
public interface Constructor<T> {
	/**
	 * Construct the object of type T and do some action with it.
	 */
	default void finish() {}

	/**
	 * @return Object of T with current values pulled from child components.
	 */
	T get();

	/**
	 * Clear inputs.
	 */
	void reset();

}
