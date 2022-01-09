package me.coley.recaf;

import me.coley.recaf.compile.CompileManager;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.workspace.Workspace;

/**
 * Wrapper of multiple services that are provided by a controller.
 * Placing them in here keeps the actual {@link Controller} class minimal.
 *
 * @author Matt Coley
 */
public class Services {
	private final CompileManager compileManager;
	private final DecompileManager decompileManager;
	private final MappingsManager mappingsManager;
	private InheritanceGraph inheritanceGraph;
	private WorkspaceTypeSolver typeSolver;
	private JavaParserHelper javaParserHelper;

	/**
	 * Initialize services.
	 *
	 * @param controller
	 * 		Parent controller instance.
	 */
	Services(Controller controller) {
		compileManager = new CompileManager();
		decompileManager = new DecompileManager();
		mappingsManager = new MappingsManager();
	}

	/**
	 * @return The compiler manager.
	 */
	public CompileManager getCompileManager() {
		return compileManager;
	}

	/**
	 * @return The decompiler manager.
	 */
	public DecompileManager getDecompileManager() {
		return decompileManager;
	}

	/**
	 * @return The mappings manager.
	 */
	public MappingsManager getMappingsManager() {
		return mappingsManager;
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
	 * @return A JavaParser helper that handles parsing source code into an AST.
	 * If no workspace is set, the this will be {@code null}.
	 */
	public JavaParserHelper getJavaParserHelper() {
		return javaParserHelper;
	}

	/**
	 * Update services that are workspace-oriented.
	 *
	 * @param workspace
	 * 		New parent workspace in the controller.
	 */
	void updateWorkspace(Workspace workspace) {
		mappingsManager.clearAggregated();
		if (workspace == null) {
			inheritanceGraph = null;
			typeSolver = null;
			javaParserHelper = null;
		} else {
			inheritanceGraph = new InheritanceGraph(workspace);
			typeSolver = new WorkspaceTypeSolver(workspace);
			javaParserHelper = JavaParserHelper.create(typeSolver);
		}
	}
}
