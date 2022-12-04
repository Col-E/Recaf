package me.coley.recaf;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.coley.recaf.cdi.WorkspaceBeanContext;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.presentation.Presentation;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceManager;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;
import me.coley.recaf.workspace.resource.ResourceDexClassListener;
import me.coley.recaf.workspace.resource.ResourceFileListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Matt Coley
 */
@ApplicationScoped
public class Controller {
	private static final WorkspaceBeanContext workspaceBeanContext = WorkspaceBeanContext.getInstance();
	private final WorkspaceManager workspaceManager;
	private Presentation presentation;

	@Inject
	public Controller(WorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
		// Ensure presentation layer is forwarded listener events
		workspaceManager.addWorkspaceOpenListener(this::addPresentationLayerListeners);
		// Ensure CDI system for workspace scoped beans are forwarded events
		workspaceManager.addWorkspaceOpenListener(workspaceBeanContext);
		workspaceManager.addWorkspaceCloseListener(workspaceBeanContext);
	}

	/**
	 * @return Presentation implementation.
	 */
	public Presentation getPresentation() {
		return presentation;
	}

	/**
	 * @param presentation
	 * 		Presentation implementation.
	 */
	public void setPresentation(Presentation presentation) {
		if (presentation == null) throw new IllegalArgumentException("Presentation cannot be null!");
		if (this.presentation != null) throw new IllegalStateException("Presentation already set!");
		this.presentation = presentation;
		workspaceManager.addListener(presentation.workspaceLayer());
	}

	/**
	 * Adds listeners to the workspace that invoke the appropriate presentation layer methods.
	 *
	 * @param workspace
	 * 		Workspace to add listeners to.
	 */
	private void addPresentationLayerListeners(Workspace workspace) {
		Presentation.WorkspacePresentation presentation = getPresentation().workspaceLayer();
		ResourceClassListener classListener = new ResourceClassListener() {
			@Override
			public void onNewClass(Resource resource, ClassInfo newValue) {
				presentation.onNewClass(resource, newValue);
			}

			@Override
			public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
				presentation.onUpdateClass(resource, oldValue, newValue);
			}

			@Override
			public void onRemoveClass(Resource resource, ClassInfo oldValue) {
				presentation.onRemoveClass(resource, oldValue);
			}
		};
		ResourceDexClassListener dexListener = new ResourceDexClassListener() {
			@Override
			public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
				presentation.onNewDexClass(resource, dexName, newValue);
			}

			@Override
			public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
				presentation.onRemoveDexClass(resource, dexName, oldValue);
			}

			@Override
			public void onUpdateDexClass(Resource resource, String dexName,
										 DexClassInfo oldValue, DexClassInfo newValue) {
				presentation.onUpdateDexClass(resource, dexName, oldValue, newValue);
			}
		};
		ResourceFileListener fileListener = new ResourceFileListener() {
			@Override
			public void onNewFile(Resource resource, FileInfo newValue) {
				presentation.onNewFile(resource, newValue);
			}

			@Override
			public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
				presentation.onUpdateFile(resource, oldValue, newValue);
			}

			@Override
			public void onRemoveFile(Resource resource, FileInfo oldValue) {
				presentation.onRemoveFile(resource, oldValue);
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
