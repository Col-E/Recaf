package me.coley.recaf.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.TypeExtractor;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.resolution.Value;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.parse.jpimpl.RecafResolvedTypeDeclaration;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.workspace.Workspace;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkspaceSymbolSolver extends JavaSymbolSolver {
	private static final boolean DEBUG = true;
	private final WorkspaceTypeSolver typeSolver;
	private final JavaParserFacade facade;
	private final SymbolSolverImpl symbolSolverImpl;
	private final TypeExtractorImpl extractorImpl;

	private WorkspaceSymbolSolver(WorkspaceTypeSolver typeSolver) {
		super(typeSolver);
		this.typeSolver = typeSolver;
		facade = JavaParserFacade.get(typeSolver);
		try {
			if (DEBUG) {
				symbolSolverImpl = new SymbolSolverImpl(typeSolver);
				extractorImpl = new TypeExtractorImpl(typeSolver, facade);
				// Swap the symbol solver impl in the facade
				Field facadeSymbolSolver = ReflectUtil.getDeclaredField(JavaParserFacade.class, "symbolSolver");
				facadeSymbolSolver.setAccessible(true);
				ReflectUtil.quietSet(facade, facadeSymbolSolver, symbolSolverImpl);
				// Swap the type extractor
				Field facadeTypeExtractor = ReflectUtil.getDeclaredField(JavaParserFacade.class, "typeExtractor");
				facadeTypeExtractor.setAccessible(true);
				ReflectUtil.quietSet(facade, facadeTypeExtractor, extractorImpl);
			} else {
				symbolSolverImpl = null;
				extractorImpl = null;
			}
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Cannot access 'SymbolSolver#typeSolver'", ex);
		}
	}

	public static WorkspaceSymbolSolver create(Workspace workspace) {
		return new WorkspaceSymbolSolver(new WorkspaceTypeSolver(workspace));
	}

	public WorkspaceTypeSolver getTypeSolver() {
		return typeSolver;
	}

	public JavaParserFacade getFacade() {
		return facade;
	}

	public SymbolSolverImpl getSymbolSolverImpl() {
		return symbolSolverImpl;
	}

	public TypeExtractorImpl getExtractorImpl() {
		return extractorImpl;
	}

	@Override
	public <T> T resolveDeclaration(Node node, Class<T> resultClass) {
		try {
			return super.resolveDeclaration(node, resultClass);
		} catch (Throwable ex) {
			// JavaParserFacade.solveMethodAsUsage() throws 'RuntimeException' instead of 'UnsolvedSymbolException'
			return fallback(node, resultClass);
		}
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

	private class TypeExtractorImpl extends TypeExtractor {
		public TypeExtractorImpl(TypeSolver typeSolver, JavaParserFacade facade) {
			super(typeSolver, facade);
		}

		@Override
		public ResolvedType visit(ClassExpr node, Boolean solveLambdas) {
			ClassInfo classInfo = typeSolver.getWorkspace().getResources().getClass("java/lang/Class");
			/*
			ResolvedType resolvedType = node.getType().resolve();
			if (resolvedType.isReferenceType()) {
				SymbolReference<ResolvedReferenceTypeDeclaration> reference =
						typeSolver.tryToSolveType(resolvedType.asReferenceType().getQualifiedName());
				if (reference.isSolved()) {
					// TODO: Add generic type to 'Class<T>'
					CommonClassInfo info =
							((RecafResolvedTypeDeclaration) reference.getCorrespondingDeclaration()).getClassInfo();
				}
			}*/
			return RecafResolvedTypeDeclaration.from(typeSolver, classInfo).getType();
		}

		@Override
		public ResolvedType visit(MethodCallExpr node, Boolean solveLambdas) {
			return super.visit(node, solveLambdas);
		}
	}

	private class SymbolSolverImpl extends SymbolSolver {
		public SymbolSolverImpl(TypeSolver typeSolver) {
			super(typeSolver);
		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, Node node) {
			// Go up a parent until the type is resolvable.
			Node original = node;
			/*
			while (node != null) {
				try {
					if (node instanceof Resolvable<?>) {
						SymbolReference<? extends ResolvedValueDeclaration> symbolReference = solveSymbol(name, createContext(node));
						if (symbolReference.isSolved())
							return symbolReference;
					}
				} catch (Exception ex) {
					// ignored
				}
				node = node.getParentNode().orElse(null);
			}*/
			return super.solveSymbol(name, original);

		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, Context context) {
			// JavaParser is VERY slow to resolve 'variable' names in some cases due to Javassist usage.
			// This handles that edge case by instead looking
			/*
			if (context instanceof VariableDeclaratorContext) {
				Context parent = context.getParent().orElse(null);
				if (parent == null) {
					return SymbolReference.unsolved(ResolvedValueDeclaration.class);
				} else if (parent instanceof ClassOrInterfaceDeclarationContext) {
					ClassOrInterfaceDeclarationContext classContext = (ClassOrInterfaceDeclarationContext) parent;
					ClassOrInterfaceDeclaration wrappedNode = classContext.getWrappedNode();
					ItemInfo info = JavaParserResolving.of(WorkspaceSymbolSolver.this, wrappedNode);
					if (info instanceof CommonClassInfo) {
						ClassInfo classInfo = (ClassInfo) info;
						FieldInfo fieldInfo = classInfo.findField(name, null);
						if (fieldInfo != null) {
							return SymbolReference.solved(new RecafResolvedFieldDeclaration(typeSolver, classInfo, fieldInfo));
						}
					}
				}
			}*/
			return super.solveSymbol(name, context);
		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbolInType(ResolvedTypeDeclaration typeDeclaration, String name) {
			if (typeDeclaration instanceof RecafResolvedTypeDeclaration) {
				SymbolReference<? extends ResolvedValueDeclaration> reference =
						((RecafResolvedTypeDeclaration) typeDeclaration).solveSymbol(name, typeSolver);
				if (reference.isSolved())
					return reference;
			}
			// TODO: When https://github.com/javaparser/javaparser/pull/3634 gets merged we can remove this
			return super.solveSymbolInType(typeDeclaration, name);
		}

		private Context createContext(Node node) {
			if (node instanceof ClassOrInterfaceDeclaration) {
				/*return new ClassOrInterfaceDeclarationContext((ClassOrInterfaceDeclaration) node, typeSolver) {
					@Override
					public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name) {
						if (typeSolver == null) throw new IllegalArgumentException();
						ResolvedReferenceTypeDeclaration declaration = facade.getTypeDeclaration(wrappedNode);

						if (declaration.hasVisibleField(name)) {
							return SymbolReference.solved(declaration.getVisibleField(name));
						}

						// then to parent
						return solveSymbolInParentContext(name);
					}
				};*/
			}
			return JavaParserFactory.getContext(node, typeSolver);
		}
	}
}
