package software.coley.recaf.config;

import software.coley.observables.Observable;

/**
 * Basic implementation of {@link ConfigValue}.
 *
 * @param <T>
 * 		Value type.
 *
 * @author Matt Coley
 */
public class BasicConfigValue<T> implements ConfigValue<T> {
	private final String key;
	private final Class<T> type;
	private final Observable<T> observable;
	private final boolean hidden;

	/**
	 * @param key
	 * 		Value key.
	 * @param type
	 * 		Value type class.
	 * @param observable
	 * 		Observable of value.
	 */
	public BasicConfigValue(String key, Class<T> type, Observable<T> observable) {
		this(key, type, observable, false);
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
	public BasicConfigValue(String key, Class<T> type, Observable<T> observable, boolean hidden) {
		this.key = key;
		this.type = type;
		this.observable = observable;
		this.hidden = hidden;
	}

	@Override
	public String getId() {
		return key;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public Observable<T> getObservable() {
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

		BasicConfigValue<?> other = (BasicConfigValue<?>) o;

		if (!key.equals(other.key)) return false;
		return type.equals(other.type);
	}

	@Override
	public int hashCode() {
		int result = key.hashCode();
		result = 31 * result + type.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "BasicConfigValue{" +
				"key='" + key + '\'' +
				", type=" + type +
				'}';
	}
}
