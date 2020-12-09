package me.coley.recaf.parse.bytecode;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeChecker;
import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.TypeUtil;
import me.coley.recaf.Recaf;
import me.coley.recaf.graph.inheritance.HierarchyGraph;
import org.objectweb.asm.Type;

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
		return (parent, child) -> getGraph()
				.getAllParents(child.getInternalName())
					.anyMatch(n -> n != null && n.equals(parent.getInternalName()));
	}

	@Override
	protected TypeResolver createTypeResolver() {
		return new TypeResolver() {
			@Override
			public Type common(Type type1, Type type2) {
				String common = getGraph().getCommon(type1.getInternalName(), type2.getInternalName());
				if (common != null)
					return Type.getObjectType(common);
				return TypeUtil.OBJECT_TYPE;
			}

			@Override
			public Type commonException(Type type1, Type type2) {
				String common = getGraph().getCommon(type1.getInternalName(), type2.getInternalName());
				if (common != null)
					return Type.getObjectType(common);
				return TypeUtil.EXCEPTION_TYPE;
			}
		};
	}

	private static HierarchyGraph getGraph() {
		return Recaf.getCurrentWorkspace().getHierarchyGraph();
	}
}
