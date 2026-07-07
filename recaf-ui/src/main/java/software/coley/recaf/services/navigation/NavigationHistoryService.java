package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.IncompletePathException;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.ClassBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks recently interacted code declarations for pane-level back/forward navigation.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NavigationHistoryService implements Service {
	public static final String ID = "navigation-history";

	private final NavigationHistoryConfig config;
	private final Actions actions;
	private final List<PathNode<?>> entries = new ArrayList<>();
	private volatile boolean replaying;
	private int cursor = -1;

	@Inject
	public NavigationHistoryService(@Nonnull NavigationHistoryConfig config,
	                                @Nonnull Actions actions,
	                                @Nonnull WorkspaceManager workspaceManager) {
		this.config = config;
		this.actions = actions;

		// Clear history when workspace is closed. We can't navigate to declarations we no longer have access to.
		workspaceManager.addWorkspaceCloseListener(workspace -> clear());
	}

	/**
	 * @param path
	 * 		Path to record.
	 */
	public synchronized void record(@Nonnull PathNode<?> path) {
		// Skip if we're replaying a navigation event, or if the path is not supported for navigation.
		if (replaying || !isSupported(path))
			return;

		// Resolve the path to the current workspace content. If the path no longer exists, then we can't record it.
		PathNode<?> currentPath = resolveCurrent(path);
		if (currentPath == null)
			return;

		// If the current path is the same as the last recorded path, then we don't need to record it again.
		if (cursor >= 0 && entries.get(cursor).equals(currentPath))
			return;

		// Trim any forward history, as we are now recording a new path.
		while (entries.size() > cursor + 1)
			entries.removeLast();

		// Record the new path and update the cursor to point to it.
		entries.add(currentPath);
		cursor = entries.size() - 1;

		// Trim history to the maximum number of entries.
		trim();
	}

	/**
	 * Navigate to the prior recorded declaration.
	 *
	 * @return {@code true} when navigation was handled.
	 */
	public boolean back() {
		return navigate(-1);
	}

	/**
	 * Navigate to the next recorded declaration.
	 *
	 * @return {@code true} when navigation was handled.
	 */
	public boolean forward() {
		return navigate(1);
	}

	/**
	 * @return {@code true} when {@link #back()} can navigate.
	 */
	public synchronized boolean canGoBack() {
		return cursor > 0;
	}

	/**
	 * @return {@code true} when {@link #forward()} can navigate.
	 */
	public synchronized boolean canGoForward() {
		return cursor >= 0 && cursor < entries.size() - 1;
	}

	/**
	 * Navigate to a recorded declaration.
	 * @param direction {@code -1} for back, {@code 1} for forward.
	 * @return {@code true} when navigation was handled.
	 */
	private boolean navigate(int direction) {
		while (true) {
			// Try and resolve the path at the target index.
			int targetIndex;
			PathNode<?> targetPath;
			synchronized (this) {
				// If there is no history, or we are at the end of the history, then navigation is not possible.
				targetIndex = cursor + direction;
				if (targetIndex < 0 || targetIndex >= entries.size())
					return false;
				targetPath = entries.get(targetIndex);
			}

			// Resolve the path to the current workspace content.
			// If the path no longer exists, then remove it from history and try the next entry in the history.
			PathNode<?> currentPath = resolveCurrent(targetPath);
			if (currentPath == null) {
				removeAt(targetIndex);
				continue;
			}

			// Update the history entry to the current path, and update the cursor to the target index.
			synchronized (this) {
				entries.set(targetIndex, currentPath);
				cursor = targetIndex;
			}

			// Navigate to the declaration.
			// If the path is incomplete, then remove it from history and try the next entry in the history.
			try {
				// Mark that we are replaying a navigation event,
				// so that we don't record the navigation event in history.
				replaying = true;
				actions.gotoDeclaration(currentPath);
				return true;
			} catch (IncompletePathException ignored) {
				removeAt(targetIndex);
			} finally {
				replaying = false;
			}
		}
	}

	/**
	 * Clear all history entries.
	 */
	private synchronized void clear() {
		entries.clear();
		cursor = -1;
	}

	/**
	 * Trim history to the maximum number of entries.
	 */
	private synchronized void trim() {
		int maxEntries = Math.max(1, config.getMaxEntries().getValue());
		while (entries.size() > maxEntries) {
			entries.removeFirst();
			cursor--;
		}
		if (cursor < 0 && !entries.isEmpty())
			cursor = 0;
	}

	/**
	 * @param index History index to remove.
	 */
	private synchronized void removeAt(int index) {
		entries.remove(index);
		if (index < cursor)
			cursor--;
		if (cursor >= entries.size())
			cursor = entries.size() - 1;
		if (entries.isEmpty())
			cursor = -1;
	}

	/**
	 * Fetches a fresh path for the given path. This is necessary because we may be operating on an old path
	 * with outdated contents, so navigating to it may show outdated information.
	 * @param path Recorded path, either a class or class member path.
	 * @return Current path, or {@code null} when the path no longer exists.
	 */
	@Nullable
	private static PathNode<?> resolveCurrent(@Nonnull PathNode<?> path) {
		if (path instanceof ClassMemberPathNode memberPath) {
			ClassPathNode parentPath = memberPath.getParent();
			if (parentPath == null)
				return null;

			ClassPathNode currentParentPath = resolveCurrentClass(parentPath);
			if (currentParentPath == null)
				return null;

			return currentParentPath.child(memberPath.getValue().getName(), memberPath.getValue().getDescriptor());
		} else if (path instanceof ClassPathNode classPath) {
			return resolveCurrentClass(classPath);
		}
		return null;
	}

	/**
	 * Fetches a fresh path for the given class path. This is necessary because we may be operating on an old path
	 * with outdated contents, so navigating to it may show outdated information.
	 *
	 * @param classPath
	 * 		Recorded class path.
	 *
	 * @return Current class path, or {@code null} when the class no longer exists.
	 */
	@Nullable
	private static ClassPathNode resolveCurrentClass(@Nonnull ClassPathNode classPath) {
		String className = classPath.getValue().getName();

		// First check if the class is still in the workspace. If it is, we can use that reference.
		Workspace workspace = classPath.getValueOfType(Workspace.class);
		if (workspace != null)
			return workspace.findClass(className);

		// Otherwise, check if the class is still in the bundle. If it is not, then we can't navigate to it.
		ClassBundle<?> bundle = classPath.getValueOfType(ClassBundle.class);
		if (bundle != null && bundle.get(className) == null)
			return null;

		return classPath.withCurrentWorkspaceContent();
	}

	private static boolean isSupported(@Nonnull PathNode<?> path) {
		return path instanceof ClassPathNode || path instanceof ClassMemberPathNode;
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public NavigationHistoryConfig getServiceConfig() {
		return config;
	}
}
