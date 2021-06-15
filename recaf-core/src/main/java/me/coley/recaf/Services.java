package me.coley.recaf;

import me.coley.recaf.code.parse.WorkspaceTypeSolver;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.workspace.Workspace;

/**
 * Wrapper of multiple services that are provided by a controller.
 * Placing them in here keeps the actual {@link Controller} class minimal.
 *
 * @author Matt Coley
 */
public class Services {
	private final DecompileManager decompileManager;
	private InheritanceGraph inheritanceGraph;
	private WorkspaceTypeSolver typeSolver;

	/**
	 * Initialize services.
	 *
	 * @param controller
	 * 		Parent controller instance.
	 */
	Services(Controller controller) {
		decompileManager = new DecompileManager();
	}

	/**
	 * @return The decompiler manager.
	 */
	public DecompileManager getDecompileManager() {
		return decompileManager;
	}

	/**
	 * @return Inheritance graph of the {@link Controller#getWorkspace() current workspace}.
	 * If no workspace is set, the this will be {@code null}.
	 */
	public InheritanceGraph getInheritanceGraph() {
		return inheritanceGraph;
	}

	/**
	 * @return A JavaParser type solver that pulls from the {@link Controller#getWorkspace() current workspace}.
	 * If no workspace is set, the this will be {@code null}.
	 */
	public WorkspaceTypeSolver getTypeSolver() {
		return typeSolver;
	}

	/**
	 * Update services that are workspace-oriented.
	 *
	 * @param workspace
	 * 		New parent workspace in the controller.
	 */
	void updateWorkspace(Workspace workspace) {
		if (workspace == null) {
			inheritanceGraph = null;
			typeSolver = null;
		} else {
			inheritanceGraph = new InheritanceGraph(workspace);
			typeSolver = new WorkspaceTypeSolver(workspace);
		}
	}
}
