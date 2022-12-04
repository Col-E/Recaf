package me.coley.recaf.cdi;

import jakarta.enterprise.inject.spi.Bean;
import me.coley.recaf.Recaf;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.manager.BeanManagerImpl;

import java.util.Set;
import java.util.stream.Collectors;

import static me.coley.recaf.cdi.BeanUtils.isBean;

/**
 * Wrapper for {@link WeldContainer} providing support for declaring additional beans at runtime via {@link RuntimeBeans}.
 *
 * @author Matt Coley
 */
public class RecafContainer extends DelegatingContext {
	private static RecafContainer instance;
	private final RuntimeBeans runtimeBeans;
	private final Set<Class<?>> beanClasses;

	private RecafContainer(WeldContainer container) {
		super(container);
		BeanManagerImpl beanManagerImpl = BeanManagerProxy.unwrap(getBeanManager());
		runtimeBeans = new RuntimeBeans(beanManagerImpl);
		beanClasses = beanManagerImpl.getBeans().stream()
				.map(Bean::getBeanClass)
				.collect(Collectors.toSet());
	}

	/**
	 * @param classes
	 * 		Additional classes to add as beans.
	 */
	@SuppressWarnings("unchecked")
	public static void initialize(Class<?>... classes) {
		if (instance == null) {
			Weld weld = new Weld("Recaf-Container");
			weld.addExtensions(WorkspaceBeanExtension.class);
			weld.addPackage(true, Recaf.class);
			weld.addBeanClasses(classes);
			WeldContainer container = weld.initialize();
			instance = new RecafContainer(container);
		}
	}

	/**
	 * Convenience call for {@code instance.select(cls).get()}.
	 *
	 * @param cls
	 * 		Class to instantiate. Must be a bean type.
	 * @param <T>
	 * 		Bean type.
	 *
	 * @return Instance of bean.
	 */
	public static <T> T get(Class<T> cls) {
		if (!isBean(cls))
			throw new IllegalArgumentException("Class is not a CDI bean: " + cls);
		if (isPresentInContainer(cls))
			return instance.select(cls).get();
		else {
			RuntimeBeans runtime = instance.runtimeBeans;
			Bean<T> bean = runtime.bean(cls);
			return runtime.instance(bean);
		}
	}

	/**
	 * @param cls
	 * 		Class to check.
	 *
	 * @return {@code true} when the {@link WeldContainer} has the class as a bean.
	 * {@code false} implies the bean can only be used via {@link RuntimeBeans}.
	 */
	private static boolean isPresentInContainer(Class<?> cls) {
		return instance.beanClasses.contains(cls);
	}
}
