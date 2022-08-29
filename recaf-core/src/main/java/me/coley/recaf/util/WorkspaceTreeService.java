package me.coley.recaf.util;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;

import java.util.concurrent.CompletableFuture;

/**
 * Keeps an up-to-date instance of {@link ClasspathUtil.Tree} for the current {@link Workspace}.
 *
 * @author Nowilltolife
 */
public class WorkspaceTreeService implements WorkspaceListener {
	private CompletableFuture<ClasspathUtil.Tree> future;

	/**
	 * @param workspace
	 * 		Initial workspace.
	 */
	public WorkspaceTreeService(Workspace workspace) {
		rebuildClassTree(workspace);
		workspace.addListener(this);
		// TODO: Update tree when classes are added/removed
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

	/**
	 * @return Current workspace tree model.
	 */
	public ClasspathUtil.Tree getCurrentClassTree() {
		return future.join();
	}

	private static ClasspathUtil.Tree buildClassTree(Workspace workspace) {
		ClasspathUtil.Tree classTree = new ClasspathUtil.Tree(null, "");
		Streams.interruptable(workspace.getResources().getClasses())
				.map(ClassInfo::getName)
				.forEach(classTree::visitPath);
		classTree.freeze();
		return classTree;
	}
}
