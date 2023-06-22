package software.coley.recaf.services.config;

import javafx.scene.Node;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.ui.pane.ConfigPane;

/**
 * Instances of these factories determine how {@link ConfigValue} instances are represented in the {@link ConfigPane}.
 * These factories should be registered in {@link ConfigComponentManager}.
 *
 * @param <T>
 * 		Value type of {@link ConfigValue} to create a component for.
 *
 * @author Matt Coley
 * @see ConfigPane UI where these are used.
 * @see ConfigComponentManager Manager for registering these factories.
 * @see KeyedConfigComponentFactory Factory impl for targeting a specific value by its {@link ConfigValue#getId()}.
 * @see TypedConfigComponentFactory Factory impl for targeting any value by its {@link ConfigValue#getType()}.
 */
public abstract class ConfigComponentFactory<T> {
	private final boolean standAlone;

	/**
	 * @param standAlone
	 * 		See {@link #isStandAlone()}. Determines if label is automatically added.
	 */
	protected ConfigComponentFactory(boolean standAlone) {
		this.standAlone = standAlone;
	}

	/**
	 * @return {@code true} when the created component should span the full page in {@link ConfigPane}.
	 * {@code false} will place a label next to the created component based on the {@link ConfigValue#getId()}.
	 */
	public boolean isStandAlone() {
		return standAlone;
	}

	/**
	 * @param container
	 * 		Container of the value.
	 * @param value
	 * 		Value wrapper.
	 *
	 * @return Control to represent the value.
	 */
	public abstract Node create(ConfigContainer container, ConfigValue<T> value);
}
