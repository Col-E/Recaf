package software.coley.recaf.info.properties;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic implementation of property container.
 *
 * @author Matt Coley
 */
public class BasicPropertyContainer implements PropertyContainer {
	private Map<String, Property<?>> properties = new HashMap<>();

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
	public BasicPropertyContainer(Map<String, Property<?>> properties) {
		this.properties.putAll(properties);
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

		return properties.equals(other.properties);
	}

	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	@Override
	public String toString() {
		return "BasicPropertyContainer[" + properties.size() + " items]";
	}
}
