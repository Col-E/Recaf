package me.coley.recaf.parse;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistAnnotationDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistClassDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistEnumDeclaration;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistInterfaceDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionAnnotationDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionEnumDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.code.ItemInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.util.AccessFlag;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.slf4j.Logger;

import java.lang.reflect.Method;

/**
 * Utility for resolving context behind {@link Node} values.
 *
 * @author Matt Coley
 */
public class JavaParserResolving {
	private static final Logger logger = Logging.get(JavaParserResolving.class);

	/**
	 * @param node
	 * 		Some AST node.
	 *
	 * @return {@code true} if the node has some means of being resolvable.
	 */
	public static boolean isNodeResolvable(Node node) {
		Class<? extends Node> nodeClass = node.getClass();
		// Check if node is resolvable.
		try {
			if (AccessFlag.isPublic(nodeClass.getMethod("resolve").getModifiers())) {
				return true;
			}
		} catch (Throwable t) {
			// ignored
		}
		// Check if a fallback facade is available
		for (Method method : JavaParserFacade.class.getDeclaredMethods()) {
			if (method.getName().equals("solve")) {
				Class<?> param = method.getParameterTypes()[0];
				if (param.isAssignableFrom(nodeClass)) {
					return true;
				}
			}
		}
		return false;
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
	public static ItemInfo of(WorkspaceTypeSolver typeSolver, Node node) {
		Object value = null;
		Class<? extends Node> nodeClass = node.getClass();
		// Use node's resolving
		try {
			Method resolve = nodeClass.getMethod("resolve");
			resolve.setAccessible(true);
			value = resolve.invoke(node);
		} catch (ReflectiveOperationException ex) {
			// ignore items such as "NoSuchMethodException"
		}
		// If value was found, map to info
		if (value != null) {
			ItemInfo info = objectToInfo(typeSolver, value);
			if (info != null) {
				return info;
			}
		}
		// Use facade resolving
		Method solve = null;
		for (Method method : JavaParserFacade.class.getDeclaredMethods()) {
			if (method.getName().equals("solve")) {
				Class<?> param = method.getParameterTypes()[0];
				if (param.isAssignableFrom(nodeClass)) {
					solve = method;
					break;
				}
			}
		}
		if (solve != null) {
			JavaParserFacade facade = JavaParserFacade.get(typeSolver);
			try {
				value = solve.invoke(facade, node);
			} catch (Exception ex) {
				// Some of the facade implementations just throw exceptions when they don't resolve values.
				// We can ignore them and assume they failed to get anything useful.
			}
		}
		return objectToInfo(typeSolver, value);
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
		if (value instanceof SymbolReference<?>) {
			return symbolReferenceToInfo(typeSolver, (SymbolReference<?>) value);
		} else if (value instanceof ResolvedDeclaration) {
			return resolvedValueToInfo(typeSolver, (ResolvedDeclaration) value);
		} else if (value instanceof ResolvedType) {
			return resolvedTypeToInfo(typeSolver, (ResolvedType) value);
		} else if (value != null) {
			logger.warn("Unhandled type of resolved value: {}", value.getClass());
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
		if (type instanceof JavassistClassDeclaration) {
			String name = JavaParserPrinting.getType((JavassistClassDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof JavassistInterfaceDeclaration) {
			String name = JavaParserPrinting.getType((JavassistInterfaceDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof JavassistEnumDeclaration) {
			String name = JavaParserPrinting.getType((JavassistEnumDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof JavassistAnnotationDeclaration) {
			String name = JavaParserPrinting.getType((JavassistAnnotationDeclaration) type);
			return workspace.getResources().getClass(name);
		} else if (type instanceof ReflectionClassDeclaration) {
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
		// Get declaring class info
		ClassInfo ownerInfo = toClassInfo(typeSolver, field.asType());
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
		for (MethodInfo methodInfo : ownerInfo.getMethods()) {
			if (methodInfo.getName().equals(name) && methodInfo.getDescriptor().equals(desc))
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
