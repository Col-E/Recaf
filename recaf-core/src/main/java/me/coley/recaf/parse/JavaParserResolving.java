package me.coley.recaf.parse;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionAnnotationDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionEnumDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import me.coley.recaf.code.*;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility for resolving context behind {@link Node} values.
 *
 * @author Matt Coley
 */
public class JavaParserResolving {
	private static final Logger logger = Logging.get(JavaParserResolving.class);
	private static final Map<Class<?>, MethodHandle> resolveLookupCache = new IdentityHashMap<>();
	private static final Map<Class<?>, MethodHandle> solveLookupCache = new IdentityHashMap<>();
	private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

	/**
	 * @param node
	 * 		Some AST node.
	 *
	 * @return {@code true} if the node has some means of being resolvable.
	 */
	public static boolean isNodeResolvable(Node node) {
		if (node instanceof Resolvable)
			return true;
		// Check if a fallback facade is available
		Class<? extends Node> nodeClass = node.getClass();
		for (Method method : JavaParserFacade.class.getDeclaredMethods()) {
			if (method.getName().equals("solve")) {
				Class<?> param = method.getParameterTypes()[0];
				if (param.isAssignableFrom(nodeClass)) {
					return true;
				}
			}
		}
		// Edge case for <clinit>
		if (node instanceof InitializerDeclaration) {
			InitializerDeclaration init = (InitializerDeclaration) node;
			return init.isStatic();
		}
		return false;
	}

	/**
	 * @param symbolSolver
	 * 		Symbol solver tied in with a {@link Workspace}.
	 * @param node
	 * 		Some AST node.
	 *
	 * @return Either {@code null} if the passed {@link Node} is not resolvable, or one of the following:<ul>
	 * <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 * <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 * <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public static ItemInfo of(WorkspaceSymbolSolver symbolSolver, Node node) {
		Object value = null;
		Class<? extends Node> nodeClass = node.getClass();
		// Use node's resolving
		try {
			MethodHandle handle;
			if (resolveLookupCache.containsKey(nodeClass)) {
				handle = resolveLookupCache.get(nodeClass);
			} else {
				Method resolve = nodeClass.getMethod("resolve");
				resolve.setAccessible(true);
				handle = lookup.unreflect(resolve);
				resolveLookupCache.put(nodeClass, handle);
			}
			if (handle != null)
				value = handle.invoke(node);
		} catch (NoSuchMethodException ex) {
			// The node doesn't have a 'resolve' implementation.
			resolveLookupCache.put(nodeClass, null);
		} catch (UnsolvedSymbolException ex) {
			//logger.error("Resolve failure", ex);
		} catch (Throwable ex) {
			logger.error("Handle invoke exception on 'resolve'", ex);
		}
		// If value was found, map to info
		WorkspaceTypeSolver typeSolver = symbolSolver.getTypeSolver();
		if (value != null) {
			ItemInfo info = objectToInfo(typeSolver, value);
			if (info != null) {
				return info;
			}
		}
		// Use facade resolving
		MethodHandle solve = solveLookupCache.get(nodeClass);
		if (solveLookupCache.containsKey(nodeClass)) {
			solve = solveLookupCache.get(nodeClass);
		} else {
			solveLookupCache.put(nodeClass, null);
			for (Method method : JavaParserFacade.class.getDeclaredMethods()) {
				if (method.getName().equals("solve")) {
					Class<?>[] parameterTypes = method.getParameterTypes();
					if (parameterTypes.length != 1)
						continue;
					Class<?> param = parameterTypes[0];
					if (param == (nodeClass)) {
						try {
							solve = lookup.unreflect(method);
							solveLookupCache.put(nodeClass, solve);
							break;
						} catch (IllegalAccessException e) {
							// Thrown when access check fails, but all the 'solve' methods are public,
							// so this should never occur.
						}
					}
				}
			}
		}
		if (solve != null) {
			try {
				value = solve.invoke(symbolSolver.getFacade(), node);
			} catch (Throwable ignored) {
				// Some facade implementations just throw exceptions when they don't resolve values.
				// We can ignore them and assume they failed to get anything useful.
			}
		}
		if (node instanceof InitializerDeclaration) {
			InitializerDeclaration init = (InitializerDeclaration) node;
			if (init.isStatic()) {
				ItemInfo unitInfo = of(symbolSolver, node.getParentNode().get());
				if (unitInfo instanceof CommonClassInfo) {
					Optional<MethodInfo> initializerInfo = ((CommonClassInfo) unitInfo).getMethods()
							.stream()
							.filter(m -> m.getName().equals("<clinit>"))
							.findFirst();
					if (initializerInfo.isPresent()) {
						return initializerInfo.get();
					}
				}
			}
		}
		return objectToInfo(typeSolver, value);
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param node
	 * 		Some AST node.
	 *
	 * @return Either {@code null} if the passed {@link Node} is not resolvable, or one of the following:<ul>
	 * <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 * <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 * <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public static ItemInfo ofEdgeCases(WorkspaceTypeSolver typeSolver, Node node) {
		if (node == null || !node.hasParentNode())
			return null;
		Node parent = node.getParentNode().get();
		if (parent instanceof ImportDeclaration) {
			ImportDeclaration imported = (ImportDeclaration) parent;
			// Cannot determine single type from asterisk/star imports.
			//  - Static means we're importing members of a class, so in that case we can still resolve the type.
			if (imported.isAsterisk() && !imported.isStatic()) {
				return null;
			}
			// Get type/member imported.
			if (imported.isStatic()) {
				// If the import is static we may also need to check for members in addition to just class names.
				String importName = imported.getNameAsString();
				int dotIndex = importName.lastIndexOf('.');
				String className = importName.substring(0, dotIndex);
				String memberName = importName.substring(dotIndex + 1);
				// Try to resolve class name, then member if we have data about the defining class.
				SymbolReference<ResolvedReferenceTypeDeclaration> result = typeSolver.tryToSolveType(className);
				if (result.isSolved()) {
					CommonClassInfo info = (CommonClassInfo) objectToInfo(typeSolver, result);
					// Asterisk/start import means we're importing all members of a class
					if (imported.isAsterisk())
						return (ItemInfo) info;
					// Try to get matching field/method
					if (info != null) {
						for (FieldInfo fieldInfo : info.getFields())
							if (fieldInfo.getName().equals(memberName))
								return fieldInfo;
						for (MethodInfo methodInfo : info.getMethods())
							if (methodInfo.getName().equals(memberName))
								return methodInfo;
					}
				}
			} else {
				String importName = imported.getNameAsString();
				SymbolReference<ResolvedReferenceTypeDeclaration> result = typeSolver.tryToSolveType(importName);
				if (result.isSolved()) {
					return objectToInfo(typeSolver, result);
				}
			}
		} else if (parent instanceof PackageDeclaration) {
			Optional<CompilationUnit> opt = parent.findCompilationUnit();
			if (opt.isPresent()) {
				CompilationUnit unit = opt.get();
				// Check if there are no defined types. This implies we are inside a "package-info" class.
				if (unit.getTypes().isEmpty()) {
					String packageName = ((PackageDeclaration) parent).getNameAsString();
					String className = packageName + ".package-info";
					SymbolReference<ResolvedReferenceTypeDeclaration> result = typeSolver.tryToSolveType(className);
					if (result.isSolved()) {
						return objectToInfo(typeSolver, result);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param value
	 * 		Some unknown value, likely of {@link SymbolReference} or {@link ResolvedDeclaration}.
	 *
	 * @return Either {@code null} if the passed {@link Object} is not resolvable, or one of the following:<ul>
	 * <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 * <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 * <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public static ItemInfo objectToInfo(WorkspaceTypeSolver typeSolver, Object value) {
		try {
			if (value instanceof SymbolReference<?>) {
				return symbolReferenceToInfo(typeSolver, (SymbolReference<?>) value);
			} else if (value instanceof ResolvedDeclaration) {
				return resolvedValueToInfo(typeSolver, (ResolvedDeclaration) value);
			} else if (value instanceof ResolvedType) {
				return resolvedTypeToInfo(typeSolver, (ResolvedType) value);
			} else if (value != null) {
				logger.warn("Unhandled type of resolved value: {}", value.getClass());
			}
		} catch (UnsolvedSymbolException ex) {
			logger.warn("Unsolved symbol prevented resolve: " + ex.getName());
		} catch (Throwable t) {
			logger.warn("Unknown exception prevented resolve: " + t.getMessage(), t);
		}
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param ref
	 * 		Symbol reference to parse.
	 *
	 * @return Either {@code null} if the passed {@link SymbolReference} is not resolved, or one of the following:<ul>
	 * <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 * <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 * <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public static ItemInfo symbolReferenceToInfo(WorkspaceTypeSolver typeSolver, SymbolReference<?> ref) {
		if (ref.isSolved()) {
			return resolvedValueToInfo(typeSolver, ref.getCorrespondingDeclaration());
		}
		// The symbol was not resolved
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param resolved
	 * 		Resolved JavaParser declaration.
	 *
	 * @return Either {@code null} if the passed {@link ResolvedDeclaration} is not supported,
	 * or one of the following:<ul>
	 * <li>{@link me.coley.recaf.code.ClassInfo}</li>
	 * <li>{@link me.coley.recaf.code.FieldInfo}</li>
	 * <li>{@link me.coley.recaf.code.MethodInfo}</li>
	 * </ul>
	 */
	public static ItemInfo resolvedValueToInfo(WorkspaceTypeSolver typeSolver, ResolvedDeclaration resolved) {
		// AST nodes have "isField/isMethod/etc" but those sometimes don't return true when you'd expect they should.
		// So we just instanceof check instead all the way down.
		try {
			if (resolved instanceof ResolvedFieldDeclaration) {
				ResolvedFieldDeclaration field = (ResolvedFieldDeclaration) resolved;
				return toFieldInfo(typeSolver, field);
			} else if (resolved instanceof ResolvedMethodDeclaration) {
				ResolvedMethodDeclaration method = (ResolvedMethodDeclaration) resolved;
				return toMethodInfo(typeSolver, method);
			} else if (resolved instanceof ResolvedTypeDeclaration) {
				ResolvedTypeDeclaration type = (ResolvedTypeDeclaration) resolved;
				return toClassInfo(typeSolver, type);
			} else if (resolved instanceof ResolvedEnumConstantDeclaration) {
				ResolvedEnumConstantDeclaration enumField = (ResolvedEnumConstantDeclaration) resolved;
				return toFieldInfo(typeSolver, enumField);
			} else if (resolved instanceof ResolvedConstructorDeclaration) {
				ResolvedConstructorDeclaration ctor = (ResolvedConstructorDeclaration) resolved;
				return toMethodInfo(typeSolver, ctor);
			}
		} catch (UnsolvedSymbolException ex) {
			logger.warn("Cannot map resolved item '{}' to workspace class/member info due to unsolved symbol: {}",
					resolved.getName(), ex.getName());
		}
		// Cannot resolve unknown value
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param value
	 * 		Resolved type.
	 *
	 * @return A {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static ClassInfo resolvedTypeToInfo(WorkspaceTypeSolver typeSolver, ResolvedType value) {
		Workspace workspace = typeSolver.getWorkspace();
		String name = JavaParserPrinting.getType(value);
		return workspace.getResources().getClass(name);
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param type
	 * 		Resolved declaration of a type.
	 *
	 * @return A {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static ClassInfo toClassInfo(WorkspaceTypeSolver typeSolver, ResolvedTypeDeclaration type) {
		Workspace workspace = typeSolver.getWorkspace();
		// I hate JavaParser for not making a common way to get the internal name instead of this mess.
		if (type instanceof ReflectionClassDeclaration) {
			String name = JavaParserPrinting.getType((ReflectionClassDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof ReflectionInterfaceDeclaration) {
			String name = JavaParserPrinting.getType((ReflectionInterfaceDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof ReflectionEnumDeclaration) {
			String name = JavaParserPrinting.getType((ReflectionEnumDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof ReflectionAnnotationDeclaration) {
			String name = JavaParserPrinting.getType((ReflectionAnnotationDeclaration) type);
			return workspace.getResources().getClass(name);
		} else {
			// Some unknown implementation... Ughh....
			return qualifiedToClassInfo(workspace, type.getQualifiedName());
		}
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param field
	 * 		Resolved declaration of a field.
	 *
	 * @return A {@link FieldInfo} of a {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static FieldInfo toFieldInfo(WorkspaceTypeSolver typeSolver, ResolvedFieldDeclaration field) {
		// Get declaring class info
		ClassInfo ownerInfo = toClassInfo(typeSolver, field.declaringType());
		if (ownerInfo == null)
			return null;
		// Get field info
		String name = field.getName();
		String desc = JavaParserPrinting.getFieldDesc(field);
		// Find matching field entry
		for (FieldInfo fieldInfo : ownerInfo.getFields()) {
			if (fieldInfo.getName().equals(name) && fieldInfo.getDescriptor().equals(desc))
				return fieldInfo;
		}
		// No match
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param field
	 * 		Resolved declaration of a field.
	 *
	 * @return A {@link FieldInfo} of a {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static FieldInfo toFieldInfo(WorkspaceTypeSolver typeSolver, ResolvedEnumConstantDeclaration field) {
		// Get declaring class info, which doubles as the descriptor type
		//  - This returns as an internal type instead of a desc... IDK why
		String type = JavaParserPrinting.getFieldDesc(field);
		ClassInfo ownerInfo = qualifiedToClassInfo(typeSolver.getWorkspace(), type);
		if (ownerInfo == null)
			return null;
		// Get field name
		String name = field.getName();
		String desc = "L" + type + ";";
		// Find matching field entry
		for (FieldInfo fieldInfo : ownerInfo.getFields()) {
			if (fieldInfo.getName().equals(name) && fieldInfo.getDescriptor().equals(desc))
				return fieldInfo;
		}
		// No match
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param method
	 * 		Resolved declaration of a method.
	 *
	 * @return A {@link MethodInfo} of a {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static MethodInfo toMethodInfo(WorkspaceTypeSolver typeSolver, ResolvedMethodDeclaration method) {
		// Get declaring class info
		ClassInfo ownerInfo = toClassInfo(typeSolver, method.declaringType());
		if (ownerInfo == null)
			return null;
		// Get method info
		String name = method.getName();
		String desc = JavaParserPrinting.getMethodDesc(method);
		// Find matching method entry
		for (MethodInfo methodInfo : ownerInfo.getMethods()) {
			if (methodInfo.getName().equals(name) && methodInfo.getDescriptor().equals(desc))
				return methodInfo;
		}
		// No match
		return null;
	}

	/**
	 * @param typeSolver
	 * 		Type solver tied in with a {@link Workspace}.
	 * @param constructor
	 * 		Resolved declaration of a constructor.
	 *
	 * @return A {@link MethodInfo} of a {@link ClassInfo} in the {@link Workspace} associated with the type solver.
	 */
	private static MethodInfo toMethodInfo(WorkspaceTypeSolver typeSolver, ResolvedConstructorDeclaration constructor) {
		// Get declaring class info
		ClassInfo ownerInfo = toClassInfo(typeSolver, constructor.declaringType());
		if (ownerInfo == null)
			return null;
		// Get method info
		String name = "<init>";
		String desc = JavaParserPrinting.getConstructorDesc(constructor);
		// Find matching method entry
		List<MethodInfo> constructors = new ArrayList<>();
		for (MethodInfo methodInfo : ownerInfo.getMethods()) {
			if (methodInfo.getName().equals(name)) {
				constructors.add(methodInfo);
				if (methodInfo.getDescriptor().equals(desc))
					return methodInfo;
			}
		}
		// Enum constructors at compile-time have "String, int" added to call the `Enum.<init>(String, int)' super call.
		// So if the owner is an enum, we insert these parameters.
		if (AccessFlag.isEnum(ownerInfo.getAccess()))
			desc = "(Ljava/lang/String;I" + desc.substring(1);
		for (MethodInfo methodInfo : constructors) {
			if (methodInfo.getDescriptor().equals(desc))
				return methodInfo;
		}
		// No match
		return null;
	}

	/**
	 * @param workspace
	 * 		Workspace containing classes to pull from.
	 * @param qualified
	 * 		Qualified name <i>(Which may or may not be confusing due to inner class patterns)</i>.
	 *
	 * @return A {@link ClassInfo} of the type, or {@code null} when no class in the workspace matches.
	 */
	private static ClassInfo qualifiedToClassInfo(Workspace workspace, String qualified) {
		// Because the source-level design of JavaParser makes a package/inner-class separator both '.'
		// so if we want to get the type of an inner class we have to replace package separators until a match.
		String internal = qualified.replace('.', '/');
		ClassInfo result;
		do {
			result = workspace.getResources().getClass(internal);
			internal = StringUtil.replaceLast(internal, "/", "$");
		} while (result == null && internal.indexOf('/') > 0);
		return result;
	}
}
