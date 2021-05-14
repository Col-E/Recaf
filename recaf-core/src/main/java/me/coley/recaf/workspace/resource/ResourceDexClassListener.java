package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.DexClassInfo;

/**
 * Listener for receiving dex class updates from a {@link Resource}.
 *
 * @author Matt Coley
 */
public interface ResourceDexClassListener {
	/**
	 * Called when a new class is added.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param dexName
	 * 		Name of dex file in resource affected.
	 * @param newValue
	 * 		Class added to the resource.
	 */
	void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue);

	/**
	 * Called when an old class is removed.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param dexName
	 * 		Name of dex file in resource affected.
	 * @param oldValue
	 * 		Class removed from the resource.
	 */
	void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue);

	/**
	 * Called when the old class is replaced by the new one.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param dexName
	 * 		Name of dex file in resource affected.
	 * @param oldValue
	 * 		Prior class value.
	 * @param newValue
	 * 		New class value.
	 */
	void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue);
}
