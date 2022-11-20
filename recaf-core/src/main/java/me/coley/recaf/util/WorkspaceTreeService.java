package me.coley.recaf.util;

import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.util.threading.ThreadUtil;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.WorkspaceListener;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.ResourceClassListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Keeps an up-to-date instance of {@link ClasspathUtil.Tree} for the current {@link Workspace}.
 *
 * @author Justus Garbe
 */
public class WorkspaceTreeService implements WorkspaceListener, ResourceClassListener {
	private final Workspace workspace;
	private CompletableFuture<ClasspathUtil.Tree> future;

	/**
	 * @param workspace
	 * 		Initial workspace.
	 */
	public WorkspaceTreeService(Workspace workspace) {
		this.workspace = workspace;
		scheduleNewTree();
		workspace.addListener(this);
		for (Resource resource : workspace.getResources())
			resource.addClassListener(this);
	}

	@Override
	public void onAddLibrary(Workspace workspace, Resource library) {
		scheduleTreeUpdate(tree -> Streams.interruptable(library.getClasses().values().stream())
				.map(ClassInfo::getName)
				.forEach(tree::visitPath));
	}

	@Override
	public void onRemoveLibrary(Workspace workspace, Resource library) {
		scheduleTreeUpdate(tree -> Streams.interruptable(library.getClasses().values().stream())
				.map(ClassInfo::getName)
				.forEach(tree::trimPath));
	}

	@Override
	public void onNewClass(Resource resource, ClassInfo newValue) {
		scheduleTreeUpdate(tree -> tree.visitPath(newValue.getName()));
	}

	@Override
	public void onRemoveClass(Resource resource, ClassInfo oldValue) {
		scheduleTreeUpdate(tree -> tree.trimPath(oldValue.getName()));
	}

	@Override
	public void onUpdateClass(Resource resource, ClassInfo oldValue, ClassInfo newValue) {
		// no-op
	}

	private void scheduleTreeUpdate(Consumer<ClasspathUtil.Tree> operation) {
		if (future == null)
			scheduleNewTree();
		// Once the tree is complete, schedule an update.
		if (future.isDone()) {
			// Act immediately.
			ClasspathUtil.Tree tree = getCurrentClassTree();
			operation.accept(tree);
		} else {
			// Schedule it.
			future.thenRun(() -> {
				ClasspathUtil.Tree tree = Unchecked.get(() -> future.get());
				operation.accept(tree);
			});
		}
	}

	private void scheduleNewTree() {
		if (future != null)
			future.cancel(true);
		// start tree build on new thread to avoid locking ui thread
		future = ThreadUtil.run(() -> buildClassTree(workspace));
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
		return classTree;
	}
}
