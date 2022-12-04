package me.coley.recaf.cdi;

import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.manager.api.WeldInjectionTargetFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Since the {@link org.jboss.weld.environment.se.WeldContainer} cannot create beans after it is initialized we will
 * store additional beans here. These are accessed via {@link RecafContainer#get(Class)}.
 *
 * @author Matt Coley
 */
public class RuntimeBeans {
	private final Map<Class<?>, Bean<?>> beanCache = new ConcurrentHashMap<>();
	private final BeanManagerImpl impl;

	/**
	 * @param impl
	 * 		Weld bean manager impl used to create new beans.
	 * 		The base Jakarta model does not allow for this.
	 */
	public RuntimeBeans(BeanManagerImpl impl) {
		this.impl = impl;
	}

	/**
	 * @param bean
	 * 		Bean instance.
	 * @param <T>
	 * 		Instance of bean class.
	 *
	 * @return Instance of class of bean.
	 * {@code null} when an instance could not be created.
	 */
	public <T> T instance(Bean<T> bean) {
		try {
			return bean.create(impl.createCreationalContext(bean));
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * @param cls
	 * 		Class type representing a bean.
	 * @param <T>
	 * 		Class type.
	 *
	 * @return Bean of class type.
	 */
	@SuppressWarnings("unchecked")
	public <T> Bean<T> bean(Class<T> cls) {
		return (Bean<T>) beanCache.computeIfAbsent(cls, c -> {
			AnnotatedType<T> annotatedClass = impl.createAnnotatedType(cls);
			BeanAttributes<T> attributes = impl.createBeanAttributes(annotatedClass);
			WeldInjectionTargetFactory<T> factory = impl.getInjectionTargetFactory(annotatedClass);
			return impl.createBean(attributes, cls, factory);
		});
	}
}
