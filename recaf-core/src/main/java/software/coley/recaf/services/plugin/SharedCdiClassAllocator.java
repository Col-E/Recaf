package software.coley.recaf.services.plugin;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionTarget;
import jakarta.inject.Inject;
import org.jboss.weld.manager.api.WeldManager;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Allocator singleton instance that ties into the CDI container.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class SharedCdiClassAllocator implements CdiClassAllocator {
	private static final AtomicLong TYPE_ID_COUNTER = new AtomicLong();

	private final Map<Class<?>, AllocationTarget<?>> activeTargets = new HashMap<>();
	private final Map<Object, Allocation<?>> activeAllocations = new IdentityHashMap<>();
	private final Lock lock = new ReentrantLock();
	private final WeldManager weldManager;

	@Inject
	public SharedCdiClassAllocator(@Nonnull BeanManager beanManager) {
		if (!(beanManager instanceof WeldManager manager))
			throw new IllegalArgumentException("CdiClassAllocator requires Weld's BeanManager implementation");
		weldManager = manager.unwrap();
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public <T> T instance(@Nonnull Class<T> cls) throws AllocationException {
		lock.lock();
		try {
			AllocationTarget<T> target = (AllocationTarget<T>) activeTargets.get(cls);
			if (target == null) {
				target = createTarget(cls);
				activeTargets.put(cls, target);
			}
			target.activeAllocations++;

			CreationalContext<T> creationalContext = null;
			T instance = null;
			boolean postConstructed = false;
			boolean duplicateInstance = false;
			try {
				creationalContext = weldManager.createCreationalContext(null);
				instance = target.injectionTarget.produce(creationalContext);
				if (activeAllocations.containsKey(instance)) {
					duplicateInstance = true;
					throw new IllegalStateException("Injection target reused an active instance of " + cls.getName());
				}
				target.injectionTarget.inject(instance, creationalContext);
				target.injectionTarget.postConstruct(instance);
				postConstructed = true;
				activeAllocations.put(instance, new Allocation<>(target, creationalContext));
				return instance;
			} catch (Throwable t) {
				Throwable failure = t;
				if (instance != null && !duplicateInstance)
					failure = cleanup(target, instance, creationalContext, postConstructed, failure);
				else if (creationalContext != null)
					failure = releaseCreationalContext(creationalContext, failure);
				failure = releaseTarget(target, failure);
				throw new AllocationException(cls, failure);
			}
		} catch (AllocationException ex) {
			throw ex;
		} catch (Throwable t) {
			throw new AllocationException(cls, t);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void release(@Nonnull Object instance) throws AllocationException {
		lock.lock();
		try {
			Allocation<?> allocation = activeAllocations.remove(instance);
			if (allocation == null)
				return;

			AllocationTarget<?> target = allocation.target;
			Throwable failure = cleanup(target, instance, allocation.creationalContext, true, null);
			failure = releaseTarget(target, failure);
			if (failure != null)
				throw new AllocationException(target.type, failure);
		} finally {
			lock.unlock();
		}
	}

	private <T> AllocationTarget<T> createTarget(@Nonnull Class<T> type) throws AllocationException {
		// Weld's default annotated-type ID is class-name based, which collides across plugin classloaders.
		String id = SharedCdiClassAllocator.class.getName() + "-" + TYPE_ID_COUNTER.incrementAndGet();
		AnnotatedType<T> annotatedType = null;
		try {
			annotatedType = weldManager.createAnnotatedType(type, id);
			InjectionTarget<T> injectionTarget = weldManager.fireProcessInjectionTarget(annotatedType);
			return new AllocationTarget<>(type, id, injectionTarget);
		} catch (Throwable t) {
			if (annotatedType != null) {
				try {
					weldManager.disposeAnnotatedType(type, id);
				} catch (Throwable cleanupFailure) {
					t.addSuppressed(cleanupFailure);
				}
			}
			throw new AllocationException(type, t);
		}
	}

	private Throwable releaseTarget(@Nonnull AllocationTarget<?> target, Throwable failure) {
		if (--target.activeAllocations == 0) {
			activeTargets.remove(target.type);
			try {
				weldManager.disposeAnnotatedType(target.type, target.id);
			} catch (Throwable cleanupFailure) {
				failure = accumulate(failure, cleanupFailure);
			}
		}
		return failure;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static Throwable cleanup(@Nonnull AllocationTarget<?> target, @Nonnull Object instance,
	                                CreationalContext<?> creationalContext, boolean postConstructed, Throwable failure) {
		InjectionTarget injectionTarget = target.injectionTarget;
		if (postConstructed) {
			try {
				injectionTarget.preDestroy(instance);
			} catch (Throwable cleanupFailure) {
				failure = accumulate(failure, cleanupFailure);
			}
		}
		try {
			injectionTarget.dispose(instance);
		} catch (Throwable cleanupFailure) {
			failure = accumulate(failure, cleanupFailure);
		}
		return releaseCreationalContext(creationalContext, failure);
	}

	private static Throwable releaseCreationalContext(CreationalContext<?> creationalContext, Throwable failure) {
		if (creationalContext != null) {
			try {
				creationalContext.release();
			} catch (Throwable cleanupFailure) {
				failure = accumulate(failure, cleanupFailure);
			}
		}
		return failure;
	}

	private static Throwable accumulate(Throwable failure, Throwable additionalFailure) {
		if (failure == null)
			return additionalFailure;
		if (failure != additionalFailure)
			failure.addSuppressed(additionalFailure);
		return failure;
	}

	private static final class AllocationTarget<T> {
		private final Class<T> type;
		private final String id;
		private final InjectionTarget<T> injectionTarget;
		private int activeAllocations;

		private AllocationTarget(Class<T> type, String id, InjectionTarget<T> injectionTarget) {
			this.type = type;
			this.id = id;
			this.injectionTarget = injectionTarget;
		}
	}

	private record Allocation<T>(AllocationTarget<T> target, CreationalContext<T> creationalContext) {
	}
}
