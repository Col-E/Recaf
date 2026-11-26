package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.configurator.BeanConfigurator;
import jakarta.inject.Inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Exposes eligible beans as child-container beans backed by references from the parent container.
 *
 * @author Matt Coley
 */
final class PluginParentBeansExtension implements Extension {
	private static final String BEAN_PREFIX = "software.coley.recaf.";

	private final Map<CreationalContext<?>, CreationalContext<?>> dependentContexts = new IdentityHashMap<>();
	private final BeanManager parentBeanManager;

	@Inject
	PluginParentBeansExtension(@Nonnull BeanManager parentBeanManager) {
		this.parentBeanManager = parentBeanManager;
	}

	@SuppressWarnings("unchecked")
	void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery) {
		// Collect all beans from the parent container that are eligible to be bridged into the child container.
		Set<Bean<?>> parentBeans = parentBeanManager.getBeans(Object.class, Any.Literal.INSTANCE);
		for (Bean<?> parentBean : parentBeans) {
			if (!parentBean.getBeanClass().getName().startsWith(BEAN_PREFIX))
				continue;

			// Only allow beans that are application-scoped or dependent-scoped to be bridged into the child container.
			Class<? extends Annotation> scope = parentBean.getScope();
			if (scope != ApplicationScoped.class && scope != Dependent.class) {
				afterBeanDiscovery.addDefinitionError(new IllegalStateException(
						"Plugin CDI cannot bridge bean %s with unsupported scope %s"
								.formatted(parentBean.getBeanClass().getName(), scope.getName())));
				continue;
			}

			// Create a child bean that is backed by a reference to the parent bean.
			Bean<Object> bean = (Bean<Object>) parentBean;
			BeanConfigurator<Object> configurator = afterBeanDiscovery.addBean();
			configurator.read(bean)
					.beanClass(parentBean.getBeanClass())
					.createWith(childContext -> createReference(bean, childContext))
					.destroyWith((instance, childContext) -> destroyReference(bean, instance, childContext));
		}
	}

	/**
	 * Creates a reference to the parent bean for use in the child container.
	 *
	 * @param parentBean
	 * 		Parent bean to create a reference for.
	 * @param childContext
	 * 		Child creational context that will be used to manage the reference.
	 *
	 * @return Reference to the parent bean.
	 */
	@Nonnull
	private Object createReference(@Nonnull Bean<Object> parentBean, @Nonnull CreationalContext<Object> childContext) {
		boolean dependent = parentBean.getScope() == Dependent.class;
		CreationalContext<Object> parentContext = parentBeanManager.createCreationalContext(parentBean);
		try {
			// Create a reference to the parent bean.
			// This will ensure that the parent bean is created and managed by the parent container.
			Object reference = parentBeanManager.getReference(parentBean, referenceType(parentBean), parentContext);
			if (dependent) {
				synchronized (dependentContexts) {
					// Track the child context so that we can release the parent context when the child context is released.
					if (dependentContexts.containsKey(childContext))
						throw new IllegalStateException("Duplicate child creational context for bridged bean " + parentBean.getBeanClass().getName());
					dependentContexts.put(childContext, parentContext);
				}
			}
			return reference;
		} catch (RuntimeException | Error failure) {
			// If the reference creation fails, we need to release the parent context to avoid a memory leak.
			try {
				parentContext.release();
			} catch (Throwable releaseFailure) {
				if (failure != releaseFailure)
					failure.addSuppressed(releaseFailure);
			}
			throw failure;
		}
	}

	/**
	 * Destroys a reference to the parent bean when the child context is released.
	 *
	 * @param parentBean
	 * 		Parent bean that was referenced.
	 * @param instance
	 * 		Instance of the parent bean that was referenced.
	 * @param childContext
	 * 		Child creational context that was used to manage the reference.
	 */
	@SuppressWarnings("unchecked")
	private void destroyReference(@Nonnull Bean<Object> parentBean, @Nonnull Object instance,
	                              @Nonnull CreationalContext<Object> childContext) {
		if (parentBean.getScope() != Dependent.class)
			return;

		// Release the parent context that was used to create the reference.
		// This will ensure that the parent bean is destroyed if it is no longer needed.
		CreationalContext<?> parentContext;
		synchronized (dependentContexts) {
			parentContext = dependentContexts.remove(childContext);
		}
		if (parentContext == null)
			throw new IllegalStateException("Missing parent creational context for bridged bean " + parentBean.getBeanClass().getName());

		parentBean.destroy(instance, (CreationalContext<Object>) parentContext);
	}

	/**
	 * Gets the type of the parent bean that should be used for the reference.
	 *
	 * @param bean
	 * 		Parent bean to get the type for.
	 *
	 * @return Type of the parent bean that should be used for the reference.
	 */
	@Nonnull
	private static Type referenceType(@Nonnull Bean<?> bean) {
		Class<?> beanClass = bean.getBeanClass();
		Set<Type> beanTypes = bean.getTypes();
		if (beanTypes.contains(beanClass))
			return beanClass;
		return beanTypes.stream()
				.filter(type -> type != Object.class)
				.min(Comparator.comparing(Type::getTypeName))
				.orElseThrow(() -> new IllegalStateException("No usable bean type for bean " + beanClass.getName()));
	}
}
