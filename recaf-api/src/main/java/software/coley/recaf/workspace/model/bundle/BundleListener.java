package software.coley.recaf.workspace.model.bundle;

/**
 * Listener for updates to contents within a {@link Bundle}.
 *
 * @param <I>
 * 		Bundle item type.
 *
 * @author Matt Coley
 */
public interface BundleListener<I> {
	/**
	 * @param key
	 * 		Item key.
	 * @param value
	 * 		Item value.
	 */
	void onNewItem(String key, I value);

	/**
	 * @param key
	 * 		Item key.
	 * @param oldValue
	 * 		Prior item value.
	 * @param newValue
	 * 		New item value.
	 */
	void onUpdateItem(String key, I oldValue, I newValue);

	/**
	 * @param key
	 * 		Item key.
	 * @param value
	 * 		Item value.
	 */
	void onRemoveItem(String key, I value);
}
