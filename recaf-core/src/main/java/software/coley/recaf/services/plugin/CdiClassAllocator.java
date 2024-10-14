package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.*;
import jakarta.inject.Inject;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Allocator instance that ties into the CDI container.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CdiClassAllocator implements ClassAllocator {
	private final Map<Class<?>, Bean<?>> classBeanMap = new IdentityHashMap<>();
	private final BeanManager beanManager;

	@Inject
	public CdiClassAllocator(@Nonnull BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> T instance(@Nonnull Class<T> cls) throws AllocationException {
		try {
			// Create bean
			Bean<T> bean = (Bean<T>) classBeanMap.computeIfAbsent(cls, c -> {
				AnnotatedType<T> annotatedClass = beanManager.createAnnotatedType(cls);
				BeanAttributes<T> attributes = beanManager.createBeanAttributes(annotatedClass);
				InjectionTargetFactory<T> factory = beanManager.getInjectionTargetFactory(annotatedClass);
				return beanManager.createBean(attributes, cls, factory);
			});
			CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);

			// Allocate instance of bean
			return bean.create(creationalContext);
		} catch (Throwable t) {
			throw new AllocationException(cls, t);
		}
	}
}
