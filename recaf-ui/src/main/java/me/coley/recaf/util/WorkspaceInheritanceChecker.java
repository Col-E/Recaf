package me.coley.recaf.util;

import me.coley.recaf.Controller;
import me.coley.recaf.RecafUI;
import me.coley.recaf.assemble.util.InheritanceChecker;
import me.coley.recaf.assemble.util.ReflectiveInheritanceChecker;
import me.coley.recaf.graph.InheritanceGraph;

/**
 * Type checker that pulls info from the current workspace.
 *
 * @author Matt Coley
 * @see ReflectiveInheritanceChecker Fallback implementation.
 */
public class WorkspaceInheritanceChecker implements InheritanceChecker {
	private static final WorkspaceInheritanceChecker INSTANCE = new WorkspaceInheritanceChecker();

	private WorkspaceInheritanceChecker() {
		// disallow creation
	}

	/**
	 * @return Singleton instance.
	 */
	public static WorkspaceInheritanceChecker getInstance() {
		return INSTANCE;
	}

	@Override
	public String getCommonType(String class1, String class2) {
		Controller controller = RecafUI.getController();
		InheritanceGraph graph = controller.getServices().getInheritanceGraph();
		if (graph != null) {
			return graph.getCommon(class1, class2);
		} else {
			// Fallback attempt
			return ReflectiveInheritanceChecker.getInstance().getCommonType(class1, class2);
		}
	}
}
