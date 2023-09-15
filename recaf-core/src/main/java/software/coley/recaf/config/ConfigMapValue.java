package software.coley.recaf.config;

import java.util.Map;

/**
 * An option stored in a {@link ConfigContainer} object representing a map.
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
public interface ConfigMapValue<K, V, M extends Map<K, V>> extends ConfigValue<M> {
	/**
	 * @return Map key type.
	 */
	Class<K> getKeyType();

	/**
	 * @return Map value type.
	 */
	Class<V> getValueType();
}
