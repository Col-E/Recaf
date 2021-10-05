package me.coley.recaf.ui.behavior;

/**
 * Represents a UI component that may have some back-end resources we want to clean up when they
 * are no longer being used.
 *
 * @author Matt Coley
 */
public interface Cleanable {
	/**
	 * Closes any resources the class is using.
	 */
	void cleanup();
}
