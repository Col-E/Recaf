package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.ClassRepresentation;
import me.coley.recaf.ui.behavior.FileRepresentation;
import me.coley.recaf.ui.control.tree.item.RootItem;
import me.coley.recaf.ui.panel.DockingRootPane;
import me.coley.recaf.ui.panel.WorkspacePanel;
import me.coley.recaf.ui.prompt.WorkspaceClosePrompt;
import me.coley.recaf.ui.window.MainWindow;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

/**
 * Gui workspace presentation implementation. Orchestrates UI behavior in response to common workspace operations.
 *
 * @author Matt Coley
 */
public class GuiWorkspacePresentation implements Presentation.WorkspacePresentation {
	/**
	 * @param controller
	 * 		Parent.
	 */
	public GuiWorkspacePresentation(Controller controller) {
	}

	@Override
	public boolean closeWorkspace(Workspace workspace) {
		boolean doClose = false;
		if (Configs.display().promptCloseWorkspace) {
			doClose = WorkspaceClosePrompt.prompt(workspace);
		} else {
			doClose = true;
		}
		// Close all workspace items if close is allowed.
		if (doClose) {
			// TODO: Close all workspace tabs
		}
		return doClose;
	}

	@Override
	public void openWorkspace(Workspace workspace) {
		Workspace oldWorkspace = getWorkspacePanel().getWorkspace();
		getWorkspacePanel().onNewWorkspace(oldWorkspace, workspace);
		// Update root when workspace updates libraries
		// Run on the UI thread (delayed) so it gets called after the new root node is set (which also is on UI thread)
		Threads.runFxDelayed(10, () -> {
			RootItem root = getWorkspacePanel().getTree().getRootItem();
			workspace.addListener(root);
		});
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onNewClass(resource, newValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onUpdateClass(resource, oldValue, newValue);
		// Refresh class representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof ClassRepresentation) {
				((ClassRepresentation) tab.getContent()).onUpdate(newValue);
			}
		});
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onRemoveClass(resource, oldValue);
		// Refresh class representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof ClassRepresentation) {
				tab.getTabPane().getTabs().remove(tab);
			}
		});
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onNewDexClass(resource, dexName, newValue);
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onUpdateDexClass(resource, dexName, oldValue, newValue);
		// Refresh class representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof ClassRepresentation) {
				((ClassRepresentation) tab.getContent()).onUpdate(newValue);
			}
		});
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onRemoveDexClass(resource, dexName, oldValue);
		// Refresh class representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof ClassRepresentation) {
				tab.getTabPane().getTabs().remove(tab);
			}
		});
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onNewFile(resource, newValue);
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onUpdateFile(resource, oldValue, newValue);
		// Refresh file representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof FileRepresentation) {
				((FileRepresentation) tab.getContent()).onUpdate(newValue);
			}
		});
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		// Update tree
		RootItem root = getWorkspacePanel().getTree().getRootItem();
		root.onRemoveFile(resource, oldValue);
		// Refresh file representation
		DockingRootPane docking = getDocking();
		docking.findInfoTabs(oldValue).forEach(tab -> {
			if (tab.getContent() instanceof FileRepresentation) {
				tab.getTabPane().getTabs().remove(tab);
			}
		});
	}

	private static MainWindow getMainWindow() {
		return RecafUI.getWindows().getMainWindow();
	}

	private static WorkspacePanel getWorkspacePanel() {
		return getMainWindow().getWorkspacePanel();
	}

	private static DockingRootPane getDocking() {
		return getMainWindow().getDockingRootPane();
	}
}
