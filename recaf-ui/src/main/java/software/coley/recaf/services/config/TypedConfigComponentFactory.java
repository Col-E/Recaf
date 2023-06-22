package software.coley.recaf.services.config;

import software.coley.recaf.config.ConfigValue;

/**
 * Factory for any {@link ConfigValue} of a specific {@link ConfigValue#getType()}.
 *
 * @param <T>
 * 		Value type of {@link ConfigValue} to create a component for.
 *
 * @author Matt Coley
 */
public abstract class TypedConfigComponentFactory<T> extends ConfigComponentFactory<T> {
	private final Class<T> type;

	/**
	 * @param createLabel
	 * 		See {@link #isStandAlone()}. Determines if label is automatically added.
	 * @param type
	 * 		The {@link ConfigValue#getType()} to support.
	 */
	protected TypedConfigComponentFactory(boolean createLabel, Class<T> type) {
		super(createLabel);
		this.type = type;
	}

	/**
	 * @return The {@link ConfigValue#getType()} this factory is for.
	 */
	public Class<T> getType() {
		return type;
	}
}
