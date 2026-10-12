package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.cdi.InitializationEvent;
import software.coley.recaf.cdi.UiInitializationEvent;

import java.util.Comparator;
import java.util.List;

/**
 * Forwards events to loaded plugin containers.
 *
 * @author Matt Coley
 * @see InitializationEvent
 * @see UiInitializationEvent
 */
@ApplicationScoped
class PluginEventForwarder {
	private static final Logger logger = Logging.get(PluginEventForwarder.class);
	private final PluginManager pluginManager;

	@Inject
	PluginEventForwarder(@Nonnull PluginManager pluginManager) {
		this.pluginManager = pluginManager;
	}

	/**
	 * @param event
	 * 		Fired event instance to forward.
	 */
	void onInitialization(@Observes InitializationEvent event) {
		forward(InitializationEvent.class, event);
	}

	/**
	 * @param event
	 * 		Fired event instance to forward.
	 */
	void onUiInitialization(@Observes UiInitializationEvent event) {
		forward(UiInitializationEvent.class, event);
	}

	/**
	 * Forwards the given event to all loaded plugins.
	 *
	 * @param eventType
	 * 		Type of the event.
	 * @param event
	 * 		Event instance.
	 * @param <T>
	 * 		Event type.
	 */
	private <T> void forward(@Nonnull Class<T> eventType, @Nonnull T event) {
		List<PluginContainer<?>> plugins = pluginManager.getPlugins().stream()
				.sorted(Comparator.comparing(plugin -> plugin.info().id()))
				.toList();
		for (PluginContainer<?> plugin : plugins) {
			if (!(plugin instanceof PluginContainerImpl<?> container))
				continue;
			try {
				container.fireEvent(eventType, event);
			} catch (RuntimeException ex) {
				logger.warn("Plugin {} failed while observing startup event {}", plugin.info().id(), eventType.getName(), ex);
			}
		}
	}
}
