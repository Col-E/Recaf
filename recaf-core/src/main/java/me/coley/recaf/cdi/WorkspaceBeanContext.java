package me.coley.recaf.cdi;

import jakarta.enterprise.context.spi.AlterableContext;
import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import me.coley.recaf.util.logging.DebuggingLogger;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceCloseListener;
import me.coley.recaf.workspace.WorkspaceModificationListener;
import me.coley.recaf.workspace.WorkspaceOpenListener;
import me.coley.recaf.workspace.resource.Resource;

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
public class WorkspaceBeanContext implements AlterableContext, WorkspaceOpenListener, WorkspaceCloseListener, WorkspaceModificationListener {
	private static final DebuggingLogger logger = Logging.get(WorkspaceBeanContext.class);
	private static final WorkspaceBeanContext INSTANCE = new WorkspaceBeanContext();
	private final Map<String, WorkspaceBean<?>> map = new ConcurrentHashMap<>();
	private boolean active;

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
		logger.debugging(l -> l.info("Creating new bean: {}", beanName));
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
			logger.debugging(l -> l.warn("No bean by name: {}", beanName));
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
			logger.debugging(l -> l.info("Destroying bean: {}", beanName));
			workspaceBean.destroy();
		} else {
			logger.warn("No bean to destroy by name: {}", beanName);
		}
	}

	@Override
	public void onWorkspaceOpened(Workspace workspace) {
		active = true;
		workspace.addListener(this);
	}

	@Override
	public void onWorkspaceClosed(Workspace workspace) {
		for (WorkspaceBean<?> bean : map.values()) {
			try {
				bean.onWorkspaceClosed(workspace);
				bean.destroy();
			} catch (Throwable t) {
				logger.error("Failed to update {} bean: {}",
						WorkspaceScoped.class.getSimpleName(),
						bean.getName());
			}
		}
		// Clear cached beans so that the next workspace allocates new instances upon request
		map.clear();
		active = false;
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		for (WorkspaceBean<?> bean : map.values()) {
			try {
				bean.onAddLibrary(workspace, library);
			} catch (Throwable t) {
				logger.error("Failed to update {} bean: {}",
						WorkspaceScoped.class.getSimpleName(),
						bean.getName());
			}
		}
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		for (WorkspaceBean<?> bean : map.values()) {
			try {
				bean.onRemoveLibrary(workspace, library);
			} catch (Throwable t) {
				logger.error("Failed to update {} bean: {}",
						WorkspaceScoped.class.getSimpleName(),
						bean.getName());
			}
		}
	}

	@Override
	public boolean isActive() {
		// Active when a workspace is set
		return active;
	}
}