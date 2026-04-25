package software.coley.recaf.util.collect.primitive;

import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Map;

/**
 * A {@link Map}-like collection for {@code int}-{@code int} key-value pairs.
 *
 * @author Matt Coley
 */
public class Int2IntMap extends AbstractIntKeyMap {
	private int[] values;

	/**
	 * New map with default capacity of 16.
	 */
	public Int2IntMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * New map with specified initial capacity.
	 *
	 * @param initialCapacity
	 * 		Initial capacity of the map.
	 */
	public Int2IntMap(int initialCapacity) {
		super(initialCapacity);
		values = new int[keys.length];
	}

	/**
	 * @param consumer
	 * 		Consumer to accept all key-value pairs in the map.
	 */
	public void forEach(@Nonnull IntIntConsumer consumer) {
		for (int i = 0; i < keys.length; i++)
			if (occupied[i])
				consumer.accept(keys[i], values[i]);
	}

	/**
	 * @param key
	 * 		Key to increment the value of.
	 * @param amount
	 * 		Amount to increment the value by.
	 *
	 * @return New value after incrementing.
	 */
	public int increment(int key, int amount) {
		int newValue = getOrDefault(key, 0) + amount;
		put(key, newValue);
		return newValue;
	}

	/**
	 * @param key
	 * 		Key to compute the value of if present.
	 * @param value
	 * 		Value to compute the new value with if the key is already present in the map.
	 *
	 * @return Existing value if the key is already present, otherwise the new value computed by the mapping function.
	 */
	public int put(int key, int value) {
		int idx = findIndex(key);
		if (occupied[idx]) {
			int old = values[idx];
			values[idx] = value;
			return old;
		}

		values[idx] = value;
		insertKeyAt(idx, key);
		return -1;
	}

	/**
	 * @param key
	 * 		Key to get the value of.
	 *
	 * @return Value associated with the specified key, or {@code -1} if the key is not present in the map.
	 */
	public int get(int key) {
		int idx = findIndex(key);
		return occupied[idx] ? values[idx] : -1;
	}

	/**
	 * @param key
	 * 		Key to get the value of.
	 * @param defaultValue
	 * 		Value to return if the key is not present in the map.
	 *
	 * @return Value associated with the specified key, or {@code defaultValue} if the key is not present in the map.
	 */
	public int getOrDefault(int key, int defaultValue) {
		int idx = findIndex(key);
		return occupied[idx] ? values[idx] : defaultValue;
	}

	/**
	 * @param key
	 * 		Key to compute the value of if absent.
	 * @param mapping
	 * 		Function to compute the value if the key is not already present in the map.
	 *
	 * @return Existing value if the key is already present, otherwise the new value computed by the mapping function.
	 */
	public int computeIfAbsent(int key, @Nonnull IntIntFunction mapping) {
		int idx = findIndex(key);
		if (occupied[idx])
			return values[idx];
		int newValue = mapping.apply(key);
		values[idx] = newValue;
		insertKeyAt(idx, key);
		return newValue;
	}

	/**
	 * @param key
	 * 		Key to remove from the map.
	 *
	 * @return Value associated with the specified key before it was removed, or {@code -1} if the key was not present in the map.
	 */
	public int remove(int key) {
		int idx = findIndex(key);
		if (!occupied[idx])
			return -1;
		int old = values[idx];
		removeEntryAt(idx);
		return old;
	}

	/**
	 * Removes all key-value pairs from the map.
	 */
	public void clear() {
		clearKeys();
		Arrays.fill(values, 0);
	}

	@Override
	protected void clearValue(int index) {
		values[index] = 0;
	}

	@Override
	protected void moveValue(int from, int to) {
		values[to] = values[from];
	}

	@Override
	protected void resize() {
		int newCap = keys.length * 2;
		int[] oldKeys = keys;
		int[] oldValues = values;
		boolean[] oldOccupied = occupied;

		allocateTable(newCap);
		values = new int[newCap];

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldOccupied[i])
				put(oldKeys[i], oldValues[i]);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Int2IntMap other))
			return false;
		if (size != other.size)
			return false;

		for (int i = 0; i < keys.length; i++) {
			if (occupied[i]) {
				int key = keys[i];
				int value = values[i];
				int otherValue = other.get(key);
				if (otherValue != value)
					return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		for (int i = 0; i < keys.length; i++) {
			if (occupied[i]) {
				int key = keys[i];
				int value = values[i];
				hash += Integer.hashCode(key) ^ Integer.hashCode(value);
			}
		}
		return hash;
	}

	@FunctionalInterface
	public interface IntIntConsumer {
		void accept(int key, int value);
	}

	@FunctionalInterface
	public interface IntIntFunction {
		int apply(int key);
	}
}
