package software.coley.recaf.services.config;

import atlantafx.base.theme.Styles;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javafx.scene.Node;
import javafx.scene.control.Label;
import software.coley.recaf.config.ConfigContainer;
import software.coley.recaf.config.ConfigGroups;
import software.coley.recaf.config.ConfigValue;
import software.coley.recaf.services.Service;
import software.coley.recaf.ui.pane.ConfigPane;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manages which {@link ConfigComponentFactory} instances to use to create editor components in the {@link ConfigPane}
 * for {@link ConfigValue} instances.
 *
 * @author Matt Coley
 * @see ConfigPane UI where this manager is used.
 * @see ConfigComponentFactory Factory base.
 */
@ApplicationScoped
public class ConfigComponentManager implements Service {
	public static final String ID = "config-components";
	private final ConfigComponentFactory<Object> DEFAULT_FACTORY = new ConfigComponentFactory<>(false) {
		@Nonnull
		@Override
		public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<Object> value) {
			Label label = new Label("Unsupported: " + value.getType().getName());
			label.getStyleClass().add(Styles.WARNING);
			return label;
		}
	};
	private final Map<String, ConfigComponentFactory<?>> keyToConfigurator = new HashMap<>();
	private final Map<Class<?>, ConfigComponentFactory<?>> typeToConfigurator = new HashMap<>();
	private final ConfigComponentManagerConfig config;

	@Inject
	public ConfigComponentManager(@Nonnull ConfigComponentManagerConfig config,
	                              @Nonnull Instance<KeyedConfigComponentFactory<?>> keyedFactories,
	                              @Nonnull Instance<TypedConfigComponentFactory<?>> typedFactories) {
		this.config = config;

		// Register implementations
		for (KeyedConfigComponentFactory<?> factory : keyedFactories)
			register(factory.getId(), factory);
		for (TypedConfigComponentFactory<?> factory : typedFactories)
			register(factory.getType(), factory);
	}

	/**
	 * @param id
	 * 		The {@link ConfigContainer#getGroupAndId()} + {@link ConfigValue#getId()}, used to create factories to generate components for a specific value.
	 * @param factory
	 * 		Factory to generate components to support the given type.
	 *
	 * @see #register(ConfigContainer, String, boolean, BiFunction) Alternative for generating a {@link KeyedConfigComponentFactory}.
	 */
	public void register(@Nonnull String id, @Nonnull KeyedConfigComponentFactory<?> factory) {
		keyToConfigurator.put(id, factory);
	}

	/**
	 * @param registeringContainer
	 * 		The container holding the config value.
	 * @param valueId
	 * 		The {@link ConfigValue#getId() config value id} to register for.
	 * @param standAlone
	 * 		See {@link ConfigComponentFactory#isStandAlone()}.
	 * @param factory
	 * 		Function for the implementation of {@link KeyedConfigComponentFactory}.
	 * @param <T>
	 * 		Config value type.
	 */
	public <T> void register(@Nonnull ConfigContainer registeringContainer, @Nonnull String valueId, boolean standAlone,
	                         @Nonnull BiFunction<ConfigContainer, ConfigValue<T>, Node> factory) {
		String id = registeringContainer.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + valueId;
		keyToConfigurator.put(id, new KeyedConfigComponentFactory<T>(standAlone, id) {
			@Nonnull
			@Override
			public Node create(@Nonnull ConfigContainer container, @Nonnull ConfigValue<T> value) {
				return factory.apply(container, value);
			}
		});
	}

	/**
	 * @param type
	 * 		Class type.
	 * @param factory
	 * 		Factory to generate components to support the given type.
	 */
	public void register(@Nonnull Class<?> type, @Nonnull TypedConfigComponentFactory<?> factory) {
		typeToConfigurator.put(type, factory);
	}

	/**
	 * @param value
	 * 		Value to get factory for.
	 * @param <T>
	 * 		Value type.
	 *
	 * @return Component factory for value.
	 */
	@SuppressWarnings("unchecked")
	public <T> ConfigComponentFactory<T> getFactory(@Nonnull ConfigContainer container, @Nonnull ConfigValue<T> value) {
		// Get factory for config value ID.
		String id = container.getGroupAndId() + ConfigGroups.PACKAGE_SPLIT + value.getId();
		ConfigComponentFactory<?> factory = keyToConfigurator.get(id);
		if (factory != null)
			return (ConfigComponentFactory<T>) factory;

		// Get factory for config value type.
		Class<T> type = value.getType();
		factory = typeToConfigurator.get(type);
		if (factory != null)
			return (ConfigComponentFactory<T>) factory;

		// Check for common generic types.
		if (type.isEnum())
			factory = typeToConfigurator.get(Enum.class);
		if (factory != null)
			return (ConfigComponentFactory<T>) factory;

		// Fallback factory.
		return (ConfigComponentFactory<T>) DEFAULT_FACTORY;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public ConfigComponentManagerConfig getServiceConfig() {
		return config;
	}
}
