package me.coley.recaf.ui.component;

import javafx.scene.layout.BorderPane;

/**
 * Component to use in editor list-views.
 * 
 * @author Matt
 *
 * @param <T>
 */
public abstract class Constructor<T> extends BorderPane {
	/**
	 * Construct the object of type T and do some action with it.
	 */
	public abstract void finish();

	/**
	 * @return Object of T with current values pulled from child components.
	 */
	protected abstract T get();

	/**
	 * Clear inputs.
	 */
	protected abstract void reset();

}
