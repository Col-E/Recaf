package me.coley.recaf.presentation;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.ui.control.tree.item.RootItem;
import me.coley.recaf.ui.panel.WorkspacePanel;
import me.coley.recaf.ui.window.MainWindow;
import me.coley.recaf.util.Threads;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.ClassInfo;
import me.coley.recaf.workspace.resource.FileInfo;
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
		// TODO: Prompt and make sure user actually wants to close it
		return true;
	}

	@Override
	public void openWorkspace(Workspace workspace) {
		Workspace oldWorkspace = getWorkspacePanel().getWorkspace();
		getWorkspacePanel().onNewWorkspace(oldWorkspace, workspace);
		// Update root when workspace updates libraries
		// Run on the UI thread (delayed) so it gets called after the new root node is set (which also is on UI thread)
		Threads.runFxDelayed(10, () -> {
			RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
			workspace.addListener(root);
		});
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onNewClass(resource, newValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onUpdateClass(resource, oldValue, newValue);
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onRemoveClass(resource, oldValue);
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onNewFile(resource, newValue);
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onUpdateFile(resource, oldValue, newValue);
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		RootItem root = (RootItem) getWorkspacePanel().getTree().getRoot();
		root.onRemoveFile(resource, oldValue);
	}

	private static MainWindow getMainWindow() {
		return RecafUI.getWindows().getMainWindow();
	}

	private static WorkspacePanel getWorkspacePanel() {
		return getMainWindow().getWorkspacePanel();
	}
}
