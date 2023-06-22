package software.coley.recaf.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.*;
import jakarta.inject.Inject;
import software.coley.recaf.workspace.model.Workspace;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension to force creation of {@link EagerInitialization} annotated beans without the need to
 * {@link Inject} and reference them externally.
 *
 * @author Matt Coley
 * @see WorkspaceBeanContext#onWorkspaceOpened(Workspace) for instantiation of {@link WorkspaceScoped} eager beans.
 */
public class EagerInitializationExtension implements Extension {
	private static final EagerInitializationExtension INSTANCE = new EagerInitializationExtension();
	private static final List<Bean<?>> applicationScopedEagerBeansForDeploy = new ArrayList<>();
	private static final List<Bean<?>> applicationScopedEagerBeansForUi = new ArrayList<>();
	private static final List<Bean<?>> workspaceScopedEagerBeans = new ArrayList<>();
	private static BeanManager beanManager;

	private EagerInitializationExtension() {
	}

	/**
	 * @return Extension singleton.
	 */
	public static EagerInitializationExtension getInstance() {
		return INSTANCE;
	}

	/**
	 * @return Application scoped {@link EagerInitialization} beans.
	 */
	public static List<Bean<?>> getApplicationScopedEagerBeansForDeploy() {
		return applicationScopedEagerBeansForDeploy;
	}

	/**
	 * @return Workspace scoped {@link EagerInitialization} beans.
	 */
	public static List<Bean<?>> getWorkspaceScopedEagerBeans() {
		return workspaceScopedEagerBeans;
	}

	/**
	 * Called when a bean is discovered and processed.
	 * We will record eager beans here so we can initialize them later.
	 *
	 * @param event
	 * 		CDI bean process event.
	 */
	public void onProcessBean(@Observes ProcessBean<?> event) {
		Annotated annotated = event.getAnnotated();
		EagerInitialization eager = annotated.getAnnotation(EagerInitialization.class);
		if (eager != null) {
			if (annotated.isAnnotationPresent(ApplicationScoped.class)) {
				if (eager.value() == InitializationStage.IMMEDIATE)
					applicationScopedEagerBeansForDeploy.add(event.getBean());
				else if (eager.value() == InitializationStage.AFTER_UI_INIT)
					applicationScopedEagerBeansForUi.add(event.getBean());
			} else if (annotated.isAnnotationPresent(WorkspaceScoped.class))
				workspaceScopedEagerBeans.add(event.getBean());
		}
	}

	/**
	 * Called when the CDI container deploys.
	 *
	 * @param event
	 * 		CDI deploy event.
	 * @param beanManager
	 * 		CDI bean manager.
	 */
	public void onDeploy(@Observes AfterDeploymentValidation event, BeanManager beanManager) {
		EagerInitializationExtension.beanManager = beanManager;
		for (Bean<?> bean : applicationScopedEagerBeansForDeploy)
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
	public void onUiInitialize(@Observes UiInitializationEvent event, BeanManager beanManager) {
		EagerInitializationExtension.beanManager = beanManager;
		for (Bean<?> bean : applicationScopedEagerBeansForUi)
			create(bean);
	}

	static void create(Bean<?> bean) {
		// NOTE: Calling toString() triggers the bean's proxy to the real implementation to initialize it.
		beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
	}
}
