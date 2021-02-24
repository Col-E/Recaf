package me.coley.recaf.workspace.resource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Map of Android dex classes in the resource.
 *
 * @author Matt Coley
 */
public class MultiDexClassMap implements Map<String, DexClassInfo> {
	private final Map<String, DexClassMap> backingMaps = new HashMap<>();

	/**
	 * Create wrapper of given maps.
	 *
	 * @param backingMap
	 * 		Backing map of named dex files.
	 */
	public MultiDexClassMap(Map<String, DexClassMap> backingMap) {
		this.backingMaps.putAll(backingMap);
	}

	/**
	 * Create empty map.
	 */
	public MultiDexClassMap() {
		// nothing
	}

	/**
	 * Add a backing dex file.
	 *
	 * @param name
	 * 		Dex file name.
	 * @param map
	 * 		Dex classes.
	 */
	public void addBacking(String name, DexClassMap map) {
		backingMaps.put(name, map);
	}

	/**
	 * @return Backing maps of this multi-dex wrapper.
	 */
	public Map<String, DexClassMap> getBackingMap() {
		return backingMaps;
	}

	/**
	 * Remove all listeners.
	 */
	public void clearListeners() {
		backingMaps.values().forEach(map -> map.getListeners().clear());
	}

	/**
	 * @param listener
	 * 		Listener to add to sub-maps.
	 */
	public void addListener(ResourceDexClassListener listener) {
		backingMaps.forEach((name, map) -> map.getListeners().add(CommonItemListener.wrapDex(name, listener)));
	}

	/**
	 * @return Set of items modified since initialization.
	 */
	public Set<String> getDirtyItems() {
		Set<String> set = new HashSet<>();
		for (DexClassMap map : backingMaps.values()) {
			set.addAll(map.getDirtyItems());
		}
		return set;
	}

	@Override
	public int size() {
		int size = 0;
		for (DexClassMap map : backingMaps.values())
			size += map.size();
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		for (DexClassMap map : backingMaps.values())
			if (map.containsKey(key))
				return true;
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (DexClassMap map : backingMaps.values())
			if (map.containsValue(value))
				return true;
		return false;
	}

	@Override
	public DexClassInfo get(Object key) {
		for (DexClassMap map : backingMaps.values()) {
			DexClassInfo info = map.get(key);
			if (info != null)
				return info;
		}
		return null;
	}

	/**
	 * Add a dex item to the map.
	 *
	 * @param dexName
	 * 		Sub-dex map to add to.
	 * @param key
	 * 		Class name.
	 * @param value
	 * 		Class info.
	 *
	 * @return Previous associated value, if any. Otherwise {@code null}.
	 */
	public DexClassInfo put(String dexName, String key, DexClassInfo value) {
		DexClassMap map = backingMaps.get(dexName);
		return map.put(key, value);
	}

	/**
	 * Please use {@link #put(String, String, DexClassInfo)}.
	 *
	 * @param key
	 * 		unused.
	 * @param value
	 * 		unused.
	 *
	 * @return unused.
	 *
	 * @throws UnsupportedOperationException
	 * 		Please use {@link #put(String, String, DexClassInfo)}.
	 */
	@Override
	@Deprecated
	public DexClassInfo put(String key, DexClassInfo value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public DexClassInfo remove(Object key) {
		// TODO: May want to do the same as "put" where we also specify the dex name
		for (DexClassMap map : backingMaps.values()) {
			DexClassInfo info = map.remove(key);
			if (info != null)
				return info;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends DexClassInfo> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		backingMaps.values().forEach(ResourceItemMap::clear);
	}

	@Override
	public Set<String> keySet() {
		Set<String> keys = new TreeSet<>();
		backingMaps.values().forEach(map -> keys.addAll(map.keySet()));
		return keys;
	}

	@Override
	public Collection<DexClassInfo> values() {
		Set<DexClassInfo> keys = new HashSet<>();
		backingMaps.values().forEach(map -> keys.addAll(map.values()));
		return keys;
	}

	@Override
	public Set<Entry<String, DexClassInfo>> entrySet() {
		return backingMaps.values().stream()
				.flatMap(map -> map.values().stream())
				.collect(Collectors.toMap(ItemInfo::getName, v -> v))
				.entrySet();
	}
}
