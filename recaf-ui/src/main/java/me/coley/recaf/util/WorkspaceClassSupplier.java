package me.coley.recaf.util;

import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.util.ClassSupplier;
import me.coley.recaf.assemble.util.ReflectiveClassSupplier;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.Workspace;

/**
 * Class supplier from the current workspace.
 *
 * @author Matt Coley
 */
public class WorkspaceClassSupplier implements ClassSupplier {
	private static final WorkspaceClassSupplier INSTANCE = new WorkspaceClassSupplier();
	private static final ReflectiveClassSupplier FALLBACK = ReflectiveClassSupplier.getInstance();

	private WorkspaceClassSupplier() {
		// prevent new instances
	}

	@Override
	public byte[] getClass(String name) throws ClassNotFoundException {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null) {
			// No workspace, check if the class exists in runtime.
			return FALLBACK.getClass(name);
		}
		ClassInfo data = workspace.getResources().getClass(name);
		if (data == null) {
			// Since the workspace has an automatic component to check runtime existence, we do not need to
			// delegate to the fallback here. We can fail.
			throw new ClassNotFoundException(name);
		}
		return data.getValue();
	}

	/**
	 * @return Singleton instance.
	 */
	public static WorkspaceClassSupplier getInstance() {
		return INSTANCE;
	}
}
