package software.coley.recaf.workspace.model.bundle;


import jakarta.annotation.Nonnull;
import software.coley.recaf.behavior.PrioritySortable;

/**
 * Listener for updates to contents within a {@link Bundle}.
 *
 * @param <I>
 * 		Bundle item type.
 *
 * @author Matt Coley
 */
public interface BundleListener<I> extends PrioritySortable {
	/**
	 * @param key
	 * 		Item key.
	 * @param value
	 * 		Item value.
	 */
	void onNewItem(@Nonnull String key, @Nonnull I value);

	/**
	 * @param key
	 * 		Item key.
	 * @param oldValue
	 * 		Prior item value.
	 * @param newValue
	 * 		New item value.
	 */
	void onUpdateItem(@Nonnull String key, @Nonnull I oldValue, @Nonnull I newValue);

	/**
	 * @param key
	 * 		Item key.
	 * @param value
	 * 		Item value.
	 */
	void onRemoveItem(@Nonnull String key, @Nonnull I value);
}
