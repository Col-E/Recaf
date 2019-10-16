package me.coley.recaf.ui.controls;

import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import me.coley.recaf.ui.controls.tree.*;
import me.coley.recaf.workspace.JavaResource;
import me.coley.recaf.workspace.Workspace;

import java.util.ArrayList;
import java.util.List;

/* TODO: Account for the following
 *  - User adds/removes a library to workspace
 *  - User adds/remove/renames a class/resource
 *  - User drops file on space
 *    - Loads new workspace
 */

/**
 * Navigator for a workspace.
 *
 * @author Matt
 */
@SuppressWarnings("unchecked")
public class WorkspaceNavigator extends BorderPane {
	private final Workspace workspace;
	private TreeView tree = new TreeView();

	/**
	 * @param workspace
	 * 		The workspace to generate navigation for.
	 */
	public WorkspaceNavigator(Workspace workspace) {
		this.workspace = workspace;
		setCenter(tree);
		tree.setCellFactory(param -> new ResourceCell());
		tree.setRoot(new RootItem(workspace.getPrimary()));
		tree.getRoot().setExpanded(true);
	}

	// TODO: Allow user to switch between them by combo on the root node or something
	private List<JavaResource> resources() {
		List<JavaResource> list = new ArrayList<>();
		list.add(workspace.getPrimary());
		list.addAll(workspace.getLibraries());
		return list;
	}
}
