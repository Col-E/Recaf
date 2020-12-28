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
	 * 		Item added to the resource.
	 */
	void onRemoveItem(Resource resource, I oldValue);

	/**
	 * Called when an item is renamed.
	 *
	 * @param container
	 * 		Resource affected.
	 * @param oldKey
	 * 		Old name of the item.
	 * @param newKey
	 * 		New name of the item.
	 * @param oldValue
	 * 		Item value being renamed.
	 *
	 * @return The new value, with references updated for the new path.
	 */
	I onRenameItem(Resource container, String oldKey, String newKey, I oldValue);
}
