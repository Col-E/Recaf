package software.coley.recaf.cdi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.slf4j.Logger;
import software.coley.recaf.Recaf;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.workspace.WorkspaceCloseListener;
import software.coley.recaf.workspace.WorkspaceManager;
import software.coley.recaf.workspace.WorkspaceModificationListener;
import software.coley.recaf.workspace.WorkspaceOpenListener;
import software.coley.recaf.workspace.model.Workspace;

/**
 * Interceptor for {@link AutoRegisterWorkspaceListeners}.
 * <br>
 * Automatically registers created instances in {@link Recaf} to register workspace listeners.
 *
 * @author Matt Coley
 */
@Interceptor
@AutoRegisterWorkspaceListeners
public class AutoRegisterWorkspaceListenersInterceptor {
	private static final Logger logger = Logging.get(AutoRegisterWorkspaceListenersInterceptor.class);
	private final WorkspaceManager workspaceManager;

	/**
	 * @param workspaceManager
	 * 		Workspace manager instance.
	 */
	@Inject
	public AutoRegisterWorkspaceListenersInterceptor(WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	/**
	 * @param context
	 * 		The {@link InvocationContext#getTarget()} is the instance annotated with
	 *        {@link AutoRegisterWorkspaceListeners}.
	 *
	 * @return {@link InvocationContext#proceed()}.
	 *
	 * @throws Exception
	 * 		Forwarded from {@link InvocationContext#proceed()}.
	 */
	@PostConstruct
	@SuppressWarnings("unused")
	public Object intercept(InvocationContext context) throws Exception {
		// Instance to intercept
		Object value = context.getTarget();
		Class<?> valueType = value.getClass();

		// Check if the instance is application scoped.
		// The scope changes how we handle registering.
		boolean isApplicationScoped = valueType.getAnnotation(ApplicationScoped.class) != null;

		// Add listeners to workspace manager.
		if (isApplicationScoped) {
			// Application scoped beans may want to listen to workspaces opening and closing
			// for, well, the duration of the application.
			if (value instanceof WorkspaceOpenListener)
				workspaceManager.addWorkspaceOpenListener((WorkspaceOpenListener) value);
			if (value instanceof WorkspaceCloseListener)
				workspaceManager.addWorkspaceCloseListener((WorkspaceCloseListener) value);
		} else {
			// The bean is likely dependent scoped, or linked to the lifespan of the current workspace.
			// This it only really makes sense to have the close listener be supported.
			// And when the workspace is closed, we want to remove the instance as a listener, so it doesn't
			// stick around longer than it should.
			if (value instanceof WorkspaceOpenListener)
				logger.warn("The class '{}' implements '{}' but is not @ApplicationScoped",
						valueType.getName(), WorkspaceOpenListener.class.getSimpleName());
			if (value instanceof WorkspaceCloseListener)
				workspaceManager.addWorkspaceCloseListener(
						new AutoUnregisteringCloseListener((WorkspaceCloseListener) value));
		}

		// If a current workspace exists (at the time of creation of the instance), add the modification listener too.
		// We don't need to worry about clearing these because workspaces remove their own listeners on closing.
		Workspace current = workspaceManager.getCurrent();
		if (current != null && value instanceof WorkspaceModificationListener)
			current.addWorkspaceModificationListener((WorkspaceModificationListener) value);

		return context.proceed();
	}

	/**
	 * Wrapper class to automatically unregister itself once a single workspace is closed.
	 * This is applied to {@link Dependent} scoped beans annotated with {@link AutoRegisterWorkspaceListeners}.
	 * <br>
	 * This way once their scope is effectively expired <i>(workspace closed)</i>, the listener is removed.
	 */
	private class AutoUnregisteringCloseListener implements WorkspaceCloseListener {
		private final WorkspaceCloseListener delegate;

		AutoUnregisteringCloseListener(WorkspaceCloseListener delegate) {
			this.delegate = delegate;
		}

		@Override
		public void onWorkspaceClosed(@Nonnull Workspace workspace) {
			delegate.onWorkspaceClosed(workspace);
			workspaceManager.removeWorkspaceCloseListener(this);
		}
	}
}
