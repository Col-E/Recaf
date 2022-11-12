package me.coley.recaf.workspace.resource;

import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.util.EscapeUtil;
import me.coley.recaf.util.logging.Logging;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Stream;

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
public class ResourceItemMap<I extends ItemInfo> implements Map<String, I>, Iterable<I> {
	private final Logger logger = Logging.get(getClass());
	private final List<CommonItemListener<I>> listeners = new ArrayList<>();
	private final Map<String, Stack<I>> history = new HashMap<>();
	private final Map<String, I> backing;
	private final Resource container;

	protected ResourceItemMap(Resource container, Map<String, I> backing) {
		this.container = container;
		this.backing = backing;
	}

	/**
	 * @param listener
	 * 		Item map listener instance to add.
	 */
	public void addListener(CommonItemListener<I> listener) {
		listeners.add(listener);
	}

	/**
	 * @param listener
	 * 		Item map listener to remove.
	 */
	public void removeListener(CommonItemListener<I> listener) {
		listeners.remove(listener);
	}

	/**
	 * @return Item map listener instance.
	 */
	protected List<CommonItemListener<I>> getListeners() {
		return listeners;
	}

	/**
	 * @return Set of items modified since initialization.
	 */
	public Set<String> getDirtyItems() {
		Set<String> dirty = new TreeSet<>();
		history.forEach((key, itemHistory) -> {
			if (itemHistory.size() > 1) {
				dirty.add(key);
			}
		});
		return dirty;
	}

	/**
	 * @param key
	 * 		Item key.
	 *
	 * @return {@code true} when the item has at least one history entry.
	 */
	public boolean hasHistory(String key) {
		return getHistory(key) != null;
	}

	/**
	 * History stack for the given item key.
	 *
	 * @param key
	 * 		Item key.
	 *
	 * @return History of item.
	 */
	public Stack<I> getHistory(String key) {
		return history.get(key);
	}

	/**
	 * Create initial history item.
	 *
	 * @param itemInfo
	 * 		Origin item.
	 */
	private void initHistory(I itemInfo) {
		Stack<I> itemHistory = new Stack<>();
		itemHistory.push(itemInfo);
		history.put(itemInfo.getName(), itemHistory);
	}

	/**
	 * Add the value to the history.
	 *
	 * @param itemInfo
	 * 		Value to add.
	 */
	public void incrementHistory(I itemInfo) {
		String key = itemInfo.getName();
		Stack<I> itemHistory = getHistory(key);
		if (itemHistory == null) {
			throw new IllegalStateException("Failed history increment, no prior history to build on for: " + key);
		}
		int size = itemHistory.size();
		logger.debug("Increment history: {} - {} states", EscapeUtil.escapeCommon(key), size);
		itemHistory.push(itemInfo);
	}

	/**
	 * Decrement the history of the value associated with the key.
	 * The value removed in this operation then replaces the value of the item map.
	 *
	 * @param key
	 * 		Item key.
	 */
	public void decrementHistory(String key) {
		Stack<I> itemHistory = getHistory(key);
		if (itemHistory == null) {
			throw new IllegalStateException("Failed history decrement, no prior history to read from for: " + key);
		}
		int size = itemHistory.size();
		logger.debug("Decrement history: {} - {} states", key, size);
		// Update map with prior entry
		I currentItem = get(key);
		I priorItem;
		if (size > 1) {
			priorItem = itemHistory.pop();
		} else {
			priorItem = itemHistory.peek();
		}
		backing.put(key, priorItem);
		// Notify listener
		for (CommonItemListener<I> listener : listeners) {
			try {
				listener.onUpdateItem(container, currentItem, priorItem);
			} catch (Throwable t) {
				logger.error("Uncaught error in resource listener (revert)", t);
			}
		}
	}

	/**
	 * Wipe history of item.
	 *
	 * @param key
	 * 		Item key.
	 */
	private void removeHistory(String key) {
		history.remove(key);
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
		initHistory(itemInfo);
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

	/**
	 * @return Stream of items.
	 */
	public Stream<I> stream() {
		return values().stream();
	}

	@Override
	public I put(String key, I itemInfo) {
		I info = backing.put(key, itemInfo);
		// Notify listener
		for (CommonItemListener<I> listener : listeners) {
			try {
				if (info == null) {
					listener.onNewItem(container, itemInfo);
				} else {
					listener.onUpdateItem(container, info, itemInfo);
				}
			} catch (Throwable t) {
				logger.error("Uncaught error in resource listener (put)", t);
			}
		}
		// Update history
		if (info == null) {
			initHistory(itemInfo);
		} else {
			incrementHistory(itemInfo);
		}
		return info;
	}

	@Override
	public I remove(Object key) {
		I info = backing.remove(key);
		if (info != null) {
			// Notify listener
			for (CommonItemListener<I> listener : listeners) {
				try {
					listener.onRemoveItem(container, info);
				} catch (Throwable t) {
					logger.error("Uncaught error in resource listener (remove)", t);
				}
			}
			// Update history
			removeHistory(info.getName());
		}
		return info;
	}

	@Override
	public void putAll(Map<? extends String, ? extends I> map) {
		throw new UnsupportedOperationException("Resource item maps cannot use 'putAll'");
	}

	@Override
	public void clear() {
		backing.clear();
		history.clear();
	}

	@Override
	public Set<String> keySet() {
		return backing.keySet();
	}

	@Override
	public Collection<I> values() {
		return backing.values();
	}

	@Override
	public Set<Entry<String, I>> entrySet() {
		return backing.entrySet();
	}

	@Override
	public Iterator<I> iterator() {
		return backing.values().iterator();
	}
}
