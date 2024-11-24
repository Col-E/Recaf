package software.coley.recaf.workspace.model.bundle;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.Info;
import software.coley.recaf.workspace.model.resource.BasicWorkspaceResource;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Basic bundle implementation.
 *
 * @param <I>
 * 		Item type.
 *
 * @author Matt Coley
 */
public class BasicBundle<I extends Info> implements Bundle<I> {
	private static final Logger logger = Logging.get(BasicBundle.class);
	private final Map<String, Stack<I>> history = new ConcurrentHashMap<>();
	private final List<BundleListener<I>> listeners = new CopyOnWriteArrayList<>();
	private final Map<String, I> backing = new ConcurrentHashMap<>();
	private final Set<String> initialKeys = ConcurrentHashMap.newKeySet();
	private final NavigableSet<String> removed = Collections.synchronizedNavigableSet(new TreeSet<>());

	/**
	 * Create initial history item.
	 *
	 * @param info
	 * 		Origin item.
	 */
	private void initHistory(@Nonnull I info) {
		Stack<I> itemHistory = new Stack<>();
		itemHistory.push(info);
		history.put(info.getName(), itemHistory);
	}

	/**
	 * Utility call for {@link #put(String, Info)}, without invoking the listener.
	 *
	 * @param info
	 * 		Item to put.
	 *
	 * @see #markInitialState()
	 */
	public void initialPut(@Nonnull I info) {
		backing.put(info.getName(), info);
		initHistory(info);
	}

	/**
	 * Mark the current snapshot of items as the initial state of the bundle.
	 * <p>
	 * Called when {@link BasicWorkspaceResource} is constructed, so any bundle assigned to a resource should be
	 * automatically marked.
	 */
	public void markInitialState() {
		initialKeys.addAll(backing.keySet());
	}

	/**
	 * Utility call for {@link #put(String, Info)}
	 *
	 * @param info
	 * 		Item to put.
	 *
	 * @return Prior associated value, if any.
	 */
	@Override
	public I put(@Nonnull I info) {
		return put(info.getName(), info);
	}

	/**
	 * History contains a stack of prior states of items.
	 * If an item has not been modified there is no entry in this map.
	 *
	 * @return Map of historical states of items within this bundle.
	 */
	@Nonnull
	protected Map<String, Stack<I>> getHistory() {
		return history;
	}

	@Override
	public Stack<I> getHistory(@Nonnull String key) {
		return history.get(key);
	}

	@Nonnull
	@Override
	public Set<String> getDirtyKeys() {
		Set<String> dirty = new TreeSet<>();
		history.forEach((key, itemHistory) -> {
			if (itemHistory.size() > 1) {
				dirty.add(key);
			}
		});
		return dirty;
	}

	@Nonnull
	@Override
	public Set<String> getRemovedKeys() {
		return Collections.unmodifiableNavigableSet(removed);
	}

	@Override
	public boolean hasHistory(@Nonnull String key) {
		// History implies there are past entries for the current value, hence more than one entry.
		Stack<I> stack = history.get(key);
		return stack != null && stack.size() > 1;
	}

	@Override
	public void incrementHistory(@Nonnull I info) {
		String key = info.getName();
		Stack<I> itemHistory = getHistory(key);
		if (itemHistory == null) {
			throw new IllegalStateException("Failed history increment, no prior history to build on for: " + key);
		}
		// logger.debug("Increment history: {} - {} states", EscapeUtil.escapeCommon(key), itemHistory.size());
		itemHistory.push(info);
	}

	@Override
	public void decrementHistory(@Nonnull String key) {
		Stack<I> itemHistory = getHistory(key);
		if (itemHistory == null) {
			throw new IllegalStateException("Failed history decrement, no prior history to read from for: " + key);
		}
		int size = itemHistory.size();

		// Update map with prior entry
		I currentItem = get(key);
		I priorItem;
		if (size > 1) {
			itemHistory.pop(); // Pop current value off stack.
			priorItem = itemHistory.peek(); // Yield prior value.
		} else {
			priorItem = itemHistory.peek();
		}
		backing.put(key, priorItem);

		// Notify listeners
		Unchecked.checkedForEach(listeners, listener -> listener.onUpdateItem(key, currentItem, priorItem),
				(listener, t) -> logger.error("Exception thrown when decrementing bundle history", t));
	}

	@Override
	public void addBundleListener(@Nonnull BundleListener<I> listener) {
		listeners.add(listener);
	}

	@Override
	public void removeBundleListener(@Nonnull BundleListener<I> listener) {
		listeners.remove(listener);
	}

	@Override
	public Iterator<I> iterator() {
		return backing.values().iterator();
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
	public boolean containsKey(@Nonnull Object key) {
		return backing.containsKey(key);
	}

	@Override
	public boolean containsValue(@Nonnull Object value) {
		return backing.containsValue(value);
	}

	@Override
	public I get(@Nonnull Object key) {
		return backing.get(key);
	}

	@Override
	public I put(@Nonnull String key, @Nonnull I newValue) {
		I oldValue = backing.put(key, newValue);

		// Ensure we don't track entries by this name as 'removed'
		removed.remove(key);

		// Notify listeners
		Unchecked.checkedForEach(listeners, listener -> {
			if (oldValue == null) {
				listener.onNewItem(key, newValue);
			} else {
				listener.onUpdateItem(key, oldValue, newValue);
			}
		}, (listener, t) -> logger.error("Exception thrown when putting bundle item", t));

		// Update history
		if (oldValue == null) {
			initHistory(newValue);
		} else {
			incrementHistory(newValue);
		}
		return oldValue;
	}

	@Override
	public I remove(@Nonnull Object key) {
		I info = backing.remove(key);
		if (info != null) {
			String keyStr = (String) key;

			// Mark the entry key as being removed, but only if it was in the initial key-set.
			// Adding a file and removing it should not be tracked as a net-removal.
			if (initialKeys.contains(keyStr))
				removed.add(keyStr);

			// Notify listeners
			Unchecked.checkedForEach(listeners, listener -> listener.onRemoveItem(keyStr, info),
					(listener, t) -> logger.error("Exception thrown when removing bundle item", t));

			// Update history
			history.remove(key);
		}
		return info;
	}

	@Override
	public void putAll(@Nonnull Map<? extends String, ? extends I> map) {
		throw new UnsupportedOperationException("Bundles cannot use 'putAll'");
	}

	@Override
	public void clear() {
		removed.addAll(initialKeys);
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
	public void close() {
		listeners.clear();
		clear();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicBundle<?> other = (BasicBundle<?>) o;

		if (!history.equals(other.history)) return false;
		return backing.equals(other.backing);
	}

	@Override
	public int hashCode() {
		int result = history.hashCode();
		result = 31 * result + backing.hashCode();
		return result;
	}
}
