package software.coley.recaf.services.navigation;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import org.slf4j.Logger;
import software.coley.bentofx.dockable.Dockable;
import software.coley.bentofx.path.DockablePath;
import software.coley.collections.Unchecked;
import software.coley.recaf.analytics.logging.Logging;
import software.coley.recaf.behavior.PrioritySortable;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.builtin.RemapOriginTaskProperty;
import software.coley.recaf.path.AbstractPathNode;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.services.Service;
import software.coley.recaf.services.mapping.MappingResults;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.docking.DockingManager;
import software.coley.recaf.util.FxThreadUtil;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.WorkspaceModificationListener;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.ResourceAndroidClassListener;
import software.coley.recaf.workspace.model.resource.ResourceFileListener;
import software.coley.recaf.workspace.model.resource.ResourceJvmClassListener;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks available {@link Navigable} content currently open in the UI.
 * <br>
 * This is done by tracking the content of {@link Dockable} instances when they are {@link Navigable}.
 * This component is itself {@link Navigable} which means if we use these tracked instances as our
 * {@link #getNavigableChildren()} we can do dynamic look-ups with {@link #getNavigableChildrenByPath(PathNode)}
 * to discover any currently open content.
 *
 * @author Matt Coley
 * @see DockingManager
 */
@ApplicationScoped
public class NavigationManager implements Navigable, Service {
	public static final String ID = "navigation";
	private static final Logger logger = Logging.get(NavigationManager.class);
	private final List<NavigableAddListener> addListeners = new CopyOnWriteArrayList<>();
	private final List<NavigableRemoveListener> removeListeners = new CopyOnWriteArrayList<>();
	private final Map<Navigable, Dockable> childrenToDockable = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Map<Dockable, NavigableSpy> dockableToSpy = Collections.synchronizedMap(new IdentityHashMap<>());
	private final Forwarding forwarding = new Forwarding();
	private final NavigationManagerConfig config;
	private PathNode<?> path = new DummyInitialNode();

	@Inject
	public NavigationManager(@Nonnull NavigationManagerConfig config,
	                         @Nonnull DockingManager dockingManager,
	                         @Nonnull WorkspaceManager workspaceManager) {
		this.config = config;

		// Track what navigable content is available.
		dockingManager.getBento().events().addDockableOpenListener((path, dockable) -> {
			ObjectProperty<Node> contentProperty = dockable.nodeProperty();

			// Create spy for the tab.
			NavigableSpy spy = new NavigableSpy(dockable);
			dockableToSpy.put(dockable, spy);

			// Add listener, so if content changes we are made aware of the changes.
			contentProperty.addListener(spy);

			// Record initial value.
			spy.changed(contentProperty, null, contentProperty.getValue());
		});
		dockingManager.getBento().events().addDockableCloseListener((path, dockable) -> {
			// The tab is closed, remove its spy lookup.
			NavigableSpy spy = dockableToSpy.remove(dockable);
			if (spy == null) {
				logger.warn("Tab {} was closed, but had no associated content spy instance", dockable.getTitle());
				return;
			}

			// Remove content from navigation tracking.
			spy.remove(dockable.nodeProperty().get());

			// Remove the listener from the tab.
			dockable.nodeProperty().removeListener(spy);
		});

		// When the workspace closes, close all associated children.
		workspaceManager.addWorkspaceCloseListener(workspace -> FxThreadUtil.run(() -> {
			for (Navigable child : new ArrayList<>(childrenToDockable.keySet())) {
				child.disable();

				Dockable dockable = childrenToDockable.get(child);
				if (dockable == null) {
					logger.warn("Navigation manager children-to-dockable map did not have value for key: {}", child.getPath());
					continue;
				}

				DockablePath path = dockable.getPath();
				if (path == null) {
					logger.warn("Navigation manager couldn't invoke dockable.onClose() since dockable path could not be resolved for: {}", dockable.getTitle());

					// Still need to remove it from the map to prevent leakage.
					dockableToSpy.remove(dockable);
					continue;
				}

				// Trigger on-close handler
				if (!dockable.isClosable())
					dockable.setClosable(true);
				path.leafContainer().closeDockable(dockable);
			}

			// Validate all child references have been removed.
			if (!childrenToDockable.isEmpty()) {
				logger.warn("Navigation manager children-to-dockable map was not empty after workspace closure");
				childrenToDockable.clear();
			}

			// Remove the path reference to the old workspace.
			forwarding.workspacePath = null;
			path = new DummyInitialNode();

			// Force close any remaining tabs that hold navigable content.
			for (DockablePath path : dockingManager.getBento().search().allDockables()) {
				Dockable dockable = path.dockable();
				if (dockable.getNode() instanceof Navigable navigable) {
					navigable.disable();
					path.leafContainer().closeDockable(dockable);
				}
			}
		}));

		// Track current workspace so that we are navigable ourselves.
		// Nothing here *needs* to be on the FX thread per-say, but because the closer handling logic is on the FX
		// thread making this also on the FX thread will help prevent race conditions on assigning the 'path' field.
		workspaceManager.addWorkspaceOpenListener(workspace -> FxThreadUtil.run(() -> {
			WorkspacePathNode workspacePath = PathNodes.workspacePath(workspace);
			path = workspacePath;

			// Update forwarding's path.
			forwarding.workspacePath = workspacePath;
		}));

		// Setup forwarding workspace updates to children.
		workspaceManager.addDefaultWorkspaceModificationListeners(new WorkspaceModificationListener() {
			@Override
			public void onAddLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				library.addListener(forwarding);
			}

			@Override
			public void onRemoveLibrary(@Nonnull Workspace workspace, @Nonnull WorkspaceResource library) {
				library.removeListener(forwarding);
			}
		});
		workspaceManager.addWorkspaceOpenListener(workspace -> {
			for (WorkspaceResource resource : workspace.getAllResources(false))
				resource.addListener(forwarding);
		});
	}

	/**
	 * Looks up which {@link Dockable} has the given navigable as its {@link Dockable#nodeProperty()}.
	 *
	 * @param navigable
	 * 		Some navigable UI element.
	 *
	 * @return The dockable holding the navigable element, if any exists.
	 */
	@Nullable
	public Dockable lookupDockable(@Nonnull Navigable navigable) {
		return childrenToDockable.get(navigable);
	}

	/**
	 * Add a listener that observes the addition of {@link Navigable} components to the UI.
	 *
	 * @param listener
	 * 		Listener to add.
	 */
	public void addNavigableAddListener(@Nonnull NavigableAddListener listener) {
		PrioritySortable.add(addListeners, listener);
	}

	/**
	 * Remove a listener that observes the addition of {@link Navigable} components to the UI.
	 *
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeNavigableAddListener(@Nonnull NavigableAddListener listener) {
		addListeners.remove(listener);
	}

	/**
	 * Add a listener that observes the removal of {@link Navigable} components to the UI.
	 *
	 * @param listener
	 * 		Listener to add.
	 */
	public void addNavigableRemoveListener(@Nonnull NavigableRemoveListener listener) {
		PrioritySortable.add(removeListeners, listener);
	}

	/**
	 * Remove a listener that observes the removal of {@link Navigable} components to the UI.
	 *
	 * @param listener
	 * 		Listener to remove.
	 */
	public void removeNavigableRemoveListener(@Nonnull NavigableRemoveListener listener) {
		removeListeners.remove(listener);
	}

	@Nonnull
	@Override
	public PathNode<?> getPath() {
		return path;
	}

	@Nonnull
	@Override
	public Collection<Navigable> getNavigableChildren() {
		return Collections.unmodifiableCollection(childrenToDockable.keySet());
	}

	@Override
	public void requestFocus() {
		// no-op
	}

	@Override
	public void disable() {
		// no-op
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return ID;
	}

	@Nonnull
	@Override
	public NavigationManagerConfig getServiceConfig() {
		return config;
	}

	/**
	 * Listener to update {@link #childrenToDockable}.
	 */
	private class NavigableSpy implements ChangeListener<Node> {
		private final Dockable dockable;

		public NavigableSpy(@Nonnull Dockable dockable) {
			this.dockable = dockable;
		}

		@Override
		public void changed(ObservableValue<? extends Node> observable, Node oldValue, Node newValue) {
			remove(oldValue);
			add(newValue);
		}

		void add(@Nullable Node value) {
			if (value instanceof Navigable navigable && navigable.isTrackable()) {
				childrenToDockable.put(navigable, dockable);
				Unchecked.checkedForEach(addListeners, listener -> listener.onAdd(navigable),
						(listener, t) -> logger.error("Exception thrown when handling navigable added '{}'", navigable.getClass().getName(), t));
			}
		}

		void remove(@Nullable Node value) {
			if (value instanceof Navigable navigable) {
				childrenToDockable.remove(navigable);
				Unchecked.checkedForEach(removeListeners, listener -> listener.onRemove(navigable),
						(listener, t) -> logger.error("Exception thrown when handling navigable removed '{}'", navigable.getClass().getName(), t));

				// For dependent beans, we are likely not going to see them ever again.
				// Call disable to clean them up.
				if (value.getClass().getDeclaredAnnotation(Dependent.class) != null)
					navigable.disable();
			}
		}
	}

	/**
	 * Listener to forward updates in the workspace to {@link #getNavigableChildren() navigable components}.
	 */
	private class Forwarding implements ResourceJvmClassListener, ResourceAndroidClassListener, ResourceFileListener {
		private WorkspacePathNode workspacePath;

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			// Handle forwarding updates to remapped classes
			MappingResults mappingResults = cls.getPropertyValueOrNull(RemapOriginTaskProperty.KEY);
			if (mappingResults != null) {
				ClassPathNode preMappingPath = mappingResults.getPreMappingPath(cls.getName());
				if (preMappingPath != null) {
					ClassPathNode postMappingPath = workspacePath.child(resource).child(bundle).child(cls.getPackageName()).child(cls);
					for (Navigable navigable : getNavigableChildrenByPath(preMappingPath))
						if (navigable instanceof UpdatableNavigable updatable)
							updatable.onUpdatePath(postMappingPath);
				}
			}
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo oldCls, @Nonnull JvmClassInfo newCls) {
			BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
			ClassPathNode path = bundlePath.child(oldCls.getPackageName()).child(oldCls);
			for (Navigable navigable : getNavigableChildrenByPath(path))
				if (navigable instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(bundlePath.child(newCls.getPackageName()).child(newCls));
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull JvmClassBundle bundle, @Nonnull JvmClassInfo cls) {
			ClassPathNode path = workspacePath.child(resource).child(bundle).child(cls.getPackageName()).child(cls);
			FxThreadUtil.run(() -> {
				for (Navigable navigable : getNavigableChildrenByPath(path))
					navigable.disable();
			});
		}

		@Override
		public void onNewClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			// no-op
		}

		@Override
		public void onUpdateClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo oldCls, @Nonnull AndroidClassInfo newCls) {
			BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
			ClassPathNode path = bundlePath.child(oldCls.getPackageName()).child(oldCls);
			for (Navigable navigable : getNavigableChildrenByPath(path))
				if (navigable instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(bundlePath.child(newCls.getPackageName()).child(newCls));
		}

		@Override
		public void onRemoveClass(@Nonnull WorkspaceResource resource, @Nonnull AndroidClassBundle bundle, @Nonnull AndroidClassInfo cls) {
			ClassPathNode path = workspacePath.child(resource).child(bundle).child(cls.getPackageName()).child(cls);
			FxThreadUtil.run(() -> {
				for (Navigable navigable : getNavigableChildrenByPath(path))
					navigable.disable();
			});
		}

		@Override
		public void onNewFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
			// no-op
		}

		@Override
		public void onUpdateFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo oldFile, @Nonnull FileInfo newFile) {
			BundlePathNode bundlePath = workspacePath.child(resource).child(bundle);
			FilePathNode path = bundlePath.child(oldFile.getDirectoryName()).child(oldFile);
			for (Navigable navigable : getNavigableChildrenByPath(path))
				if (navigable instanceof UpdatableNavigable updatable)
					updatable.onUpdatePath(bundlePath.child(newFile.getDirectoryName()).child(newFile));
		}

		@Override
		public void onRemoveFile(@Nonnull WorkspaceResource resource, @Nonnull FileBundle bundle, @Nonnull FileInfo file) {
			FilePathNode path = workspacePath.child(resource).child(bundle).child(file.getDirectoryName()).child(file);
			FxThreadUtil.run(() -> {
				for (Navigable navigable : getNavigableChildrenByPath(path))
					navigable.disable();
			});
		}
	}

	/**
	 * Dummy node for initial state of {@link #path}.
	 */
	private static class DummyInitialNode extends AbstractPathNode<Object, Object> {
		private DummyInitialNode() {
			super("dummy", null, new Object());
		}

		@Nonnull
		@Override
		public Set<String> directParentTypeIds() {
			return Collections.emptySet();
		}

		@Override
		public int localCompare(PathNode<?> o) {
			return -1;
		}
	}
}
