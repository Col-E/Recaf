package me.coley.recaf;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Backbone of all internal Recaf handling.
 *
 * @author Matt Coley
 */
public class Controller {
	private final Set<ControllerListener> listeners = new HashSet<>();
	private final Services services = new Services(this);
	private final Presentation presentation;
	private Workspace workspace;

	/**
	 * @param presentation
	 * 		Presentation implementation.
	 */
	public Controller(Presentation presentation) {
		this.presentation = presentation;
	}

	/**
	 * @return Controller services.
	 */
	public Services getServices() {
		return services;
	}

	/**
	 * @return Presentation implementation.
	 */
	public Presentation getPresentation() {
		return presentation;
	}

	/**
	 * @return Current open workspace.
	 */
	public Workspace getWorkspace() {
		return workspace;
	}

	/**
	 * Add a new controller listener.
	 *
	 * @param listener
	 * 		New controller listener.
	 */
	public void addListener(ControllerListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove a controller listener.
	 *
	 * @param listener
	 * 		Controller listener to remove.
	 */
	public void removeListener(ControllerListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Set the current workspace. Will close an existing one if it exists.
	 *
	 * @param workspace
	 * 		New workspace, or {@code null} to close the workspace without a replacement.
	 *
	 * @return {@code true} when the workspace was updated.
	 * {@code false} when the change was rejected.
	 */
	public boolean setWorkspace(Workspace workspace) {
		// Close current workspace
		if (this.workspace != null) {
			if (presentation.workspaceLayer().closeWorkspace(this.workspace)) {
				this.workspace.cleanup();
			} else {
				// Presentation layer cancelled closing the workspace
				return false;
			}
		}
		// Set new workspace & update services
		Workspace oldWorkspace = this.workspace;
		this.workspace = workspace;
		this.services.updateWorkspace(workspace);
		if (workspace != null) {
			addPresentationLayerListeners(workspace);
		}
		listeners.forEach(listener -> listener.onNewWorkspace(oldWorkspace, workspace));
		// Open new workspace, if non-null
		if (workspace != null) {
			presentation.workspaceLayer().openWorkspace(workspace);
		}
		return true;
	}

	/**
	 * Adds listeners to the workspace that invoke the appropriate presentation layer methods.
	 *
	 * @param workspace
	 * 		Workspace to add listeners to.
	 */
	private void addPresentationLayerListeners(Workspace workspace) {
		ResourceClassListener classListener = new ResourceClassListener() {
			@Override
			public void onNewClass(Resource resource, ClassInfo newValue) {
				getPresentation().workspaceLayer().onNewClass(resource, newValue);
			}

			@Override
			public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
				getPresentation().workspaceLayer().onUpdateClass(resource, oldValue, newValue);
			}

			@Override
			public void onRemoveClass(Resource resource, ClassInfo oldValue) {
				getPresentation().workspaceLayer().onRemoveClass(resource, oldValue);
			}
		};
		ResourceDexClassListener dexListener = new ResourceDexClassListener() {
			@Override
			public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
				getPresentation().workspaceLayer().onNewDexClass(resource, dexName, newValue);
			}

			@Override
			public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
				getPresentation().workspaceLayer().onRemoveDexClass(resource, dexName, oldValue);
			}

			@Override
			public void onUpdateDexClass(Resource resource, String dexName,
										 DexClassInfo oldValue, DexClassInfo newValue) {
				getPresentation().workspaceLayer().onUpdateDexClass(resource, dexName, oldValue, newValue);
			}
		};
		ResourceFileListener fileListener = new ResourceFileListener() {
			@Override
			public void onNewFile(Resource resource, FileInfo newValue) {
				getPresentation().workspaceLayer().onNewFile(resource, newValue);
			}

			@Override
			public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
				getPresentation().workspaceLayer().onUpdateFile(resource, oldValue, newValue);
			}

			@Override
			public void onRemoveFile(Resource resource, FileInfo oldValue) {
				getPresentation().workspaceLayer().onRemoveFile(resource, oldValue);
			}
		};
		// Add
		List<Resource> resources = new ArrayList<>(workspace.getResources().getLibraries());
		resources.add(workspace.getResources().getPrimary());
		resources.forEach(resource -> {
			resource.addClassListener(classListener);
			resource.addDexListener(dexListener);
			resource.addFileListener(fileListener);
		});
	}
}
