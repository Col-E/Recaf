package software.coley.recaf.test.dummy;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DummyEmptyMap<K, V> implements Map<K, V> {

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean containsKey(Object key) {
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		return false;
	}

	@Override
	public V get(Object key) {
		return null;
	}

	@Override
	public V put(K key, V value) {
		return null;
	}

	@Override
	public V remove(Object key) {
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// no-op
	}

	@Override
	public void clear() {
		// no-op
	}

	@Override
	public Set<K> keySet() {
		return Collections.emptySet();
	}

	@Override
	public Collection<V> values() {
		return Collections.emptySet();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return Collections.emptySet();
	}
}
