package me.coley.recaf;

import me.coley.recaf.compile.CompilerManager;
import me.coley.recaf.decompile.DecompileManager;
import me.coley.recaf.graph.InheritanceGraph;
import me.coley.recaf.mapping.MappingsManager;
import me.coley.recaf.parse.JavaParserHelper;
import me.coley.recaf.parse.WorkspaceSymbolSolver;
import me.coley.recaf.ssvm.SsvmIntegration;
import me.coley.recaf.util.WorkspaceTreeService;
import me.coley.recaf.workspace.Workspace;

/**
 * Wrapper of multiple services that are provided by a controller.
 * Placing them in here keeps the actual {@link Controller} class minimal.
 *
 * @author Matt Coley
 */
public class Services {
	private final CompilerManager compilerManager;
	private final DecompileManager decompileManager;
	private final MappingsManager mappingsManager;
	private WorkspaceTreeService treeService;
	private SsvmIntegration ssvmIntegration;
	private InheritanceGraph inheritanceGraph;
	private WorkspaceSymbolSolver symbolSolver;
	private JavaParserHelper javaParserHelper;

	/**
	 * Initialize services.
	 *
	 * @param controller
	 * 		Parent controller instance.
	 */
	Services(Controller controller) {
		compilerManager = new CompilerManager();
		decompileManager = new DecompileManager();
		mappingsManager = new MappingsManager();
	}

	/**
	 * @return The compiler manager.
	 */
	public CompilerManager getCompilerManager() {
		return compilerManager;
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
	 * If no workspace is set, then this will be {@code null}.
	 */
	public InheritanceGraph getInheritanceGraph() {
		return inheritanceGraph;
	}

	/**
	 * @return A JavaParser symbol solver that pulls from the {@link Controller#getWorkspace() current workspace}.
	 * If no workspace is set, then this will be {@code null}.
	 */
	public WorkspaceSymbolSolver getSymbolSolver() {
		return symbolSolver;
	}

	/**
	 * @return A JavaParser helper that handles parsing source code into an AST.
	 * If no workspace is set, then this will be {@code null}.
	 */
	public JavaParserHelper getJavaParserHelper() {
		return javaParserHelper;
	}

	/**
	 * @return A wrapper around SSVM for easier integration into Recaf.
	 */
	public SsvmIntegration getSsvmIntegration() {
		return ssvmIntegration;
	}

	/**
	 * @return Provides access to the {@link Workspace} package and class names as a tree model.
	 */
	public WorkspaceTreeService getTreeService() {
		return treeService;
	}

	/**
	 * Update services that are workspace-oriented.
	 *
	 * @param workspace
	 * 		New parent workspace in the controller.
	 */
	void updateWorkspace(Workspace workspace) {
		mappingsManager.clearAggregated();
		if (ssvmIntegration != null) {
			ssvmIntegration.cleanup();
		}
		if (workspace == null) {
			inheritanceGraph = null;
			symbolSolver = null;
			javaParserHelper = null;
			ssvmIntegration = null;
			treeService = null;
		} else {
			inheritanceGraph = new InheritanceGraph(workspace);
			symbolSolver = WorkspaceSymbolSolver.create(workspace);
			javaParserHelper = JavaParserHelper.create(symbolSolver);
			ssvmIntegration = new SsvmIntegration(workspace);
			treeService = new WorkspaceTreeService(workspace);
		}
	}
}
