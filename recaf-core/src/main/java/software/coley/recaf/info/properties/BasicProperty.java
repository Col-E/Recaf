package software.coley.recaf.info.properties;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Basic property implementation.
 *
 * @param <V>
 * 		Value type.
 *
 * @author Matt Coley
 */
public class BasicProperty<V> implements Property<V> {
	private final String key;
	private final V value;

	/**
	 * @param key
	 * 		Property key.
	 * @param value
	 * 		Property value.
	 */
	public BasicProperty(String key, V value) {
		this.key = key;
		this.value = value;
	}

	@Nonnull
	@Override
	public String key() {
		return key;
	}

	@Override
	public V value() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BasicProperty<?> that = (BasicProperty<?>) o;

		if (!Objects.equals(key, that.key)) return false;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		int result = key != null ? key.hashCode() : 0;
		result = 31 * result + (value != null ? value.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "SimpleProperty{" +
				"key='" + key + '\'' +
				", value=" + value +
				'}';
	}
}
