package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Simple multi-map.
 *
 * @author xDark
 */
public final class MultiMap<K, V, C extends Collection<V>> {
	private final Map<K, C> backing;
	private final Function<K, ? extends C> collectionFunction;

	/**
	 * @param backing
	 * 		Backing map.
	 * @param collectionSupplier
	 * 		Collection supplier.
	 */
	private MultiMap(@Nonnull Map<K, C> backing, @Nonnull Supplier<? extends C> collectionSupplier) {
		this.backing = backing;
		this.collectionFunction = __ -> collectionSupplier.get();
	}

	/**
	 * @return Total amount of items in the map.
	 */
	public int size() {
		return backing.values()
				.stream()
				.mapToInt(Collection::size)
				.sum();
	}

	/**
	 * @return {@code true} if the map is empty.
	 */
	public boolean isEmpty() {
		return backing.values()
				.stream()
				.noneMatch(Collection::isEmpty);
	}

	/**
	 * @param key
	 * 		Key to check.
	 *
	 * @return {@code true} if map contains the key.
	 */
	public boolean containsKey(K key) {
		return backing.containsKey(key);
	}

	/**
	 * @param value
	 * 		Value to check.
	 *
	 * @return {@code true} if map contains the value.
	 */
	public boolean containsValue(V value) {
		return backing.values()
				.stream()
				.anyMatch(c -> c.contains(value));
	}

	/**
	 * @param key
	 * 		Key to get a collection for.
	 *
	 * @return Collection for the key.
	 */
	@Nonnull
	public C get(K key) {
		return backing.computeIfAbsent(key, collectionFunction);
	}

	/**
	 * @param key
	 * 		Key to get a collection for.
	 *
	 * @return A collection of values or an empty list, if none.
	 */
	@SuppressWarnings("unchecked")
	@Nonnull
	public Collection<V> getIfPresent(K key) {
		return ((Map<K, Collection<V>>) backing).getOrDefault(key, Collections.emptyList());
	}

	/**
	 * @param key
	 * 		Key to get a collection for.
	 *
	 * @return A collection of values or {@code defaultValue}, if none.
	 */
	public C getOrDefault(K key, C defaultValue) {
		return backing.getOrDefault(key, defaultValue);
	}

	/**
	 * Puts value in the map.
	 *
	 * @param key
	 * 		Key.
	 * @param value
	 * 		Value.
	 *
	 * @return {@code true} if value was added to the collection.
	 */
	public boolean put(K key, V value) {
		return get(key).add(value);
	}

	/**
	 * Puts a collection of values in the map.
	 *
	 * @param key
	 * 		Key.
	 * @param values
	 * 		Values.
	 *
	 * @return {@code true} if any value was added to the collection.
	 */
	public boolean putAll(K key, @Nonnull Collection<? extends V> values) {
		return get(key).addAll(values);
	}

	/**
	 * Removes a key-value pair.
	 *
	 * @param key
	 * 		Key.
	 * @param value
	 * 		Value.
	 *
	 * @return {@code true} if key-value pair was removed.
	 */
	public boolean remove(K key, V value) {
		C collection = backing.get(key);
		if (collection != null && collection.remove(value) && collection.isEmpty()) {
			backing.remove(key);
			return true;
		}
		return false;
	}

	/**
	 * Removes a collection of values.
	 *
	 * @param key
	 * 		Key to remove.
	 *
	 * @return Collection of removed values or an empty list, if none.
	 */
	@Nonnull
	public Collection<V> remove(K key) {
		Collection<V> collection = backing.remove(key);
		if (collection == null) {
			return Collections.emptyList();
		}
		return collection;
	}

	/**
	 * Clears the map.
	 */
	public void clear() {
		backing.clear();
	}

	/**
	 * @return All keys.
	 */
	@Nonnull
	public Set<K> keySet() {
		return backing.keySet();
	}

	/**
	 * @return All values.
	 */
	@Nonnull
	public Stream<V> values() {
		return backing.values()
				.stream()
				.flatMap(Collection::stream);
	}

	/**
	 * @return Map entry set.
	 */
	@Nonnull
	public Set<Map.Entry<K, C>> entrySet() {
		return backing.entrySet();
	}

	/**
	 * Creates a multi-map.
	 *
	 * @param map
	 * 		Backing map.
	 * @param collectionSupplier
	 * 		Supplier for collections of values.
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 * @param <C>
	 * 		Collection type.
	 *
	 * @return New multi-map.
	 */
	@Nonnull
	public static <K, V, C extends Collection<V>> MultiMap<K, V, C> from(Map<K, C> map, Supplier<? extends C> collectionSupplier) {
		return new MultiMap<>(map, collectionSupplier);
	}
}
