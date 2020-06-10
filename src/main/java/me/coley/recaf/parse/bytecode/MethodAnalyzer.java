package me.coley.recaf.parse.bytecode;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.recaf.Recaf;

/**
 * {@link SimAnalyzer} extension that implements a {@link #createTypeChecker() type checker}
 * using Recaf's workspaces.
 */
public class MethodAnalyzer extends SimAnalyzer {

	/**
	 * Create method analyzer.
	 *
	 * @param interpreter
	 * 		Interpreter to use.
	 */
	public MethodAnalyzer(SimInterpreter interpreter) {
		super(interpreter);
	}

	@Override
	protected TypeChecker createTypeChecker() {
		return (parent, child) -> Recaf.getCurrentWorkspace().getHierarchyGraph()
				.getAllParents(child.getInternalName())
					.anyMatch(n -> n != null && n.equals(parent.getInternalName()));
	}
}
