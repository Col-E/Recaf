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
	 * @param isStandAlone
	 * 		See {@link #isStandAlone()}. Creates an adjacent label for the config value name if {@code true}.
	 * @param type
	 * 		The {@link ConfigValue#getType()} to support.
	 */
	protected TypedConfigComponentFactory(boolean isStandAlone, Class<T> type) {
		super(isStandAlone);
		this.type = type;
	}

	/**
	 * @return The {@link ConfigValue#getType()} this factory is for.
	 */
	public Class<T> getType() {
		return type;
	}
}
