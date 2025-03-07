package software.coley.recaf.cdi;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension to force creation of {@link EagerInitialization} annotated beans without the need to
 * {@link Inject} and reference them externally.
 *
 * @author Matt Coley
 */
public class EagerInitializationExtension implements Extension {
	private static final EagerInitializationExtension INSTANCE = new EagerInitializationExtension();
	private static final List<Bean<?>> applicationScopedEagerBeans = new ArrayList<>();
	private static final List<Bean<?>> applicationScopedEagerBeansForUi = new ArrayList<>();
	private static BeanManager beanManager;

	private EagerInitializationExtension() {
	}

	/**
	 * @return Extension singleton.
	 */
	@Nonnull
	public static EagerInitializationExtension getInstance() {
		return INSTANCE;
	}

	/**
	 * @return Application scoped {@link EagerInitialization} beans.
	 */
	@Nonnull
	public static List<Bean<?>> getApplicationScopedEagerBeans() {
		return applicationScopedEagerBeans;
	}

	/**
	 * @return Application scoped {@link EagerInitialization} beans which will wait for the UI to be initialized before being initialized.
	 */
	@Nonnull
	public static List<Bean<?>> getApplicationScopedEagerBeansForUi() {
		return applicationScopedEagerBeansForUi;
	}

	/**
	 * Called when a bean is discovered and processed.
	 * We will record eager beans here so that we can initialize them later.
	 *
	 * @param event
	 * 		CDI bean process event.
	 */
	public void onProcessBean(@Observes ProcessBean<?> event) {
		Annotated annotated = event.getAnnotated();
		EagerInitialization eager = annotated.getAnnotation(EagerInitialization.class);
		if (eager != null && annotated.isAnnotationPresent(ApplicationScoped.class)) {
			if (eager.value() == InitializationStage.IMMEDIATE)
				applicationScopedEagerBeans.add(event.getBean());
			else if (eager.value() == InitializationStage.AFTER_UI_INIT)
				applicationScopedEagerBeansForUi.add(event.getBean());
		}
	}

	/**
	 * Called when Recaf initializes the CDI container, and after plugins are loaded.
	 *
	 * @param event
	 * 		Recaf initialization event.
	 * @param beanManager
	 * 		CDI bean manager.
	 */
	public void onInitialize(@Observes InitializationEvent event, @Nonnull BeanManager beanManager) {
		EagerInitializationExtension.beanManager = beanManager;
		for (Bean<?> bean : applicationScopedEagerBeans)
			create(bean);
	}

	/**
	 * Called when the UI is populated.
	 * This obviously means that this only gets called when running from the UI module.
	 *
	 * @param event
	 * 		UI initialization event.
	 * @param beanManager
	 * 		CDI bean manager.
	 */
	public void onUiInitialize(@Observes UiInitializationEvent event, @Nonnull BeanManager beanManager) {
		EagerInitializationExtension.beanManager = beanManager;
		for (Bean<?> bean : applicationScopedEagerBeansForUi)
			create(bean);
	}

	static void create(@Nonnull Bean<?> bean) {
		// NOTE: Calling toString() triggers the bean's proxy to the real implementation to initialize it.
		// We have a null check here because under some test environments this may trigger without being set (see above)
		if (beanManager != null)
			beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
	}
}
