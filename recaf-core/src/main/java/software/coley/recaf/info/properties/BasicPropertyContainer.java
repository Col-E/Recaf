package software.coley.recaf.info.properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Basic implementation of property container.
 *
 * @author Matt Coley
 */
public class BasicPropertyContainer implements PropertyContainer {
	private Map<String, Property<?>> properties;

	/**
	 * Container with empty map.
	 */
	public BasicPropertyContainer() {
		this(Collections.emptyMap());
	}

	/**
	 * @param properties
	 * 		Pre-defined property map.
	 */
	public BasicPropertyContainer(@Nullable Map<String, Property<?>> properties) {
		this.properties = properties == null || properties.isEmpty() ? null : new HashMap<>(properties);
	}

	@Override
	public <V> void setProperty(Property<V> property) {
		if (properties == null) // Memory optimization to keep null by default
			properties = new HashMap<>();
		properties.put(property.key(), property);
	}

	@Override
	public void removeProperty(String key) {
		if (properties != null)
			properties.remove(key);
	}

	@Nonnull
	public Map<String, Property<?>> getProperties() {
		if (properties == null)
			return Collections.emptyMap();
		// Disallow modification
		return Collections.unmodifiableMap(properties);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicPropertyContainer other = (BasicPropertyContainer) o;

		return Objects.equals(properties, other.properties);
	}

	@Override
	public int hashCode() {
		if (properties == null) return 0;
		return properties.hashCode();
	}

	@Override
	public String toString() {
		String typeName = getClass().getSimpleName();
		if (properties == null) return typeName + "[0]";
		return typeName + "[" + properties.size() + " items]";
	}
}
