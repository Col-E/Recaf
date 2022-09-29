package me.coley.recaf.presentation;

import javafx.scene.Node;
import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.DexClassInfo;
import me.coley.recaf.code.FileInfo;
import me.coley.recaf.config.Configs;
import me.coley.recaf.ui.behavior.Cleanable;
import me.coley.recaf.ui.behavior.Representation;
import me.coley.recaf.ui.control.NavigationBar;
import me.coley.recaf.ui.control.tree.item.WorkspaceRootItem;
import me.coley.recaf.ui.docking.DockTab;
import me.coley.recaf.ui.docking.RecafDockingManager;
import me.coley.recaf.ui.docking.impl.ClassTab;
import me.coley.recaf.ui.docking.impl.FileTab;
import me.coley.recaf.ui.pane.WorkspacePane;
import me.coley.recaf.ui.prompt.WorkspaceClosePrompt;
import me.coley.recaf.ui.window.MainMenu;
import me.coley.recaf.ui.window.MainWindow;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;

import java.util.List;

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
		boolean doClose;
		if (Configs.display().promptCloseWorkspace) {
			doClose = WorkspaceClosePrompt.prompt(workspace);
		} else {
			doClose = true;
		}
		// Close all workspace items if close is allowed.
		if (doClose) {
			// Update recent workspaces list in main menu.
			// We do this in the "close" section because its makes it easy to assume
			// that this is the final form of the workspace.
			if (Configs.recentWorkspaces().canSerialize(workspace)) {
				Configs.recentWorkspaces().addWorkspace(workspace);
				getMainMenu().refreshRecent();
			}
			// Close workspace tree
			Workspace oldWorkspace = getWorkspacePane().getWorkspace();
			getWorkspacePane().onNewWorkspace(oldWorkspace, null);
			// Close workspace tabs
			List<DockTab> tabs = getDocking().getAllTabs();
			for (DockTab tab : tabs) {
				Node content = tab.getContent();
				// Cleanup the view if possible
				if (content instanceof Cleanable)
					((Cleanable) content).cleanup();
				// Remove the tab
				if (content instanceof Representation)
					tab.close();
			}

			// Clear the navbar
			NavigationBar.getInstance().clear();
		}
		return doClose;
	}

	@Override
	public void openWorkspace(Workspace workspace) {
		Workspace oldWorkspace = getWorkspacePane().getWorkspace();
		getWorkspacePane().onNewWorkspace(oldWorkspace, workspace);
		// Update root when workspace updates libraries
		// Run on the UI thread (delayed) so it gets called after the new root node is set (which also is on UI thread)
		FxThreadUtil.delayedRun(10, () -> {
			WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
			workspace.addListener(root);
		});
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onNewClass(resource, newValue);
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onUpdateClass(resource, oldValue, newValue);
		// Refresh class representation
		RecafDockingManager docking = getDocking();
		ClassTab tab = docking.getClassTabs().get(oldValue.getName());
		if (tab != null)
			tab.getClassRepresentation().onUpdate(newValue);
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		FxThreadUtil.run(() -> {
			WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
			root.onRemoveClass(resource, oldValue);
			// Refresh class representation
			RecafDockingManager docking = getDocking();
			ClassTab tab = docking.getClassTabs().get(oldValue.getName());
			if (tab != null)
				tab.close();
		});
	}

	@Override
	public void onNewDexClass(Resource resource, String dexName, DexClassInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onNewDexClass(resource, dexName, newValue);
	}

	@Override
	public void onUpdateDexClass(Resource resource, String dexName, DexClassInfo oldValue, DexClassInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onUpdateDexClass(resource, dexName, oldValue, newValue);
		// Refresh class representation
		RecafDockingManager docking = getDocking();
		ClassTab tab = docking.getClassTabs().get(oldValue.getName());
		if (tab != null)
			tab.getClassRepresentation().onUpdate(newValue);
	}

	@Override
	public void onRemoveDexClass(Resource resource, String dexName, DexClassInfo oldValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onRemoveDexClass(resource, dexName, oldValue);
		// Refresh class representation
		RecafDockingManager docking = getDocking();
		ClassTab tab = docking.getClassTabs().get(oldValue.getName());
		if (tab != null)
			tab.close();
	}

	@Override
	public void onNewFile(Resource resource, FileInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onNewFile(resource, newValue);
	}

	@Override
	public void onUpdateFile(Resource resource, FileInfo oldValue, FileInfo newValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onUpdateFile(resource, oldValue, newValue);
		// Refresh file representation
		RecafDockingManager docking = getDocking();
		FileTab tab = docking.getFileTabs().get(oldValue.getName());
		if (tab != null)
			tab.getFileRepresentation().onUpdate(newValue);
	}

	@Override
	public void onRemoveFile(Resource resource, FileInfo oldValue) {
		// Update tree
		WorkspaceRootItem root = getWorkspacePane().getTree().getRootItem();
		root.onRemoveFile(resource, oldValue);
		// Refresh file representation
		RecafDockingManager docking = getDocking();
		FileTab tab = docking.getFileTabs().get(oldValue.getName());
		if (tab != null)
			tab.close();
	}

	private static MainWindow getMainWindow() {
		return RecafUI.getWindows().getMainWindow();
	}

	private static MainMenu getMainMenu() {
		return MainMenu.getInstance();
	}

	private static WorkspacePane getWorkspacePane() {
		return WorkspacePane.getInstance();
	}

	private static RecafDockingManager getDocking() {
		return RecafDockingManager.getInstance();
	}
}
