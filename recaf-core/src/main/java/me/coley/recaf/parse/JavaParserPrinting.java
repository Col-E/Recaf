package me.coley.recaf.parse;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFactory;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserEnumDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserInterfaceDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserParameterDeclaration;
import com.github.javaparser.symbolsolver.logic.AbstractClassDeclaration;
import com.github.javaparser.symbolsolver.logic.AbstractTypeDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.*;
import me.coley.recaf.parse.jpimpl.RecafResolvedTypeDeclaration;
import me.coley.recaf.parse.jpimpl.RecafResolvedFieldDeclaration;
import me.coley.recaf.parse.jpimpl.RecafResolvedMethodLikeDeclaration;
import me.coley.recaf.util.ReflectUtil;
import me.coley.recaf.util.StringUtil;
import me.coley.recaf.util.logging.Logging;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

import static me.coley.recaf.util.ReflectUtil.getDeclaredField;

/**
 * Utility for printing internal types/descriptors of items since these libraries tend to hide internal details to be
 * more source-level friendly and presentable.
 *
 * @author Matt Coley
 */
public class JavaParserPrinting {
	private static final Logger logger = Logging.get(JavaParserPrinting.class);
	// JavaParser accessors
	private static Field reflectionClass;
	private static Field reflectionClassInterface;
	private static Field reflectionClassAnnotation;
	private static Field reflectionClassEnum;
	private static Field reflectionField;
	private static Field reflectionFieldEnum;
	private static Field reflectionMethod;
	private static Field reflectionMethodAnno;
	private static Field reflectionMethodCtor;

	static {
		try {
			reflectionClass = getDeclaredField(ReflectionClassDeclaration.class, "clazz");
			reflectionClassInterface = getDeclaredField(ReflectionInterfaceDeclaration.class, "clazz");
			reflectionClassAnnotation = getDeclaredField(ReflectionAnnotationDeclaration.class, "clazz");
			reflectionClassEnum = getDeclaredField(ReflectionEnumDeclaration.class, "clazz");
			reflectionField = getDeclaredField(ReflectionFieldDeclaration.class, "field");
			reflectionFieldEnum = getDeclaredField(ReflectionEnumConstantDeclaration.class, "enumConstant");
			reflectionMethod = getDeclaredField(ReflectionMethodDeclaration.class, "method");
			reflectionMethodAnno = getDeclaredField(ReflectionAnnotationMemberDeclaration.class, "annotationMember");
			reflectionMethodCtor = getDeclaredField(ReflectionConstructorDeclaration.class, "constructor");
		} catch (ReflectiveOperationException ex) {
			// Should not occur unless API internals change in JavaParser
			logger.error("Failed to get internal name/descriptor accessors! Internal JavaParser API changed?", ex);
		}
	}

	/**
	 * @param param
	 * 		Parameter node declaration.
	 *
	 * @return Type descriptor of parameter.
	 */
	private static String getTypeDesc(ResolvedParameterDeclaration param) {
		try {
			ResolvedType paramType = param.getType();
			return getTypeDesc(paramType);
		} catch (Exception ex) {
			// JavaParser will throw an exception if the type parameters of the parameter do not match the
			// ones declared on the resolved class.
			// This is an ugly hack to get around that without requiring much change to this class.
			if (param instanceof JavaParserParameterDeclaration) {
				JavaParserParameterDeclaration jpParam = (JavaParserParameterDeclaration) param;
				TypeSolver solver = getTypeSolver(jpParam);
				Context context = JavaParserFactory.getContext(jpParam.getWrappedNode(), solver);
				String name = jpParam.getWrappedNode().getType().asClassOrInterfaceType().getNameWithScope();
				SymbolReference<ResolvedTypeDeclaration> ref = context.solveType(name,null);
				if (ref.isSolved()) {
					return "L" + getType(ref.getCorrespondingDeclaration()) + ";";
				}
			}
			return null;
		}
	}

	/**
	 * @param type
	 * 		Generic type.
	 *
	 * @return Type descriptor.
	 */
	public static String getTypeDesc(ResolvedType type) {
		String internal = getType(type);
		if (type.isVoid() || type.isPrimitive() || type.isArray()) {
			return internal;
		}
		return "L" + internal + ";";
	}

	/**
	 * @param type
	 * 		Generic type.
	 *
	 * @return Internal type.
	 */
	public static String getType(ResolvedType type) {
		if (type instanceof ResolvedReferenceType) {
			Optional<ResolvedReferenceTypeDeclaration> dec = ((ResolvedReferenceType) type).getTypeDeclaration();
			if (dec.isPresent()) {
				return getType(dec.get());
			}
		} else if (type.isVoid()) {
			return "V";
		} else if (type.isPrimitive()) {
			String name = type.describe();
			switch (name) {
				case "byte":
					return "B";
				case "short":
					return "S";
				case "char":
					return "C";
				case "int":
					return "I";
				case "long":
					return "J";
				case "boolean":
					return "Z";
				case "float":
					return "F";
				case "double":
					return "D";
				default:
					throw new IllegalStateException("Unknown primitive: " + name);
			}
		} else if (type.isArray()) {
			ResolvedArrayType resolvedArrayType = type.asArrayType();
			ResolvedType component = resolvedArrayType.getComponentType();
			String arrLevel = StringUtil.repeat("[", resolvedArrayType.arrayLevel());
			return arrLevel + getTypeDesc(component);
		} else if (type.isTypeVariable()) {
			ResolvedTypeVariable resolvedTypeVariable = type.asTypeVariable();
			String description = resolvedTypeVariable.describe();
			// So usually description is just "T" or some basic generic definition.
			// If the type is not bound it'll be Object. 
			if (description.contains(" ")) {
				throw new IllegalStateException("Unsupported type variable");
			} else return "java/lang/Object";
		}
		throw new IllegalStateException();
	}

	/**
	 * @param type
	 * 		Generic type.
	 *
	 * @return Internal type.
	 */
	public static String getType(ResolvedDeclaration type) {
		if (type instanceof RecafResolvedTypeDeclaration) {
			return ((RecafResolvedTypeDeclaration) type).getClassInfo().getName();
		} else if (type instanceof ReflectionClassDeclaration) {
			return getType((ReflectionClassDeclaration) type);
		} else if (type instanceof ReflectionInterfaceDeclaration) {
			return getType((ReflectionInterfaceDeclaration) type);
		} else if (type instanceof ReflectionAnnotationDeclaration) {
			return getType((ReflectionAnnotationDeclaration) type);
		} else if (type instanceof ReflectionEnumDeclaration) {
			return getType((ReflectionEnumDeclaration) type);
		} else if (type instanceof JavaParserClassDeclaration) {
			return getType((JavaParserClassDeclaration) type);
		} else if (type instanceof JavaParserInterfaceDeclaration) {
			return getType((JavaParserInterfaceDeclaration) type);
		} else if (type instanceof JavaParserEnumDeclaration) {
			return getType((JavaParserEnumDeclaration) type);
		} else {
			throw new UnsupportedOperationException("Unsupported declaration type: " +
					type.getClass() + " - \"" + type.getName() + "\"");
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionClassDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClass.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionInterfaceDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassInterface.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionAnnotationDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassAnnotation.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	public static String getType(ReflectionEnumDeclaration clazz) {
		try {
			return Type.getInternalName((Class<?>) reflectionClassEnum.get(clazz));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get internal type", ex);
		}
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	private static String getType(JavaParserClassDeclaration clazz) {
		return clazz.getPackageName().replace('.', '/') + "/" + getInternalBaseName(clazz, clazz.getWrappedNode());
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	private static String getType(JavaParserInterfaceDeclaration clazz) {
		return clazz.getPackageName().replace('.', '/') + "/" + getInternalBaseName(clazz, clazz.getWrappedNode());
	}

	/**
	 * @param clazz
	 * 		Type declaration.
	 *
	 * @return Internal type.
	 */
	private static String getType(JavaParserEnumDeclaration clazz) {
		return clazz.getPackageName().replace('.', '/') + "/" + getInternalBaseName(clazz, clazz.getWrappedNode());
	}

	/**
	 * @param type
	 * 		Generic field declaration.
	 *
	 * @return Internal type.
	 */
	public static String getFieldDesc(ResolvedFieldDeclaration type) {
		if (type instanceof RecafResolvedFieldDeclaration) {
			return ((RecafResolvedFieldDeclaration) type).getFieldInfo().getDescriptor();
		} else if (type instanceof ReflectionFieldDeclaration) {
			return getFieldDesc((ReflectionFieldDeclaration) type);
		} else {
			return getTypeDesc(type.getType());
		}
	}

	/**
	 * @param type
	 * 		Generic enum constant field declaration.
	 *
	 * @return Internal type.
	 */
	public static String getFieldDesc(ResolvedEnumConstantDeclaration type) {
		if (type instanceof ReflectionEnumConstantDeclaration) {
			return getFieldDesc((ReflectionEnumConstantDeclaration) type);
		} else {
			return getType(type.getType());
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(ReflectionFieldDeclaration field) {
		try {
			return Type.getType(((Field) reflectionField.get(field)).getType()).getDescriptor();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param field
	 * 		Field declaration.
	 *
	 * @return Field descriptor.
	 */
	public static String getFieldDesc(ReflectionEnumConstantDeclaration field) {
		try {
			return Type.getType(((Field) reflectionFieldEnum.get(field)).getType()).getDescriptor();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get field descriptor", ex);
		}
	}

	/**
	 * @param type
	 * 		Generic method declaration.
	 *
	 * @return Internal type.
	 */
	public static String getMethodDesc(ResolvedMethodDeclaration type) {
		if (type instanceof RecafResolvedMethodLikeDeclaration) {
			return ((RecafResolvedMethodLikeDeclaration) type).getMethodInfo().getDescriptor();
		} else if (type instanceof ReflectionMethodDeclaration) {
			return getMethodDesc((ReflectionMethodDeclaration) type);
		} else if (type instanceof ReflectionAnnotationMemberDeclaration) {
			return getMethodDesc((ReflectionAnnotationMemberDeclaration) type);
		} else {
			StringBuilder desc = new StringBuilder("(");
			for (int p = 0; p < type.getNumberOfParams(); p++) {
				ResolvedParameterDeclaration param = type.getParam(p);
				String paramDesc = getTypeDesc(param);
				desc.append(paramDesc);
			}
			ResolvedType returnType = type.getReturnType();
			String returnTypeDesc = getTypeDesc(returnType);
			desc.append(")");
			desc.append(returnTypeDesc);
			return desc.toString();
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(ReflectionMethodDeclaration method) {
		try {
			return Type.getMethodDescriptor((Method) reflectionMethod.get(method));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param method
	 * 		Method declaration.
	 *
	 * @return Method descriptor.
	 */
	public static String getMethodDesc(ReflectionAnnotationMemberDeclaration method) {
		try {
			return Type.getMethodDescriptor((Method) reflectionMethodAnno.get(method));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get method descriptor", ex);
		}
	}

	/**
	 * @param type
	 * 		Generic constructor declaration.
	 *
	 * @return Internal type.
	 */
	public static String getConstructorDesc(ResolvedConstructorDeclaration type) {
		if (type instanceof RecafResolvedMethodLikeDeclaration) {
			return ((RecafResolvedMethodLikeDeclaration) type).getMethodInfo().getDescriptor();
		} else if (type instanceof ReflectionConstructorDeclaration) {
			return getConstructorDesc((ReflectionConstructorDeclaration) type);
		} else {
			StringBuilder desc = new StringBuilder("(");
			for (int p = 0; p < type.getNumberOfParams(); p++) {
				ResolvedType paramType = type.getParam(p).getType();
				String paramDesc = getTypeDesc(paramType);
				desc.append(paramDesc);
			}
			desc.append(")V");
			return desc.toString();
		}
	}

	/**
	 * @param ctor
	 * 		Constructor declaration.
	 *
	 * @return Constructor descriptor.
	 */
	public static String getConstructorDesc(ReflectionConstructorDeclaration ctor) {
		try {
			return Type.getConstructorDescriptor((Constructor<?>) reflectionMethodCtor.get(ctor));
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to get constructor descriptor", ex);
		}
	}

	/**
	 * @param node
	 * 		A node that is linked to a JP AST model.
	 *
	 * @return The type solver field associated with the node. May be {@code null}.
	 */
	public static TypeSolver getTypeSolver(AssociableToAST node) {
		try {
			Field field = ReflectUtil.getDeclaredField(node.getClass(), "typeSolver");
			return ReflectUtil.quietGet(node, field);
		} catch (ReflectiveOperationException ex) {
			return null;
		}
	}

	/**
	 * Get the inner class name of just the {@link AbstractClassDeclaration#getClassName() class name},
	 * not including the package.
	 *
	 * @param type
	 * 		Type to get the name of.
	 * @param wrappedNode
	 * 		AST declaration of the node.
	 *
	 * @return Internal base name of the class.
	 */
	private static String getInternalBaseName(AbstractTypeDeclaration type, TypeDeclaration<?> wrappedNode) {
		// getClassName() behaves similar to Class.getSimpleName(), where the package name is not in the result.
		// A root level class would be "Root" instead of "com.example.Root".
		// An inner class would be "Root.InnerClass".
		// So we can assume if we see a "." that it is an inner class name.
		String className = type.getClassName();
		if (className.contains(".") && isInner(wrappedNode))
			className = className.replace('.', '$');
		return className;
	}

	/**
	 * @param type
	 * 		AST type to check.
	 *
	 * @return {@code true} when the AST node denotes an inner class.
	 * {@code false} for root level classes.
	 */
	private static boolean isInner(TypeDeclaration<?> type) {
		Optional<?> parent = type.getParentNode();
		if (!parent.isPresent()) {
			logger.warn("Type '{}' did not have an associated parent node", type.getClass().getName());
			return false;
		}
		// Inner classes will have the root class as their parent.
		// The root class's parent is the compilation unit.
		return !(parent.get() instanceof CompilationUnit);
	}
}
