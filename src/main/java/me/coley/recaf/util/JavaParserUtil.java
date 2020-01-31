package me.coley.recaf.util;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;

import java.util.Optional;

/**
 * JavaParser utilities.
 *
 * @author Matt
 */
public class JavaParserUtil {
	/**
	 * @param type
	 * 		Resolved field declaration.
	 *
	 * @return Descriptor of the resolved field. May be {@code null}.
	 */
	public static String getDescriptor(ResolvedFieldDeclaration type) {
		String desc = null;
		try {
			desc =	getDescriptor(type.getType());
		} catch(UnsolvedSymbolException ex) {
			if (type instanceof JavaParserFieldDeclaration) {
				desc = getDescriptor(((JavaParserFieldDeclaration) type).getWrappedNode().getCommonType());
			}
		} catch(UnsupportedOperationException e) { /* Ignored */ }
		return desc;
	}

	/**
	 * @param type
	 * 		Resolved method declaration.
	 *
	 * @return Descriptor of the resolved method.
	 */
	public static String getDescriptor(ResolvedMethodDeclaration type) {
		Optional<MethodDeclaration> ast = type.toAst();
		String desc = null;
		if (ast.isPresent()) {
			desc = getDescriptor(ast.get());
		} /* else if (type instanceof JavassistMethodDeclaration){
			CtMethod method = Reflect.get(type, "ctMethod");
			if (method != null)
				desc = method.getMethodInfo().getDescriptor();
		} else if (type instanceof ReflectionMethodDeclaration) {
			ReflectionMethodDeclaration ref = (ReflectionMethodDeclaration) type;
			Method method = Reflect.get(ref, "method");
			desc = org.objectweb.asm.Type.getType(method).getDescriptor();
		} */ else {
			StringBuilder sbDesc = new StringBuilder("(");
			// Append the method parameters for the descriptor
			int p = type.getNumberOfParams();
			for (int i = 0; i < p; i++) {
				ResolvedParameterDeclaration param = type.getParam(i);
				String pDesc = null;
				if (param.isType()) {
					pDesc = "L" + param.asType().getQualifiedName().replace('.', '/') + ";";
				} else {
					ResolvedType pType = param.getType();
					pDesc = typeToDesc(pType);
				}
				if (pDesc == null)
					return null;
				sbDesc.append(pDesc);
			}
			// Append the return type for the descriptor
			ResolvedType typeRet = type.getReturnType();
			String retDesc = typeToDesc(typeRet);
			if (retDesc == null) {
				return null;
			}
			sbDesc.append(")");
			sbDesc.append(retDesc);
			return sbDesc.toString();
		}
		return desc;
	}

	/**
	 * @param type
	 * 		Resolved constructor declaration.
	 *
	 * @return Descriptor of the resolved constructor.
	 */
	public static String getDescriptor(ResolvedConstructorDeclaration type) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the constructor parameters for the descriptor
		int p = type.getNumberOfParams();
		for(int i = 0; i < p; i++) {
			ResolvedParameterDeclaration param = type.getParam(i);
			sbDesc.append(typeToDesc(param.getType()));
		}
		sbDesc.append(")V");
		return sbDesc.toString();
	}

	/**
	 * @param md
	 *            JavaParser method declaration.
	 * @return Internal descriptor from declaration, or {@code null} if any parsing
	 *         failures occured.
	 */
	public static String getDescriptor(MethodDeclaration md) {
		StringBuilder sbDesc = new StringBuilder("(");
		// Append the method parameters for the descriptor
		NodeList<Parameter> params = md.getParameters();
		for (Parameter param : params) {
			Type pType = param.getType();
			String pDesc = getDescriptor(pType);
			if (pDesc == null)
				return null;
			sbDesc.append(pDesc);
		}
		// Append the return type for the descriptor
		Type typeRet = md.getType();
		String retDesc = getDescriptor(typeRet);
		if (retDesc == null)
			return null;
		sbDesc.append(")");
		sbDesc.append(retDesc);
		return sbDesc.toString();
	}

	/**
	 * @param dec
	 * 		Resolved value declaration.
	 *
	 * @return Internal descriptor of the value's type.
	 */
	public static String getDescriptor(ResolvedValueDeclaration dec) {
		return getDescriptor(dec.getType());
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	public static String getDescriptor(ResolvedType type) {
		if (type.isArray())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return type.isPrimitive() ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * @param type
	 * 		JavaParser type.
	 *
	 * @return Internal descriptor from type, assuming the type is available or if it is a
	 * primitive or void type.
	 */
	public static String getDescriptor(Type type) {
		if (type.isArrayType())
			return "[" + getDescriptor(type.asArrayType().getComponentType());
		return isPrim(type) ? primTypeToDesc(type) : typeToDesc(type);
	}

	/**
	 * Converts the resolved type to an internal representation.
	 * If the type is an array the component type's internal name is returned.
	 * Primitives return their boxed names.
	 *
	 * @param type
	 * 		JavaParser resolved type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternal(ResolvedType type) {
		if(type.isVoid() || type.isPrimitive())
			return type.asPrimitive().getBoxTypeQName().replace(".", "/");
		if(type.isArray())
			return toInternal(type.asArrayType().getComponentType());
		if(type.isReference())
			return type.asReferenceType().getQualifiedName().replace(".", "/");
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}

	/**
	 * Converts the resolved type to an internal representation.
	 *
	 * @param type
	 * 		JavaParser resolved declaration type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternal(ResolvedTypeDeclaration type) {
		if(type.isClass() || type.isEnum() || type.isInterface()) {
			String packagee = type.getPackageName();
			String simple = type.isClass() ?
					type.asClass().getName() :
					type.isEnum() ? type.asEnum().getName() :
							type.asInterface().getName();
			String full = type.asReferenceType().getQualifiedName().replace('.', '/');
			String prefix = (packagee == null || packagee.isEmpty()) ? "" : packagee.replace('.', '/') + "/";
			// Test if normal class
			if (full.equals(prefix + simple))
				return full;
			// It's an inner class.
			return prefix + type.getClassName().replace('.', '$');
		}
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}

	/**
	 * @param dec
	 * 		Resolved field declaration.
	 *
	 * @return Internal name of the field's owner.
	 */
	public static String getOwner(ResolvedFieldDeclaration dec) {
		return toInternal(dec.declaringType());
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal name of the method's owner.
	 */
	public static String getOwner(ResolvedMethodDeclaration dec) {
		return toInternal(dec.declaringType());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(ResolvedType type) {
		String qualified = null;
		if(type instanceof ResolvedTypeVariable)
			qualified = ((ResolvedTypeVariable) type).qualifiedName();
		else if(type instanceof ResolvedTypeParameterDeclaration)
			qualified = type.asTypeParameter().getQualifiedName();
		else if(type.isPrimitive())
			return primTypeToDesc(type.asPrimitive());
		else if(type.isVoid())
			return "V";
		else
			qualified = type.describe();
		if(qualified == null)
			return null;
		if(qualified.contains("<") && qualified.contains(">"))
			qualified = qualified.substring(0, qualified.indexOf('<'));
		StringBuilder sbDesc = new StringBuilder();
		for(int i = 0; i < type.arrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(qualified.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be an object type.
	 * @return Internal descriptor from type, assuming the type is available.
	 */
	private static String typeToDesc(Type type) {
		String key = null;
		if (type instanceof ClassOrInterfaceType) {
			try {
				key = toInternal(((ClassOrInterfaceType) type).resolve().getTypeDeclaration());
			} catch(UnsolvedSymbolException ex) {
				Log.warn("Failed to resolve type '{}'", ex.getName());
			}
		}
		if (key == null)
			key = type.asString();
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < type.getArrayLevel(); i++)
			sbDesc.append("[");
		sbDesc.append("L");
		sbDesc.append(key.replace('.', '/'));
		sbDesc.append(";");
		return sbDesc.toString();
	}

	/**
	 * @param type
	 *            JavaParser type.
	 * @return {@code true} if the type denotes a primitive or void type.
	 */
	private static boolean isPrim(Type type) {
		// void is not a primitive, but lets just pretend it is.
		return type.isVoidType() || type.isPrimitiveType();
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(ResolvedType type) {
		return primTypeToDesc(type.describe(), type.arrayLevel());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(Type type) {
		return primTypeToDesc(type.asString(), type.getArrayLevel());
	}

	/**
	 * @param type
	 *            JavaParser type. Must be a primitive.
	 * @return Internal descriptor.
	 */
	private static String primTypeToDesc(String type, int arrayLevel) {
		String desc = null;
		switch (type) {
			case "boolean":
				desc = "Z";
				break;
			case "int":
				desc = "I";
				break;
			case "long":
				desc = "J";
				break;
			case "short":
				desc = "S";
				break;
			case "byte":
				desc = "B";
				break;
			case "double":
				desc = "D";
				break;
			case "float":
				desc = "F";
				break;
			case "void":
				desc = "V";
				break;
			default:
				throw new RuntimeException("Unknown primitive type field '" + type + "'");
		}
		StringBuilder sbDesc = new StringBuilder();
		for (int i = 0; i < arrayLevel; i++)
			sbDesc.append("[");
		sbDesc.append(desc);
		return sbDesc.toString();
	}

	// ==================================================================================== //

	/**
	 * @param node
	 * 		Node to resolve.
	 *
	 * @return If the node is a class, {@code {name}}.<br>If the node is a member, {@code {owner,
	 * name, desc}}.
	 */
	public static String[] resolveReference(Node node) {
		if(!(node instanceof Resolvable))
			return null;
		// Resolve node to some declaration type
		Resolvable<?> r = (Resolvable<?>) node;
		Object resolved = null;
		try {
			resolved = r.resolve();
		} catch(Exception ex) {
			return null;
		}
		if (resolved instanceof ResolvedMethodDeclaration) {
			ResolvedMethodDeclaration type = (ResolvedMethodDeclaration) resolved;
			ResolvedTypeDeclaration declaring = type.declaringType();
			String owner = declaring.getQualifiedName().replace('.', '/');
			String name = type.getName();
			String desc = getDescriptor(type);
			return new String[] { owner, name, desc };
		} else if (resolved instanceof ResolvedFieldDeclaration) {
			ResolvedFieldDeclaration type = (ResolvedFieldDeclaration) resolved;
			ResolvedTypeDeclaration declaring = type.declaringType();
			String owner = declaring.getQualifiedName().replace('.', '/');
			String name = type.getName();
			String desc = getDescriptor(type);
			return new String[] { owner, name, desc };
		} else if (resolved instanceof ResolvedTypeDeclaration) {
			ResolvedTypeDeclaration owner = (ResolvedTypeDeclaration) resolved;
			String ownerInternal = owner.getQualifiedName().replace('.', '/');
			return new String[] { ownerInternal };
		}
		return null;
	}

	// ==================================================================================== //


	/**
	 * Finds the member by the given name and descriptor and returns its range.
	 *
	 * @param unit
	 * 		AST tree.
	 * @param name
	 * 		Member name.
	 * @param desc
	 * 		Member descriptor.
	 *
	 * @return Range of member if the member exists.
	 */
	public static Optional<Range> getMemberRange(CompilationUnit unit, String name, String desc) {
		Optional<Range> range = Optional.empty();
		if(desc.contains("(")) {
			// Methods
			Optional<MethodDeclaration> opt = unit.findFirst(MethodDeclaration.class,
					(MethodDeclaration md) ->
							name.equals(md.getName().asString()) && desc.equals(getDescriptor(md)));
			if(opt.isPresent())
				range = opt.get().getName().getRange();
		} else {
			// Fields
			Optional<FieldDeclaration> opt = unit.findFirst(FieldDeclaration.class, (FieldDeclaration fd) -> {
				VariableDeclarator vd = fd.getVariable(0);
				return name.equals(vd.getName().asString()) &&
						desc.equals(getDescriptor(vd.getType()));
			});
			if(opt.isPresent())
				range = opt.get().getVariable(0).getName().getRange();
		}
		return range;
	}
}
