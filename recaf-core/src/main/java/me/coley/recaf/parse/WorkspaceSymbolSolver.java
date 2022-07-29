package me.coley.recaf.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import me.coley.recaf.workspace.Workspace;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@link JavaSymbolSolver} with the ability to hook a few things to provide fallback lookups.
 *
 * @author Matt Coley
 */
public class WorkspaceSymbolSolver extends JavaSymbolSolver {
	private final WorkspaceTypeSolver typeSolver;
	private final JavaParserFacade facade;

	private WorkspaceSymbolSolver(WorkspaceTypeSolver typeSolver) {
		super(typeSolver);
		this.typeSolver = typeSolver;
		facade = JavaParserFacade.get(typeSolver);
	}

	/**
	 * @param workspace
	 * 		Workspace to create solver for.
	 *
	 * @return Solver for workspace.
	 */
	public static WorkspaceSymbolSolver create(Workspace workspace) {
		return new WorkspaceSymbolSolver(new WorkspaceTypeSolver(workspace));
	}

	/**
	 * @return Backing type solver.
	 */
	public WorkspaceTypeSolver getTypeSolver() {
		return typeSolver;
	}

	/**
	 * @return Associated facade instance.
	 */
	public JavaParserFacade getFacade() {
		return facade;
	}

	@Override
	public <T> T resolveDeclaration(Node node, Class<T> resultClass) {
		try {
			return super.resolveDeclaration(node, resultClass);
		} catch (Throwable ex) {
			// JavaParserFacade.solveMethodAsUsage() throws 'RuntimeException' instead of 'UnsolvedSymbolException'
			try {
				return fallback(node, resultClass);
			} catch (RuntimeException ignored) {
				// The fallback will delegate to 'solveMethodAsUsage()' in some cases, so we need to catch again.
			}
		}
		throw new UnsolvedSymbolException("Cannot solve for " + resultClass.getSimpleName() + " on " + node.toString());
	}

	@SuppressWarnings("unchecked")
	private <T> T fallback(Node node, Class<T> resultClass) {
		if (node.getClass() == NameExpr.class && node.getParentNode().isPresent()) {
			Node parent = node.getParentNode().get();
			if (parent instanceof MethodCallExpr) {
				if (resultClass.isAssignableFrom(ResolvedMethodLikeDeclaration.class)) {
					return (T) resolveDeclaration(parent, ResolvedMethodLikeDeclaration.class);
				} else {
					return (T) calculateType((Expression) node).asReferenceType().getTypeDeclaration().get();
				}
			} else if (parent instanceof FieldAccessExpr) {
				if (resultClass.isAssignableFrom(ResolvedFieldDeclaration.class)) {
					return (T) resolveDeclaration(parent, ResolvedFieldDeclaration.class);
				} else {
					return (T) calculateType((Expression) node).asReferenceType().getTypeDeclaration().get();
				}
			}
		} else if (node.getClass() == MethodCallExpr.class) {
			MethodCallExpr expr = (MethodCallExpr) node;
			return (T) facade.solveMethodAsUsage(expr);
		} else if (node.getClass() == MethodReferenceExpr.class) {
			MethodReferenceExpr expr = (MethodReferenceExpr) node;
			ResolvedType declaration = expr.getScope().calculateResolvedType();
			if (declaration != null) {
				List<ResolvedMethodDeclaration> methodsByName = declaration.asReferenceType().getAllMethods().stream()
						.filter(m -> m.getName().equals(expr.getIdentifier()))
						.collect(Collectors.toList());
				if (methodsByName.size() == 1)
					return (T) methodsByName.get(0);
			}
		}
		throw new IllegalStateException();
	}
}
