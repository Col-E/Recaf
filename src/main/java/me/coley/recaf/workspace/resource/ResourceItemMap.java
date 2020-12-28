package me.coley.recaf.workspace.resource;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Forwarding map base for implementing children.
 *
 * @param <I>
 * 		Item type.
 *
 * @author Matt Coley
 * @see ClassMap
 * @see FileMap
 */
public class ResourceItemMap<I extends ItemInfo> implements Map<String, I> {
	private final Map<String, I> backing;
	private final Resource container;
	protected ResourceItemListener<I> listener;

	protected ResourceItemMap(Resource container, Map<String, I> backing) {
		this.container = container;
		this.backing = backing;
	}

	/**
	 * @param listener
	 * 		Item map listener instance.
	 */
	public void setListener(ResourceItemListener<I> listener) {
		this.listener = listener;
	}

	/**
	 * @return Item map listener instance.
	 */
	public ResourceItemListener<I> getListener() {
		return listener;
	}

	/**
	 * Rename an item in the map.
	 *
	 * @param key
	 * 		Old item key.
	 * @param newKey
	 * 		New item key.
	 *
	 * @return {@code true} for successful rename.
	 * {@code false} if it did not go through due to a conflict with the new key, or the old key not existing.
	 */
	public boolean rename(String key, String newKey) {
		if (containsKey(newKey) || !containsKey(key)) {
			return false;
		}
		I info = backing.remove(key);
		if (listener != null) {
			info = listener.onRenameItem(container, key, newKey, info);
		}
		backing.put(newKey, info);
		return true;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return backing.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return backing.containsValue(value);
	}

	@Override
	public I get(Object key) {
		return backing.get(key);
	}

	/**
	 * Utility call for {@link #put(String, ItemInfo)}, without invoking the listener.
	 *
	 * @param itemInfo
	 * 		Item to put.
	 */
	public void initialPut(I itemInfo) {
		backing.put(itemInfo.getName(), itemInfo);
	}

	/**
	 * Utility call for {@link #put(String, ItemInfo)}
	 *
	 * @param itemInfo
	 * 		Item to put.
	 *
	 * @return Prior associated value, if any.
	 */
	public I put(I itemInfo) {
		return put(itemInfo.getName(), itemInfo);
	}

	@Override
	public I put(String key, I itemInfo) {
		I info = backing.put(key, itemInfo);
		if (listener != null) {
			if (info == null) {
				listener.onNewItem(container, itemInfo);
			} else {
				listener.onUpdateClass(container, info, itemInfo);
			}
		}
		return info;
	}

	@Override
	public I remove(Object key) {
		I info = backing.remove(key);
		if (listener != null && info != null) {
			listener.onRemoveItem(container, info);
		}
		return info;
	}

	@Override
	public void putAll(Map<? extends String, ? extends I> map) {
		backing.putAll(map);
	}

	@Override
	public void clear() {
		backing.clear();
	}

	@Override
	public Set<String> keySet() {
		return backing.keySet();
	}

	@Override
	public Collection<I> values() {
		return values();
	}

	@Override
	public Set<Entry<String, I>> entrySet() {
		return entrySet();
	}
}
