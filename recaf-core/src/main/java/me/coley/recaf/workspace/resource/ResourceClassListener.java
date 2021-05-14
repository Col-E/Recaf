package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ClassInfo;

/**
 * Listener for receiving class updates from a {@link Resource}.
 *
 * @author Matt Coley
 */
public interface ResourceClassListener {
	/**
	 * Called when a new class is added.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param newValue
	 * 		Class added to the resource.
	 */
	void onNewClass(Resource resource, ClassInfo newValue);

	/**
	 * Called when an old class is removed.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Class removed from the resource.
	 */
	void onRemoveClass(Resource resource, ClassInfo oldValue);

	/**
	 * Called when the old class is replaced by the new one.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Prior class value.
	 * @param newValue
	 * 		New class value.
	 */
	void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue);
}
