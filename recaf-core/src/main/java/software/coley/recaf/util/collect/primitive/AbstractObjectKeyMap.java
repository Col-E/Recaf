package software.coley.recaf.util.collect.primitive;

import java.util.Arrays;
import java.util.Objects;

import static software.coley.recaf.util.NumberUtil.nextPowerOfTwo;

/**
 * Outline for common object-keyed map implementations.
 *
 * @param <K>
 * 		Type of keys in the map.
 *
 * @author Matt Coley
 */
abstract class AbstractObjectKeyMap<K> {
	protected static final int DEFAULT_CAPACITY = 16;
	protected static final float LOAD_FACTOR = 0.75f;

	protected Object[] keys;
	protected boolean[] occupied;
	protected int size;
	protected int threshold;

	protected AbstractObjectKeyMap() {
		this(DEFAULT_CAPACITY);
	}

	protected AbstractObjectKeyMap(int initialCapacity) {
		allocateTable(Math.max(2, nextPowerOfTwo(initialCapacity)));
	}

	/**
	 * @param key
	 * 		Key to check for in the map.
	 *
	 * @return {@code true} if the map contains the specified key, {@code false} otherwise.
	 */
	public final boolean containsKey(K key) {
		return key != null && occupied[findIndex(key)];
	}

	/**
	 * @return Number of element pairs in the map.
	 */
	public final int size() {
		return size;
	}

	protected final void allocateTable(int capacity) {
		keys = new Object[capacity];
		occupied = new boolean[capacity];
		size = 0;
		threshold = (int) (capacity * LOAD_FACTOR);
	}

	protected final void clearKeys() {
		size = 0;
		Arrays.fill(keys, null);
		Arrays.fill(occupied, false);
	}

	protected final int findIndex(K key) {
		int len = keys.length;
		int idx = hash(key) & (len - 1);
		while (occupied[idx]) {
			if (Objects.equals(keys[idx], key))
				return idx;
			idx = (idx + 1) & (len - 1);
		}
		return idx;
	}

	protected final void insertKeyAt(int index, K key) {
		keys[index] = key;
		occupied[index] = true;
		size++;
		if (size > threshold)
			resize();
	}

	protected final void removeEntryAt(int index) {
		size--;
		closeDeletion(index);
	}

	protected int hash(Object key) {
		int h = key.hashCode();
		return h ^ (h >>> 16);
	}

	private void closeDeletion(int deletedIndex) {
		int index = deletedIndex;
		int len = keys.length;

		for (int next = (index + 1) & (len - 1); occupied[next]; next = (next + 1) & (len - 1)) {
			int slot = hash(keys[next]) & (len - 1);
			if ((next < slot && (slot <= index || index <= next)) || (slot <= index && index <= next)) {
				keys[index] = keys[next];
				occupied[index] = true;
				moveValue(next, index);
				index = next;
			}
		}

		keys[index] = null;
		occupied[index] = false;
		clearValue(index);
	}

	protected abstract void clearValue(int index);

	protected abstract void moveValue(int from, int to);

	protected abstract void resize();
}
