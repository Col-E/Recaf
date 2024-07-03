package software.coley.recaf.config;

import jakarta.annotation.Nonnull;
import software.coley.observables.ObservableMap;

import java.util.Map;

/**
 * Basic implementation of {@link ConfigMapValue}.
 *
 * @param <K>
 * 		Map key type.
 * @param <V>
 * 		Map value type.
 * @param <M>
 * 		Map type.
 *
 * @author Matt Coley
 */
public class BasicMapConfigValue<K, V, M extends Map<K, V>> implements ConfigMapValue<K, V, M> {
	private final String key;
	private final Class<K> keyType;
	private final Class<V> valueType;
	private final Class<M> mapType;
	private final ObservableMap<K, V, M> observable;
	private final boolean hidden;

	/**
	 * @param key
	 * 		Value key.
	 * @param type
	 * 		Value type class.
	 * @param observable
	 * 		Observable of value.
	 */
	@SuppressWarnings("rawtypes")
	public BasicMapConfigValue(String key,
	                           Class<? extends Map> type,
	                           Class<K> keyType,
	                           Class<V> valueType,
	                           ObservableMap<K, V, M> observable) {
		this(key, type, keyType, valueType, observable, false);
	}

	/**
	 * @param key
	 * 		Value key.
	 * @param type
	 * 		Value type class.
	 * @param observable
	 * 		Observable of value.
	 * @param hidden
	 * 		Hidden flag.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public BasicMapConfigValue(String key,
	                           Class<? extends Map> type,
	                           Class<K> keyType,
	                           Class<V> valueType,
	                           ObservableMap<K, V, M> observable,
	                           boolean hidden) {
		this.key = key;
		this.mapType = (Class<M>) type;
		this.keyType = keyType;
		this.valueType = valueType;
		this.observable = observable;
		this.hidden = hidden;
	}

	@Nonnull
	@Override
	public String getId() {
		return key;
	}

	@Nonnull
	@Override
	public Class<M> getType() {
		return mapType;
	}

	@Override
	public Class<K> getKeyType() {
		return keyType;
	}

	@Override
	public Class<V> getValueType() {
		return valueType;
	}

	@Nonnull
	@Override
	public ObservableMap<K, V, M> getObservable() {
		return observable;
	}

	@Override
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicMapConfigValue<?, ?, ?> that = (BasicMapConfigValue<?, ?, ?>) o;

		if (hidden != that.hidden) return false;
		if (!key.equals(that.key)) return false;
		if (!keyType.equals(that.keyType)) return false;
		if (!valueType.equals(that.valueType)) return false;
		if (!mapType.equals(that.mapType)) return false;
		return observable.equals(that.observable);
	}

	@Override
	public int hashCode() {
		int result = key.hashCode();
		result = 31 * result + keyType.hashCode();
		result = 31 * result + valueType.hashCode();
		result = 31 * result + mapType.hashCode();
		result = 31 * result + observable.hashCode();
		result = 31 * result + (hidden ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		return "BasicMapConfigValue{" +
				"key='" + key + '\'' +
				", keyType=" + keyType +
				", valueType=" + valueType +
				", mapType=" + mapType +
				", observable=" + observable +
				", hidden=" + hidden +
				'}';
	}
}
