package software.coley.recaf.config;

import software.coley.observables.ObservableCollection;

import java.util.Collection;

/**
 * Basic implementation of {@link ConfigCollectionValue}.
 *
 * @param <T>
 * 		Value type.
 * @param <C>
 * 		Collection type.
 *
 * @author Matt Coley
 */
public class BasicCollectionConfigValue<T, C extends Collection<T>> implements ConfigCollectionValue<T, C> {
	private final String key;
	private final Class<C> collectionType;
	private final Class<T> itemType;
	private final ObservableCollection<T, C> observable;

	/**
	 * @param key
	 * 		Value key.
	 * @param type
	 * 		Value type class.
	 * @param observable
	 * 		Observable of value.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public BasicCollectionConfigValue(String key,
									  Class<? extends Collection> type,
									  Class<T> itemType,
									  ObservableCollection<T, C> observable) {
		this.key = key;
		this.collectionType = (Class<C>) type;
		this.itemType = itemType;
		this.observable = observable;
	}

	@Override
	public String getId() {
		return key;
	}

	@Override
	public Class<C> getType() {
		return collectionType;
	}

	@Override
	public Class<T> getItemType() {
		return itemType;
	}

	@Override
	public ObservableCollection<T, C> getObservable() {
		return observable;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicCollectionConfigValue<?, ?> other = (BasicCollectionConfigValue<?, ?>) o;

		if (!key.equals(other.key)) return false;
		if (!collectionType.equals(other.collectionType)) return false;
		if (!itemType.equals(other.itemType)) return false;
		return observable.equals(other.observable);
	}

	@Override
	public int hashCode() {
		int result = key.hashCode();
		result = 31 * result + collectionType.hashCode();
		result = 31 * result + itemType.hashCode();
		result = 31 * result + observable.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "BasicCollectionConfigValue{" +
				"key='" + key + '\'' +
				", collectionType=" + collectionType +
				", itemType=" + itemType +
				'}';
	}
}
