package software.coley.recaf.cdi;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import software.coley.recaf.analytics.logging.DebuggingLogger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context for tracking {@link WorkspaceScoped} beans.
 * <br>
 * Beans in this context are alive for the duration of however long the {@link Workspace} is active.
 * When the current workspace closes, all beans are discarded. When a new workspace is opened the beans
 * can be re-created for however long that new one is alive.
 *
 * @author Matt Coley
 */
public class WorkspaceBeanContext implements AlterableContext, WorkspaceOpenListener, WorkspaceCloseListener {
	private static final DebuggingLogger logger = Logging.get(WorkspaceBeanContext.class);
	private static final WorkspaceBeanContext INSTANCE = new WorkspaceBeanContext();
	private final Map<String, WorkspaceBean<?>> map = new ConcurrentHashMap<>();

	public static WorkspaceBeanContext getInstance() {
		return INSTANCE;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return WorkspaceScoped.class;
	}

	@Override
	public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
		// Get existing bean
		Bean<T> bean = (Bean<T>) contextual;
		String beanName = bean.getBeanClass().getName();
		T foundBean = get(contextual);
		if (foundBean != null)
			return foundBean;

		// Not found, create a new bean instead
		logger.debugging(l -> l.info("Creating instance of bean type: {}", beanName));
		WorkspaceBean<T> workspaceBean = new WorkspaceBean<>(contextual, creationalContext, beanName);
		map.put(beanName, workspaceBean);
		return workspaceBean.getBean();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Contextual<T> contextual) {
		Bean<T> bean = (Bean<T>) contextual;
		String beanName = bean.getBeanClass().getName();
		WorkspaceBean<T> workspaceBean = (WorkspaceBean<T>) map.get(beanName);
		if (workspaceBean == null) {
			logger.debugging(l -> l.warn("No instance of bean type: {}", beanName));
			return null;
		}
		return workspaceBean.getBean();
	}

	@Override
	public void destroy(Contextual<?> contextual) {
		Bean<?> bean = (Bean<?>) contextual;
		String beanName = bean.getBeanClass().getName();
		WorkspaceBean<?> workspaceBean = map.remove(beanName);
		if (workspaceBean != null) {
			logger.debugging(l -> l.info("Destroying bean instance of type: {}", beanName));
			workspaceBean.destroy();
		} else {
			logger.warn("No bean instance to destroy by type: {}", beanName);
		}
	}

	@Override
	public void onWorkspaceOpened(@Nonnull Workspace workspace) {
		// Clear cached beans if any were created while there was no active workspace
		map.clear();

		// For eager beans, initialize them.
		for (Bean<?> bean : EagerInitializationExtension.getWorkspaceScopedEagerBeans())
			EagerInitializationExtension.create(bean);
	}

	@Override
	public void onWorkspaceClosed(@Nonnull Workspace workspace) {
		for (WorkspaceBean<?> bean : map.values()) {
			try {
				bean.destroy();
			} catch (Throwable t) {
				logger.error("Failed to update {} bean instance of type: {}",
						WorkspaceScoped.class.getSimpleName(),
						bean.getName());
			}
		}
		// Clear cached beans so that the next workspace allocates new instances upon request
		map.clear();
	}

	@Override
	public boolean isActive() {
		return true;
	}
}