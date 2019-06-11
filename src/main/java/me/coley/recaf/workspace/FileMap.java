package me.coley.recaf.workspace;

import me.coley.recaf.Input;
import me.coley.recaf.Logging;

import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.util.*;

/**
 * Map wrapper for this structure. Allows users to access classes in a
 * familiar fashion.
 *
 * @param <K>
 * @param <V>
 *
 * @author Matt
 */
public abstract class FileMap<K, V> implements InputFileSystem, Map<K, V> {
	private final Input input;
	protected Map<K, V> cache = new HashMap<>();
	protected final Set<K> keys;

	public FileMap(Input input, Set<K> keys) {
		this.input = input;
		this.keys = keys;
	}

	/**
	 * Exists because casting generics alone is a bad idea without
	 * implementation information.
	 *
	 * @param in
	 * 		Key input.
	 *
	 * @return Key output.
	 */
	abstract K castKey(Object in);

	/**
	 * Exists because casting generics alone is a bad idea without
	 * implementation information.
	 *
	 * @param file
	 * 		Value input.
	 *
	 * @return Value output.
	 */
	abstract V castValue(byte[] file);

	/**
	 * Exists because casting generics alone is a bad idea without
	 * implementation information.
	 *
	 * @param value
	 * 		Value input.
	 *
	 * @return {@code byte[]} of input.
	 */
	abstract byte[] castBytes(V value);

	@Override
	public int size() {
		return keys.size();
	}

	@Override
	public boolean isEmpty() {
		return keys.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return keys.contains(key);
	}

	public boolean containsCache(Object key) {
		return cache.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public void removeCache(Object key) {
		cache.remove(key);
	}

	@Override
	public V get(Object key) {
		// check if cached copy exists.
		V v = cache.get(key);
		if(v != null) {
			return v;
		}
		// no cache, fetch from file system, add to cache.
		try {
			v = castValue(getFile(key.toString()));
			cache.put(castKey(key), v);
			return v;
		} catch(ClosedByInterruptException e) {
			// happens when closing the editor window when runnon on an
			// instrumented input.
			// Ignore.
		} catch(NoSuchFileException e) {
			Logging.error("No file '" + key + "' exists");
		} catch(IOException e) {
			Logging.warn(e);
		}
		return null;
	}

	@Override
	public V put(K key, V value) {
		keys.add(key);
		cache.remove(key);
		try {
			write(getPath(key.toString()), castBytes(value));
		} catch(IOException e) {
			Logging.fatal(e);
		}
		return value;
	}

	@Override
	public V remove(Object key) {
		String ks = key.toString();
		V v = get(key);
		try {
			removeFile(ks);
			keys.remove(ks);
			input.getHistory().remove(ks);
		} catch(IOException e) {
			return v;
		}
		return v;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for(Entry<? extends K, ? extends V> entry : m.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		cache.clear();
		for(K key : keys) {
			remove(key);
			input.getHistory().remove(key);
		}
	}

	@Override
	public Set<K> keySet() {
		return keys;
	}

	@Override
	public Collection<V> values() {
		Set<V> s = new HashSet<>();
		for(K key : keys) {
			s.add(get(key));
		}
		return s;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> s = new HashSet<>();
		for(final K key : keys) {
			final V value = get(key);
			s.add(new Entry<K, V>() {
				@Override
				public K getKey() {
					return key;
				}

				@Override
				public V getValue() {
					return value;
				}

				@Override
				public V setValue(V value) {
					throw new UnsupportedOperationException();
				}
			});
		}
		return s;
	}

	@Override
	public FileSystem getFileSystem() {
		return input.getFileSystem();
	}
}