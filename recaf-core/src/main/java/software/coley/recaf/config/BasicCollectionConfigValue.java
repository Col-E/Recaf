package software.coley.recaf.config;

import jakarta.annotation.Nonnull;
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
	private final boolean hidden;

	/**
	 * @param key
	 * 		Value key.
	 * @param type
	 * 		Value type class.
	 * @param observable
	 * 		Observable of value.
	 */
	public BasicCollectionConfigValue(@Nonnull String key,
	                                  @Nonnull Class<? extends Collection> type,
	                                  @Nonnull Class<T> itemType,
	                                  @Nonnull ObservableCollection<T, C> observable) {
		this(key, type, itemType, observable, false);
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
	public BasicCollectionConfigValue(@Nonnull String key,
	                                  @Nonnull Class<? extends Collection> type,
	                                  @Nonnull Class<T> itemType,
	                                  @Nonnull ObservableCollection<T, C> observable,
	                                  boolean hidden) {
		this.key = key;
		this.collectionType = (Class<C>) type;
		this.itemType = itemType;
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
	public Class<C> getType() {
		return collectionType;
	}

	@Override
	public Class<T> getItemType() {
		return itemType;
	}

	@Nonnull
	@Override
	public ObservableCollection<T, C> getObservable() {
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
