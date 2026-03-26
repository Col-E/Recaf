package software.coley.recaf.util.collect.primitive;

import java.util.Map;

/**
 * A {@link Map}-like collection for {@code Object}-{@code long} key-value pairs.
 *
 * @param <K>
 * 		Type of keys in the map.
 *
 * @author Matt Coley
 */
public class Object2LongMap<K> extends AbstractObjectKeyMap<K> {
	private long[] values;

	/**
	 * New map with default capacity of 16.
	 */
	public Object2LongMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * New map with specified initial capacity.
	 *
	 * @param initialCapacity
	 * 		Initial capacity of the map.
	 */
	public Object2LongMap(int initialCapacity) {
		super(initialCapacity);
		values = new long[keys.length];
	}

	/**
	 * @param consumer
	 * 		Consumer to accept all key-value pairs in the map.
	 */
	public void forEach(ObjectLongConsumer<K> consumer) {
		for (int i = 0; i < keys.length; i++)
			if (occupied[i])
				consumer.accept((K) keys[i], values[i]);
	}

	/**
	 * @param key
	 * 		Key to compute the value of if present.
	 * @param value
	 * 		Value to compute the new value with if the key is already present in the map.
	 *
	 * @return Existing value if the key is already present, otherwise the new value computed by the mapping function.
	 */
	public long put(K key, long value) {
		if (key == null)
			throw new NullPointerException("Null keys not supported");
		int idx = findIndex(key);
		if (occupied[idx]) {
			long old = values[idx];
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
	public long get(K key) {
		if (key == null)
			return -1;
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
	public long getOrDefault(K key, long defaultValue) {
		if (key == null)
			return defaultValue;
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
	public long computeIfAbsent(K key, ObjectLongFunction<K> mapping) {
		if (key == null)
			throw new NullPointerException("Null keys not supported");
		int idx = findIndex(key);
		if (occupied[idx])
			return values[idx];
		long newValue = mapping.apply(key);
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
	public long remove(K key) {
		if (key == null)
			return -1;
		int idx = findIndex(key);
		if (!occupied[idx])
			return -1;
		long old = values[idx];
		removeEntryAt(idx);
		return old;
	}

	/**
	 * Removes all key-value pairs from the map.
	 */
	public void clear() {
		clearKeys();
	}

	@Override
	protected void clearValue(int index) {
		values[index] = 0L;
	}

	@Override
	protected void moveValue(int from, int to) {
		values[to] = values[from];
	}

	@Override
	protected void resize() {
		int newCap = keys.length * 2;
		Object[] oldKeys = keys;
		long[] oldValues = values;
		boolean[] oldOccupied = occupied;

		allocateTable(newCap);
		values = new long[newCap];

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldOccupied[i])
				put((K) oldKeys[i], oldValues[i]);
		}
	}

	@FunctionalInterface
	public interface ObjectLongConsumer<K> {
		void accept(K key, long value);
	}

	@FunctionalInterface
	public interface ObjectLongFunction<K> {
		long apply(K key);
	}
}
