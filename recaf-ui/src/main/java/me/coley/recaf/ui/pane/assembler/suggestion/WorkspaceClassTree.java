package me.coley.recaf.ui.pane.assembler.suggestion;

import me.coley.recaf.ControllerListener;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;

public class WorkspaceClassTree implements ControllerListener, WorkspaceListener {

	private static ClasspathUtil.Tree classTree = new ClasspathUtil.Tree(null ,"");

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		rebuildClassTree();
		newWorkspace.addListener(this);
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		rebuildClassTree();
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		rebuildClassTree();
	}

	public void rebuildClassTree() {
		ThreadUtil.run(this::buildClassTree); // start tree build on new thread to avoid locking ui thread
	}

	public void buildClassTree() {
		for (ClassInfo aClass : RecafUI.getController().getWorkspace().getResources().getClasses()) {
			classTree.visitPath(aClass.getName());
		}
		classTree.freeze();
	}

	public static ClasspathUtil.Tree getCurrentClassTree() {
		return classTree;
	}



}
