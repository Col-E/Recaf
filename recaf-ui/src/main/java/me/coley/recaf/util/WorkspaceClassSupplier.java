package me.coley.recaf.util;

import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.compiler.ClassSupplier;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.workspace.Workspace;

/**
 * Class supplier from the current workspace.
 *
 * @author Matt Coley
 */
public class WorkspaceClassSupplier implements ClassSupplier {
	private static final WorkspaceClassSupplier INSTANCE = new WorkspaceClassSupplier();

	private WorkspaceClassSupplier() {
		// prevent new instances
	}

	@Override
	public byte[] getClass(String name) {
		Workspace workspace = RecafUI.getController().getWorkspace();
		if (workspace == null)
			return null;
		ClassInfo data = workspace.getResources().getClass(name);
		if (data == null)
			return null;
		return data.getValue();
	}

	/**
	 * @return Singleton instance.
	 */
	public static WorkspaceClassSupplier getInstance() {
		return INSTANCE;
	}
}
