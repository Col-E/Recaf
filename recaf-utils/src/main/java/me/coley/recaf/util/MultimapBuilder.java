package me.coley.recaf.util;

import java.util.*;
import java.util.function.Supplier;

/**
 * Multimap builder.
 *
 * @param <K>
 * 		Key type.
 * @param <V>
 * 		Value type.
 * @param <C>
 * 		Collection type.
 *
 * @author xDark
 */
public final class MultimapBuilder<K, V, C extends Collection<V>> {
	private final Supplier<? extends Map<K, Collection<V>>> mapSupplier;
	private Supplier<Collection<V>> collectionSupplier;

	private MultimapBuilder(Supplier<? extends Map<K, Collection<V>>> mapSupplier) {
		this.mapSupplier = mapSupplier;
	}

	/**
	 * Creates new multimap builder with hash keys.
	 *
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 *
	 * @return New builder.
	 */
	public static <K, V> MultimapBuilder<K, V, Collection<V>> hashKeys() {
		return new MultimapBuilder<>(HashMap::new);
	}

	/**
	 * Creates new multimap builder with tree keys.
	 *
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 *
	 * @return New builder.
	 */
	@SuppressWarnings(value = {"unused"})
	public static <K extends Comparable<K>, V> MultimapBuilder<K, V, Collection<V>> treeKeys() {
		return new MultimapBuilder<>(TreeMap::new);
	}

	/**
	 * Creates new multimap builder with enum keys.
	 *
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 *
	 * @return New builder.
	 */
	public static <K extends Enum<K>, V> MultimapBuilder<K, V, Collection<V>> enumKeys(Class<K> type) {
		return new MultimapBuilder<>(() -> new EnumMap<>(type));
	}

	/**
	 * Creates new multimap builder with enum keys.
	 *
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 *
	 * @return New builder.
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <K extends Enum<K>, V> MultimapBuilder<K, V, Collection<V>> enumKeys(K... typeHint) {
		return enumKeys((Class<K>) typeHint.getClass().getComponentType());
	}

	/**
	 * Creates new multimap builder with user-specific keys.
	 *
	 * @param supplier
	 * 		Map supplier.
	 * @param <K>
	 * 		Key type.
	 * @param <V>
	 * 		Value type.
	 *
	 * @return New builder.
	 */
	public static <K, V> MultimapBuilder<K, V, Collection<V>> keys(Supplier<? extends Map<K, Collection<V>>> supplier) {
		return new MultimapBuilder<>(supplier);
	}

	/**
	 * Sets backing collection to the array list.
	 *
	 * @return This builder.
	 */
	public MultimapBuilder<K, V, List<V>> arrayValues() {
		collectionSupplier = ArrayList::new;
		return upgrade();
	}

	/**
	 * Sets backing collection to the hash set.
	 *
	 * @return This builder.
	 */
	public MultimapBuilder<K, V, Set<V>> hashValues() {
		collectionSupplier = HashSet::new;
		return upgrade();
	}

	/**
	 * Sets backing collection to the enum set.
	 *
	 * @return This builder.
	 */
	@SafeVarargs
	@SuppressWarnings({"unchecked", "rawtypes"})
	public final MultimapBuilder<K, V, Set<V>> enumValues(V... typeHint) {
		Class<Enum> type = (Class<Enum>) typeHint.getClass().getComponentType();
		if (!type.isEnum()) {
			throw new IllegalStateException("Values are not a enum");
		}
		collectionSupplier = () -> (Set) EnumSet.noneOf(type);
		return upgrade();
	}

	/**
	 * @param collectionSupplier
	 * 		Collection supplier.
	 * @param <C1>
	 * 		Backing collection type.
	 *
	 * @return This builder.
	 */
	@SuppressWarnings("unchecked")
	public <C1 extends Collection<V>> MultimapBuilder<K, V, C1> values(Supplier<C1> collectionSupplier) {
		this.collectionSupplier = (Supplier<Collection<V>>) collectionSupplier;
		return upgrade();
	}

	/**
	 * @return Built multimap.
	 */
	@SuppressWarnings("unchecked")
	public Multimap<K, V, C> build() {
		//noinspection unchecked
		return Multimap.from((Map<K, C>) mapSupplier.get(), (Supplier<C>) collectionSupplier);
	}

	@SuppressWarnings("unchecked")
	private <C1 extends Collection<V>> MultimapBuilder<K, V, C1> upgrade() {
		return (MultimapBuilder<K, V, C1>) this;
	}
}
