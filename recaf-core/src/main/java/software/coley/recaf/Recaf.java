package software.coley.recaf;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.se.SeContainer;
import software.coley.recaf.cdi.WorkspaceBeanContext;
import software.coley.recaf.workspace.WorkspaceManager;

import java.lang.annotation.Annotation;
import java.util.Locale;

/**
 * Recaf application instance.
 *
 * @author Matt Coley
 */
public class Recaf {
	private static final Annotation[] NO_QUALIFIERS = new Annotation[0];
	private final SeContainer container;

	/**
	 * @param container
	 * 		Container instance for bean management.
	 */
	public Recaf(@Nonnull SeContainer container) {
		this.container = container;
		setupWorkspaceScopedBeanContext();
	}

	/**
	 * Registers {@link WorkspaceBeanContext} as a listener to the {@link WorkspaceManager} instance.
	 * This allows the context to remove old beans associated with old workspaces.
	 */
	private void setupWorkspaceScopedBeanContext() {
		WorkspaceBeanContext instance = WorkspaceBeanContext.getInstance();
		WorkspaceManager workspaceManager = get(WorkspaceManager.class);
		workspaceManager.addWorkspaceOpenListener(instance);
		workspaceManager.addWorkspaceCloseListener(instance);
	}

	/**
	 * @return The CDI container.
	 */
	public SeContainer getContainer() {
		return container;
	}

	/**
	 * @param type
	 * 		Type to get an {@link Instance} of.
	 * @param <T>
	 * 		Instance type.
	 *
	 * @return Instance accessor for bean of the given type.
	 */
	@Nonnull
	public <T> Instance<T> instance(@Nonnull Class<T> type) {
		return instance(type, NO_QUALIFIERS);
	}

	/**
	 * @param type
	 * 		Type to get an {@link Instance} of.
	 * @param qualifiers
	 * 		Qualifiers to narrow down an option if multiple candidate instances exist for the type.
	 * @param <T>
	 * 		Instance type.
	 *
	 * @return Instance accessor for bean of the given type.
	 */
	@Nonnull
	public <T> Instance<T> instance(@Nonnull Class<T> type, Annotation... qualifiers) {
		return container.select(type, qualifiers);
	}

	/**
	 * @param type
	 * 		Type to get instance of.
	 * @param <T>
	 * 		Instance type.
	 *
	 * @return Instance of type <i>(Wrapped in a proxy)</i>.
	 */
	@Nonnull
	public <T> T get(@Nonnull Class<T> type) {
		return get(type, NO_QUALIFIERS);
	}

	/**
	 * @param type
	 * 		Type to get instance of.
	 * @param qualifiers
	 * 		Qualifiers to narrow down an option if multiple candidate instances exist for the type.
	 * @param <T>
	 * 		Instance type.
	 *
	 * @return Instance of type <i>(Wrapped in a proxy)</i>.
	 */
	@Nonnull
	public <T> T get(@Nonnull Class<T> type, Annotation... qualifiers) {
		return instance(type, qualifiers).get();
	}

	static {
		// Enforce US locale just in case we have some string formatting susceptible to this "feature":
		//  https://mattryall.net/blog/the-infamous-turkish-locale-bug
		Locale.setDefault(Locale.US);
	}
}
