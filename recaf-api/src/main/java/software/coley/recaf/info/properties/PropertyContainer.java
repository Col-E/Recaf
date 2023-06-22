package software.coley.recaf.info.properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Outline of a type with additional properties able to be assigned.
 *
 * @author Matt Coley
 */
public interface PropertyContainer {
	/**
	 * @param key
	 * 		Key of property to set.
	 * @param value
	 * 		Value of property to set.
	 * @param <V>
	 * 		Property value type.
	 */
	default <V> void setPropertyValue(String key, V value) {
		setProperty(new BasicProperty<>(key, value));
	}

	/**
	 * @param property
	 * 		Property to set.
	 * @param <V>
	 * 		Property value type.
	 */
	<V> void setProperty(Property<V> property);

	/**
	 * @param key
	 * 		Key of property to set.
	 * @param property
	 * 		Property to set, if no value is associated with the given key.
	 * @param <V>
	 * 		Property value type.
	 */
	default <V> void setPropertyIfMissing(String key, Supplier<Property<V>> property) {
		if (getProperty(key) == null)
			setProperty(property.get());
	}

	/**
	 * @param key
	 * 		Key of property to remove.
	 */
	void removeProperty(String key);

	/**
	 * @param key
	 * 		Property key.
	 * @param <V>
	 * 		Property value type.
	 *
	 * @return Property associated with key. May be {@code null} for unknown keys.
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	default <V> Property<V> getProperty(String key) {
		return (Property<V>) getProperties().get(key);
	}

	/**
	 * @param key
	 * 		Property key.
	 * @param <V>
	 * 		Property value type.
	 *
	 * @return Value of property, or {@code null} if the property is not set.
	 */
	@Nullable
	default <V> V getPropertyValueOrNull(String key) {
		Property<V> property = getProperty(key);
		if (property == null)
			return null;
		return property.value();
	}

	/**
	 * @return Properties.
	 */
	@Nonnull
	Map<String, Property<?>> getProperties();

	/**
	 * @return Properties, but only those that are {@link Property#persistent()}.
	 */
	default Map<String, Property<?>> getPersistentProperties() {
		return getProperties().entrySet().stream()
				.filter((e) -> e.getValue().persistent())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
