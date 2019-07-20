package me.coley.recaf.util.struct;

import java.util.Objects;

/**
 * Simple Key:Value pair.
 *
 * @param <K>
 * 		Key type.
 * @param <V>
 * 		Value type.
 *
 * @author Matt
 */
public class Pair<K, V> {
	private final K key;
	private final V value;

	/**
	 * Constructs a pair.
	 *
	 * @param key
	 * 		Left item.
	 * @param value
	 * 		Right item.
	 */
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * @return Left item.
	 */
	public K getKey() { return key; }

	/**
	 * @return Right item, or item associated with the {@link #getKey()}.
	 */
	public V getValue() { return value; }

	@Override
	public String toString() { return key + "=" + value; }

	@Override
	public int hashCode() {	return Objects.hash(key, value); }

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o instanceof Pair) {
			Pair other = (Pair) o;
			if(key != null && !key.equals(other.key)) return false;
			if(value != null && !value.equals(other.value)) return false;
			return true;
		}
		return false;
	}
}
