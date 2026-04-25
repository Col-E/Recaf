package software.coley.recaf.util.collect.primitive;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;

/**
 * A {@link Map}-like collection for {@code int}-{@code Object} key-value pairs.
 *
 * @author Matt Coley
 */
public class Int2ObjectMap<V> extends AbstractIntKeyMap {
	private Object[] values;

	/**
	 * New map with default capacity of 16.
	 */
	public Int2ObjectMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * New map with specified initial capacity.
	 *
	 * @param initialCapacity
	 * 		Initial capacity of the map.
	 */
	public Int2ObjectMap(int initialCapacity) {
		super(initialCapacity);
		values = new Object[keys.length];
	}

	/**
	 * @param consumer
	 * 		Consumer to accept all key-value pairs in the map.
	 */
	public void forEach(IntObjectConsumer<V> consumer) {
		for (int i = 0; i < keys.length; i++)
			if (occupied[i])
				consumer.accept(keys[i], (V) values[i]);
	}

	/**
	 * @param key
	 * 		Key to compute the value of if present.
	 * @param value
	 * 		Value to compute the new value with if the key is already present in the map.
	 *
	 * @return Existing value if the key is already present, otherwise the new value computed by the mapping function.
	 */
	public V put(int key, V value) {
		int idx = findIndex(key);
		if (occupied[idx]) {
			Object old = values[idx];
			values[idx] = value;
			return (V) old;
		}

		values[idx] = value;
		insertKeyAt(idx, key);
		return null;
	}

	/**
	 * @param key
	 * 		Key to get the value of.
	 *
	 * @return Value associated with the specified key, or {@code null} if the key is not present in the map.
	 */
	public V get(int key) {
		int idx = findIndex(key);
		return occupied[idx] ? (V) values[idx] : null;
	}

	/**
	 * @param key
	 * 		Key to get the value of.
	 * @param defaultValue
	 * 		Value to return if the key is not present in the map.
	 *
	 * @return Value associated with the specified key, or {@code defaultValue} if the key is not present in the map.
	 */
	public V getOrDefault(int key, Object defaultValue) {
		int idx = findIndex(key);
		return (V) (occupied[idx] ? values[idx] : defaultValue);
	}

	/**
	 * @param key
	 * 		Key to compute the value of if absent.
	 * @param mapping
	 * 		Function to compute the value if the key is not already present in the map.
	 *
	 * @return Existing value if the key is already present, otherwise the new value computed by the mapping function.
	 */
	public V computeIfAbsent(int key, @Nonnull IntFunction<? extends V> mapping) {
		int idx = findIndex(key);
		if (occupied[idx])
			return (V) values[idx];
		V newValue = mapping.apply(key);
		values[idx] = newValue;
		insertKeyAt(idx, key);
		return newValue;
	}

	/**
	 * @param key
	 * 		Key to remove from the map.
	 *
	 * @return Value associated with the specified key before it was removed, or {@code null} if the key was not present in the map.
	 */
	public V remove(int key) {
		int idx = findIndex(key);
		if (!occupied[idx])
			return null;
		V old = (V) values[idx];
		removeEntryAt(idx);
		return old;
	}

	/**
	 * Removes all key-value pairs from the map.
	 */
	public void clear() {
		clearKeys();
		Arrays.fill(values, null);
	}

	@Override
	protected void clearValue(int index) {
		values[index] = null;
	}

	@Override
	protected void moveValue(int from, int to) {
		values[to] = values[from];
	}

	@Override
	protected void resize() {
		int newCap = keys.length * 2;
		int[] oldKeys = keys;
		Object[] oldValues = values;
		boolean[] oldOccupied = occupied;

		allocateTable(newCap);
		values = new Object[newCap];

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldOccupied[i])
				put(oldKeys[i], (V) oldValues[i]);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Int2ObjectMap<?> other))
			return false;
		if (size() != other.size())
			return false;

		for (int i = 0; i < keys.length; i++) {
			if (!occupied[i])
				continue;
			int key = keys[i];
			Object value = values[i];
			Object thatValue = other.get(key);
			if (!Objects.equals(value, thatValue))
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < keys.length; i++) {
			if (!occupied[i])
				continue;
			int key = keys[i];
			Object value = values[i];
			hash += Integer.hashCode(key) ^ Objects.hashCode(value);
		}
		return hash;
	}

	@FunctionalInterface
	public interface IntObjectConsumer<V> {
		void accept(int key, V value);
	}
}
