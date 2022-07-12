package me.coley.recaf.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.TypeExtractor;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.ClassOrInterfaceDeclarationContext;
import com.github.javaparser.symbolsolver.javaparsermodel.contexts.VariableDeclaratorContext;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.model.resolution.Value;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.parse.jpimpl.RecafResolvedFieldDeclaration;
import me.coley.recaf.parse.jpimpl.RecafResolvedTypeDeclaration;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.workspace.Workspace;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkspaceSymbolSolver extends JavaSymbolSolver {
	private final WorkspaceTypeSolver typeSolver;
	private final JavaParserFacade facade;
	private final SolverImpl solverImpl;
	private final Foo extractorImpl;

	private WorkspaceSymbolSolver(WorkspaceTypeSolver typeSolver) {
		super(typeSolver);
		this.typeSolver = typeSolver;
		facade = JavaParserFacade.get(typeSolver);
		solverImpl = new SolverImpl(typeSolver);
		extractorImpl = new Foo(typeSolver, facade);
		try {
			// Swap the symbol solver impl in the facade
			Field facadeSymbolSolver = ReflectUtil.getDeclaredField(JavaParserFacade.class, "symbolSolver");
			facadeSymbolSolver.setAccessible(true);
			ReflectUtil.quietSet(facade, facadeSymbolSolver, solverImpl);
			// Swap the type extractor
			Field facadeTypeExtractor = ReflectUtil.getDeclaredField(JavaParserFacade.class, "typeExtractor");
			facadeTypeExtractor.setAccessible(true);
			ReflectUtil.quietSet(facade, facadeTypeExtractor, extractorImpl);
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("Cannot access 'SymbolSolver#typeSolver'", ex);
		}
	}

	class Foo extends TypeExtractor {
		public Foo(TypeSolver typeSolver, JavaParserFacade facade) {
			super(typeSolver, facade);
		}

		@Override
		public ResolvedType visit(ClassExpr node, Boolean solveLambdas) {
			ResolvedType resolvedType = node.getType().resolve();
			if (resolvedType.isReferenceType()) {
				SymbolReference<ResolvedReferenceTypeDeclaration> reference =
						typeSolver.tryToSolveType(resolvedType.asReferenceType().getQualifiedName());
				if (reference.isSolved()) {
					CommonClassInfo info =
							((RecafResolvedTypeDeclaration) reference.getCorrespondingDeclaration()).getClassInfo();
					return RecafResolvedTypeDeclaration.from(typeSolver, info).getType();
				}
			}
			return super.visit(node, solveLambdas);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolveDeclaration(Node node, Class<T> resultClass) {
		try {
			return super.resolveDeclaration(node, resultClass);
		} catch (UnsolvedSymbolException ex) {
			if (node.getClass() == NameExpr.class && node.getParentNode().isPresent()) {
				Node parent = node.getParentNode().get();
				if (parent instanceof MethodCallExpr) {
					return (T) resolveDeclaration(parent, ResolvedMethodLikeDeclaration.class).declaringType();
				} else if (parent instanceof FieldAccessExpr) {
					return (T) resolveDeclaration(parent, ResolvedFieldDeclaration.class).declaringType();
				}
			} else if (node.getClass() == MethodCallExpr.class) {
				MethodCallExpr expr = (MethodCallExpr) node;
				return (T) facade.solveMethodAsUsage(expr);
			}
			throw ex;
		} catch (Throwable t) {
			// JavaParserFacade.solveMethodAsUsage() throws 'RuntimeException' instead of 'UnsolvedSymbolException'
			if (node.getClass() == MethodReferenceExpr.class) {
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
			throw t;
		}
	}

	private static WorkspaceTypeSolver createTypeSolver(Workspace workspace) {
		return new WorkspaceTypeSolver(workspace);
	}

	public static WorkspaceSymbolSolver create(Workspace workspace) {
		return new WorkspaceSymbolSolver(createTypeSolver(workspace));
	}

	public WorkspaceTypeSolver getTypeSolver() {
		return typeSolver;
	}

	public JavaParserFacade getFacade() {
		return facade;
	}

	class SolverImpl extends SymbolSolver {
		public SolverImpl(TypeSolver typeSolver) {
			super(typeSolver);
		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, Node node) {
			// Go up a parent until the type is resolvable.
			while (node != null && !(node instanceof Resolvable<?>))
				node = node.getParentNode().orElse(null);

			if (node != null) {
				return solveSymbol(name, createContext(node));
			} else {
				// Cannot resolve if there is no node
				return SymbolReference.unsolved(ResolvedValueDeclaration.class);
			}
		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbol(String name, Context context) {
			// JavaParser is VERY slow to resolve 'variable' names in some cases due to Javassist usage.
			// This handles that edge case by instead looking
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
			}
			return super.solveSymbol(name, context);
		}


		@Override
		public Optional<Value> solveSymbolAsValue(String name, Context context) {
			return super.solveSymbolAsValue(name, context);
		}

		@Override
		public Optional<Value> solveSymbolAsValue(String name, Node node) {
			return super.solveSymbolAsValue(name, node);
		}

		@Override
		public SymbolReference<? extends ResolvedTypeDeclaration> solveType(String name, Context context) {
			return super.solveType(name, context);
		}

		@Override
		public SymbolReference<? extends ResolvedTypeDeclaration> solveType(String name, Node node) {
			return super.solveType(name, node);
		}

		@Override
		public MethodUsage solveMethod(String methodName, List<ResolvedType> argumentsTypes, Context context) {
			return super.solveMethod(methodName, argumentsTypes, context);
		}

		@Override
		public MethodUsage solveMethod(String methodName, List<ResolvedType> argumentsTypes, Node node) {
			return super.solveMethod(methodName, argumentsTypes, node);
		}

		@Override
		public ResolvedTypeDeclaration solveType(Type type) {
			return super.solveType(type);
		}

		@Override
		public ResolvedType solveTypeUsage(String name, Context context) {
			return super.solveTypeUsage(name, context);
		}

		@Override
		public SymbolReference<? extends ResolvedValueDeclaration> solveSymbolInType(ResolvedTypeDeclaration typeDeclaration, String name) {
			if (typeDeclaration instanceof RecafResolvedTypeDeclaration) {
				return ((RecafResolvedTypeDeclaration) typeDeclaration).solveSymbol(name);
			}
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
