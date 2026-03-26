package software.coley.recaf.util.collect.primitive;

import java.util.Map;

/**
 * A {@link Map}-like collection for {@code Object}-{@code int} key-value pairs.
 *
 * @param <K>
 * 		Type of keys in the map.
 *
 * @author Matt Coley
 */
public class Object2IntMap<K> extends AbstractObjectKeyMap<K> {
	private int[] values;

	/**
	 * New map with default capacity of 16.
	 */
	public Object2IntMap() {
		this(DEFAULT_CAPACITY);
	}

	/**
	 * New map with specified initial capacity.
	 *
	 * @param initialCapacity
	 * 		Initial capacity of the map.
	 */
	public Object2IntMap(int initialCapacity) {
		super(initialCapacity);
		values = new int[keys.length];
	}

	/**
	 * @param consumer
	 * 		Consumer to accept all key-value pairs in the map.
	 */
	public void forEach(ObjectIntConsumer<K> consumer) {
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
	public int put(K key, int value) {
		if (key == null)
			throw new NullPointerException("Null keys not supported");
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
	public int get(K key) {
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
	public int getOrDefault(K key, int defaultValue) {
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
	public int computeIfAbsent(K key, ObjectIntFunction<K> mapping) {
		if (key == null)
			throw new NullPointerException("Null keys not supported");
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
	public int remove(K key) {
		if (key == null)
			return -1;
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
		Object[] oldKeys = keys;
		int[] oldValues = values;
		boolean[] oldOccupied = occupied;

		allocateTable(newCap);
		values = new int[newCap];

		for (int i = 0; i < oldKeys.length; i++) {
			if (oldOccupied[i])
				put((K) oldKeys[i], oldValues[i]);
		}
	}

	@FunctionalInterface
	public interface ObjectIntConsumer<K> {
		void accept(K key, int value);
	}

	@FunctionalInterface
	public interface ObjectIntFunction<K> {
		int apply(K key);
	}
}
