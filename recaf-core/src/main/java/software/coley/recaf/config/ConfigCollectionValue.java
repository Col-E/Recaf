package software.coley.recaf.config;

import java.util.Collection;

/**
 * An option stored in a {@link ConfigContainer} object representing a collection.
 *
 * @param <T>
 * 		Collection value type.
 * @param <C>
 * 		Collection type.
 *
 * @author Matt Coley
 */
public interface ConfigCollectionValue<T, C extends Collection<T>> extends ConfigValue<C> {
	/**
	 * @return Collection type.
	 */
	Class<T> getItemType();
}
