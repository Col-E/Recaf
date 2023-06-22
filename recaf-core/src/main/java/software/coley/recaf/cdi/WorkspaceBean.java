package software.coley.recaf.cdi;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

/**
 * Wrapper for a bean class annotated with {@link WorkspaceScoped}.
 *
 * @param <T>
 * 		Bean type.
 *
 * @author Matt Coley
 */
public class WorkspaceBean<T> {
	private final Contextual<T> contextual;
	private final CreationalContext<T> creationalContext;
	private final T bean;
	private final String beanName;

	/**
	 * @param contextual
	 * 		Bean operations for creation and destruction.
	 * @param creationalContext
	 * 		Supporting operations for the {@code contextual}.
	 * @param beanName
	 * 		Name of the allocated bean.
	 */
	public WorkspaceBean(@Nonnull Contextual<T> contextual,
						 @Nonnull CreationalContext<T> creationalContext,
						 @Nonnull String beanName) {
		this.contextual = contextual;
		this.creationalContext = creationalContext;
		this.beanName = beanName;
		bean = contextual.create(creationalContext);
	}

	/**
	 * @return Bean instance.
	 */
	public T getBean() {
		return bean;
	}

	/**
	 * @return Bean name.
	 */
	public String getName() {
		return beanName;
	}

	/**
	 * Destroy the bean.
	 */
	public void destroy() {
		contextual.destroy(bean, creationalContext);
	}
}