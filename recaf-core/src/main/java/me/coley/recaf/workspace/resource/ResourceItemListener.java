package me.coley.recaf.workspace.resource;

/**
 * Listener for receiving item update events.
 *
 * @param <I>
 * 		Item type implementation. Either {@link ClassInfo} or {@link FileInfo}.
 *
 * @author Matt Coley
 */
public interface ResourceItemListener<I extends ItemInfo> {
	/**
	 * Called when a new value is added.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param newValue
	 * 		Item added to the resource.
	 */
	void onNewItem(Resource resource, I newValue);

	/**
	 * Called when the old value is replaced by the new one.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Prior item value.
	 * @param newValue
	 * 		New item value.
	 */
	void onUpdateClass(Resource resource, I oldValue, I newValue);

	/**
	 * Called when an old value is removed.
	 *
	 * @param resource
	 * 		Resource affected.
	 * @param oldValue
	 * 		Item removed from the resource.
	 */
	void onRemoveItem(Resource resource, I oldValue);
}
