package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.*;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Allocator instance that ties into the CDI container.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class CdiClassAllocator implements ClassAllocator {
	private final Map<Class<?>, Bean<?>> classBeanMap = new WeakHashMap<>();
	private final Lock lock = new ReentrantLock();
	private final BeanManager beanManager;

	@Inject
	public CdiClassAllocator(@Nonnull BeanManager beanManager) {
		this.beanManager = beanManager;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> T instance(@Nonnull Class<T> cls) throws AllocationException {
		lock.lock();
		try {
			// Create bean
			Bean<T> bean = (Bean<T>) classBeanMap.computeIfAbsent(cls, c -> {
				// TODO bugged.
				// Equivalence check is based on the class name and does not include the loader.
				AnnotatedType<T> annotatedClass = beanManager.createAnnotatedType((Class<T>) c);
				BeanAttributes<T> attributes = beanManager.createBeanAttributes(annotatedClass);
				InjectionTargetFactory<T> factory = beanManager.getInjectionTargetFactory(annotatedClass);
				return beanManager.createBean(attributes, (Class<T>) c, factory);
			});
			CreationalContext<T> creationalContext = beanManager.createCreationalContext(bean);

			// Allocate instance of bean
			return bean.create(creationalContext);
		} catch (Throwable t) {
			throw new AllocationException(cls, t);
		} finally {
			lock.unlock();
		}
	}
}
