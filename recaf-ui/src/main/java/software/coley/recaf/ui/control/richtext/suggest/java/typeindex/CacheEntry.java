package software.coley.recaf.ui.control.richtext.suggest.java.typeindex;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Cached workspace-backed {@link TypeIndex} entry.
 * <p>
 * The entry lazily builds an index, attaches the listeners needed to observe workspace changes,
 * and applies queued workspace deltas when the index is next requested.
 *
 * @author Matt Coley
 * @see JavaTypeIndexService
 */
public final class CacheEntry {
	private static final Logger logger = Logging.get(CacheEntry.class);

	private final Workspace workspace;
	private final WorkspaceModificationListener workspaceListener = new WorkspaceModificationListener() {
		@Override
		public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			attachResource(library);
			recordChange(new ResourceChange(library, true));
		}

		@Override
		public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
			detachResource(library);
			recordChange(new ResourceChange(library, false));
		}
	};
	private final ResourceJvmClassListener jvmClassListener = new ResourceJvmClassListener() {
		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			recordChange(new ClassChange(resource, bundle, null, cls));
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource,
		                          @Nonnull JvmClassBundle bundle,
		                          @Nonnull JvmClassInfo oldCls,
		                          @Nonnull JvmClassInfo newCls) {
			recordChange(new ClassChange(resource, bundle, oldCls, newCls));
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			recordChange(new ClassChange(resource, bundle, cls, null));
		}
	};
	private final ResourceAndroidClassListener androidClassListener = new ResourceAndroidClassListener() {
		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			recordChange(new ClassChange(resource, bundle, null, cls));
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource,
		                          @Nonnull AndroidClassBundle bundle,
		                          @Nonnull AndroidClassInfo oldCls,
		                          @Nonnull AndroidClassInfo newCls) {
			recordChange(new ClassChange(resource, bundle, oldCls, newCls));
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			recordChange(new ClassChange(resource, bundle, cls, null));
		}
	};
	private final List<PendingChange> pendingChanges = new ArrayList<>();

	private boolean attached;
	private boolean fullRebuildRequired = true;
	private TypeIndex index;

	CacheEntry(@Nonnull Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * @return Current type index for the workspace, rebuilding or patching it when the cache is stale.
	 */
	@Nonnull
	public synchronized TypeIndex getIndex() {
		if (!attached)
			attach();
		if (index == null || fullRebuildRequired)
			rebuild();
		else if (!pendingChanges.isEmpty())
			applyPendingChanges();
		return index;
	}

	/**
	 * Detaches all listeners and drops the cached index for the workspace.
	 */
	public synchronized void close() {
		if (!attached)
			return;
		workspace.removeWorkspaceModificationListener(workspaceListener);
		for (WorkspaceResource resource : workspace.getAllResources(true))
			detachResource(resource);
		attached = false;
		index = null;
		fullRebuildRequired = true;
		pendingChanges.clear();
	}

	/**
	 * Rebuilds the index from scratch, dropping any pending changes.
	 */
	private void rebuild() {
		index = TypeIndex.build(workspace);
		fullRebuildRequired = false;
		pendingChanges.clear();
	}

	/**
	 * Applies pending changes to the current index, rebuilding if any change fails to apply.
	 */
	private void applyPendingChanges() {
		TypeIndex currentIndex = index;

		// Do a full rebuild if no existing index exists.
		if (currentIndex == null) {
			rebuild();
			return;
		}

		try {
			// Apply all pending changes to the current index.
			for (PendingChange change : List.copyOf(pendingChanges))
				change.apply(currentIndex);
			pendingChanges.clear();
		} catch (Throwable t) {
			logger.warn("Failed to apply pending changes to type index, rebuilding from scratch", t);

			// Something failed, just wipe the state and start over.
			fullRebuildRequired = true;
			pendingChanges.clear();
			rebuild();
		}
	}

	/**
	 * @param change
	 * 		Change to record for later application to the index.
	 */
	private synchronized void recordChange(@Nonnull PendingChange change) {
		if (index == null || fullRebuildRequired) {
			fullRebuildRequired = true;
			pendingChanges.clear();
			return;
		}
		pendingChanges.add(change);
	}

	/**
	 * Attaches listeners to the workspace and all current resources.
	 */
	private void attach() {
		// Watch the workspace and all current resources so later completion requests see updated types.
		workspace.addWorkspaceModificationListener(workspaceListener);
		for (WorkspaceResource resource : workspace.getAllResources(true))
			attachResource(resource);
		attached = true;
	}

	/**
	 * @param resource
	 * 		Resource to attach listeners to.
	 */
	private void attachResource(@Nonnull WorkspaceResource resource) {
		resource.addListener(jvmClassListener);
		resource.addListener(androidClassListener);
	}

	/**
	 * @param resource
	 * 		Resource to remove listeners from.
	 */
	private void detachResource(@Nonnull WorkspaceResource resource) {
		resource.removeListener(jvmClassListener);
		resource.removeListener(androidClassListener);
	}

	/**
	 * Outline of some change.
	 */
	private interface PendingChange {
		/**
		 * @param index
		 * 		Index to apply the change to.
		 */
		void apply(@Nonnull TypeIndex index);
	}

	/**
	 * Change to a resource, either an addition or removal.
	 *
	 * @param resource
	 * 		Resource being added or removed.
	 * @param add
	 *        {@code true} if the resource is being added, or {@code false} if it's being removed.
	 */
	private record ResourceChange(@Nonnull WorkspaceResource resource, boolean add) implements PendingChange {
		@Override
		public void apply(@Nonnull TypeIndex index) {
			if (add)
				index.addResource(resource);
			else
				index.removeResource(resource);
		}
	}

	/**
	 * Change to a class, either an addition, update, or removal.
	 *
	 * @param resource
	 * 		Containing resource.
	 * @param bundle
	 * 		Containing bundle.
	 * @param oldInfo
	 * 		Prior class info, or {@code null} if the class is being added.
	 * @param newInfo
	 * 		New class info, or {@code null} if the class is being removed.
	 */
	private record ClassChange(@Nonnull WorkspaceResource resource,
	                           @Nonnull Bundle<?> bundle,
	                           @Nullable ClassInfo oldInfo,
	                           @Nullable ClassInfo newInfo) implements PendingChange {
		@Override
		public void apply(@Nonnull TypeIndex index) {
			if (oldInfo == null && newInfo != null) {
				index.addClass(resource, bundle, newInfo);
			} else if (oldInfo != null && newInfo == null) {
				index.removeClass(resource, bundle, oldInfo);
			} else if (oldInfo != null) {
				index.updateClass(resource, bundle, oldInfo, newInfo);
			}
		}
	}
}
