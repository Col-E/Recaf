package me.coley.recaf;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.*;

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
	 */
	public void setWorkspace(Workspace workspace) {
		// Close current workspace
		if (this.workspace != null) {
			if (presentation.workspaceLayer().closeWorkspace(this.workspace)) {
				this.workspace.cleanup();
			} else {
				// Presentation layer cancelled closing the workspace
				return;
			}
		}
		// Set new workspace & update services
		Workspace oldWorkspace = this.workspace;
		this.workspace = workspace;
		this.services.updateWorkspace(workspace);
		if (workspace != null) {
			addPresentationLayerListeners(workspace);
			addServiceListeners(workspace);
		}
		listeners.forEach(listener -> listener.onNewWorkspace(oldWorkspace, workspace));
		// Open new workspace, if non-null
		if (workspace != null) {
			presentation.workspaceLayer().openWorkspace(workspace);
		}
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

	/**
	 * Adds listeners to the workspace that update the relevant services when changes occur.
	 *
	 * @param workspace
	 * 		Workspace to add listeners to.
	 */
	private void addServiceListeners(Workspace workspace) {
		InheritanceGraph graph = getServices().getInheritanceGraph();
		ResourceClassListener classListener = new ResourceClassListener() {
			@Override
			public void onNewClass(Resource resource, ClassInfo newValue) {
				graph.populateParentToChildLookup(newValue);
			}

			@Override
			public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
				if (!oldValue.getSuperName().equals(newValue.getSuperName())) {
					graph.removeParentToChildLookup(oldValue.getName(), oldValue.getSuperName());
					graph.populateParentToChildLookup(newValue.getName(), newValue.getSuperName());
				}
				Set<String> interfaces = new HashSet<>(oldValue.getInterfaces());
				interfaces.addAll(newValue.getInterfaces());
				for (String itf : interfaces) {
					boolean oldHas = oldValue.getInterfaces().contains(itf);
					boolean newHas = newValue.getInterfaces().contains(itf);
					if (oldHas && !newHas) {
						graph.removeParentToChildLookup(oldValue.getName(), itf);
					} else if (!oldHas && newHas) {
						graph.populateParentToChildLookup(newValue.getName(), itf);
					}
				}
			}

			@Override
			public void onRemoveClass(Resource resource, ClassInfo oldValue) {
				graph.removeParentToChildLookup(oldValue);
			}
		};
		ResourceDexClassListener dexListener = new ResourceDexClassListener() {
			@Override
			public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
				graph.populateParentToChildLookup(newValue);
			}

			@Override
			public void onUpdateDexClass(Resource resource, String dexName,
										 DexClassInfo oldValue, DexClassInfo newValue) {
				if (!oldValue.getSuperName().equals(newValue.getSuperName())) {
					graph.removeParentToChildLookup(oldValue.getName(), oldValue.getSuperName());
					graph.populateParentToChildLookup(newValue.getName(), newValue.getSuperName());
				}
				Set<String> interfaces = new HashSet<>(oldValue.getInterfaces());
				interfaces.addAll(newValue.getInterfaces());
				for (String itf : interfaces) {
					boolean oldHas = oldValue.getInterfaces().contains(itf);
					boolean newHas = newValue.getInterfaces().contains(itf);
					if (oldHas && !newHas) {
						graph.removeParentToChildLookup(oldValue.getName(), itf);
					} else if (!oldHas && newHas) {
						graph.populateParentToChildLookup(newValue.getName(), itf);
					}
				}
			}

			@Override
			public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
				graph.removeParentToChildLookup(oldValue);
			}
		};
		// Add
		List<Resource> resources = new ArrayList<>(workspace.getResources().getLibraries());
		resources.add(workspace.getResources().getPrimary());
		resources.forEach(resource -> {
			resource.addClassListener(classListener);
			resource.addDexListener(dexListener);
		});
	}
}
