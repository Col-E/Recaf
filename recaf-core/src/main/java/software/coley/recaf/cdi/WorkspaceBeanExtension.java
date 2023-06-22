package software.coley.recaf.cdi;

import jakarta.enterprise.context.spi.Context;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * Extension that enabled {@link WorkspaceScoped} annotated beans to be registered with {@link WorkspaceBeanContext}.
 * The custom context enables some workspace-oriented operations.
 *
 * @author Matt Coley
 */
@SuppressWarnings("unused")
public class WorkspaceBeanExtension implements Extension {
	private static final WorkspaceBeanExtension INSTANCE = new WorkspaceBeanExtension();

	private WorkspaceBeanExtension() {
	}

	/**
	 * @return Extension singleton.
	 */
	public static WorkspaceBeanExtension getInstance() {
		return INSTANCE;
	}

	/**
	 * Adds the {@link WorkspaceScoped} handler {@link WorkspaceBeanContext} to the CDI container via the event.
	 *
	 * @param event
	 * 		Event to call {@link AfterBeanDiscovery#addContext(Context)} on.
	 */
	public void registerContext(@Observes AfterBeanDiscovery event) {
		WorkspaceBeanContext context = WorkspaceBeanContext.getInstance();
		event.addContext(context);
	}
}