package me.coley.recaf.util.struct;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Map implementation that allows registering listeners for map update calls.<br>
 * See:<ul>
 * <li>{@link #getPutListeners()}</li>
 * <li>{@link #getRemoveListeners()}</li>
 * </ul>
 *
 * @param <K> Key type of map.
 * @param <V> Value type of map.
 */
public class ListeningMap<K, V> implements Map<K, V> {
	private final Set<BiConsumer<K, V>> putListeners = new HashSet<>();
	private final Set<Consumer<Object>> removeListeners = new HashSet<>();
	private Map<K, V> backing;

	/**
	 * @param backing
	 * 		The map to contain the actual data.
	 */
	public void setBacking(Map<K, V> backing) {
		this.backing = backing;
	}

	/**
	 * @return {@code true} when the backing map is not null.
	 */
	public boolean isBacked() {
		return backing != null;
	}

	/**
	 * @return Set of listeners that are fed the key and value of items putted items.
	 */
	public Set<BiConsumer<K, V>> getPutListeners() {
		return putListeners;
	}

	/**
	 * @return Set of listeners that are fed keys or removed items.
	 */
	public Set<Consumer<Object>> getRemoveListeners() {
		return removeListeners;
	}

	@Override
	public V put(K key, V value) {
		putListeners.forEach(listener -> listener.accept(key, value));
		return backing.put(key, value);
	}

	@Override
	public V remove(Object key) {
		removeListeners.forEach(listener -> listener.accept(key));
		return backing.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for(Map.Entry<? extends K, ? extends V> e : m.entrySet())
			put(e.getKey(), e.getValue());
	}

	@Override
	public V get(Object key) {
		return backing.get(key);
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
	public void clear() {
		backing.clear();
	}

	@Override
	public Set<K> keySet() {
		return backing.keySet();
	}

	@Override
	public Collection<V> values() {
		return backing.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return backing.entrySet();
	}
}
