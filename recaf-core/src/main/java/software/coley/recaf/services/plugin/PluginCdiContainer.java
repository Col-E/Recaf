package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.spi.BeanManager;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CDI runtime of a plugin. Each plugin has its own CDI container that is isolated from other plugins.
 *
 * @author Matt Coley
 */
final class PluginCdiContainer {
	private final WeldContainer container;
	private final CdiClassAllocator allocator;
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * @param container
	 * 		CDI container instance.
	 */
	private PluginCdiContainer(@Nonnull WeldContainer container) {
		this.container = container;

		allocator = new SharedCdiClassAllocator(container.getBeanManager());
	}

	/**
	 * Creates a new plugin CDI container.
	 *
	 * @param source
	 * 		Plugin source to load classes from.
	 * @param classLoader
	 * 		Class loader to use for loading plugin classes.
	 * @param parentBeanManager
	 * 		Parent bean manager to use for CDI discovery.
	 *
	 * @return New plugin CDI container.
	 *
	 * @throws PluginException
	 * 		When the plugin CDI container could not be created.
	 */
	@Nonnull
	static PluginCdiContainer create(@Nonnull PluginSource source,
	                                 @Nonnull PluginClassLoaderImpl classLoader,
	                                 @Nonnull BeanManager parentBeanManager) throws PluginException {
		Weld weld = new Weld("recaf-plugin-" + UUID.randomUUID());
		weld.disableDiscovery();
		weld.setClassLoader(classLoader);
		weld.addExtension(new PluginParentBeansExtension(parentBeanManager));

		// Load all plugin classes into the CDI container.
		Collection<String> classNames = source.classNames();
		List<Class<?>> beanClasses = new ArrayList<>(classNames.size());
		for (String className : classNames) {
			try {
				beanClasses.add(classLoader.lookupClass(className));
			} catch (Throwable failure) {
				PluginException exception = new PluginException("Failed to load plugin class for CDI discovery: " + className, failure);
				shutdownAfterFailedStart(weld, exception);
				throw exception;
			}
		}

		// Initialize the CDI container with the plugin classes.
		try {
			weld.addBeanClasses(beanClasses.toArray(Class<?>[]::new));
			return new PluginCdiContainer(weld.initialize());
		} catch (Throwable failure) {
			PluginException exception = new PluginException("Failed to initialize plugin CDI container", failure);
			shutdownAfterFailedStart(weld, exception);
			throw exception;
		}
	}

	/**
	 * Initializes a new instance of the given type from the plugin CDI container.
	 *
	 * @param type
	 * 		Type to allocate an instance of.
	 * @param <T>
	 * 		Requested type.
	 *
	 * @return Instance of the given type.
	 *
	 * @throws AllocationException
	 * 		When the instance could not be allocated.
	 */
	<T> T instance(Class<T> type) throws AllocationException {
		return allocator.instance(type);
	}

	/**
	 * Releases the CDI resources associated with an instance returned by {@link #instance(Class)}.
	 *
	 * @param instance
	 * 		Instance to release.
	 *
	 * @throws AllocationException
	 * 		When lifecycle cleanup fails.
	 */
	void release(Object instance) throws AllocationException {
		allocator.release(instance);
	}

	/**
	 * Fires an event to the plugin CDI container.
	 *
	 * @param eventType
	 * 		Event type.
	 * @param event
	 * 		Event instance.
	 * @param <T>
	 * 		Event type.
	 */
	<T> void fireEvent(@Nonnull Class<T> eventType, @Nonnull T event) {
		if (closed.get())
			return;

		Event<T> typedEvent = container.event().select(eventType);
		typedEvent.fire(event);
	}

	/**
	 * Shuts down the plugin CDI container.
	 */
	void shutdown() {
		if (closed.compareAndSet(false, true))
			container.shutdown();
	}

	/**
	 * Attempts to shut down the CDI container after a failed start.
	 *
	 * @param weld
	 * 		CDI container instance.
	 * @param failure
	 * 		Failure that caused the shutdown.
	 */
	private static void shutdownAfterFailedStart(@Nonnull Weld weld, @Nonnull PluginException failure) {
		try {
			weld.shutdown();
		} catch (Throwable shutdownFailure) {
			failure.addSuppressed(shutdownFailure);
		}
	}
}
