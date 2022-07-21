package me.coley.recaf.ui.pane.assembler.suggestion;

import me.coley.recaf.ControllerListener;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.ClasspathUtil;
import me.coley.recaf.util.Streams;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;

import java.util.concurrent.CompletableFuture;

public class WorkspaceClassTree implements ControllerListener, WorkspaceListener {
	private static CompletableFuture<ClasspathUtil.Tree> future; // TODO bad static

	@Override
	public void onNewWorkspace(Workspace oldWorkspace, Workspace newWorkspace) {
		rebuildClassTree(newWorkspace);
		newWorkspace.addListener(this);
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		rebuildClassTree(workspace);
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		rebuildClassTree(workspace);
	}

	private void rebuildClassTree(Workspace workspace) {
		if (future != null) {
			future.cancel(true);
		}
		future = ThreadUtil.run(() -> buildClassTree(workspace)); // start tree build on new thread to avoid locking ui thread
	}

	private ClasspathUtil.Tree buildClassTree(Workspace workspace) {
		ClasspathUtil.Tree classTree = new ClasspathUtil.Tree(null, "");
		Streams.interruptable(workspace.getResources().getClasses())
				.map(ClassInfo::getName)
				.forEach(classTree::visitPath);
		classTree.freeze();
		return classTree;
	}

	public static ClasspathUtil.Tree getCurrentClassTree() {
		return future.join();
	}
}
