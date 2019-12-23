package me.coley.recaf.util;

import com.github.javaparser.resolution.declarations.*;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * JavaParser utilities.
 *
 * @author Matt
 */
public class SourceUtil {
	/**
	 * @param dec
	 * 		Resolved field declaration.
	 *
	 * @return Internal name of the field's owner.
	 */
	public static String getFieldOwner(ResolvedFieldDeclaration dec) {
		ResolvedTypeDeclaration owner = dec.declaringType();
		String prefix = owner.getPackageName().replace(".", "/");
		if(!prefix.isEmpty())
			prefix += "/";
		return prefix + owner.getClassName().replace(".", "$");
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal name of the method's owner.
	 */
	public static String getMethodOwner(ResolvedMethodDeclaration dec) {
		ResolvedTypeDeclaration owner = dec.declaringType();
		String prefix = owner.getPackageName().replace(".", "/");
		if(!prefix.isEmpty())
			prefix += "/";
		return prefix + owner.getClassName().replace(".", "$");
	}

	/**
	 * @param dec
	 * 		Resolved value declaration.
	 *
	 * @return Internal descriptor of the value's type.
	 */
	public static String getValueDesc(ResolvedValueDeclaration dec) {
		return toInternalDesc(dec.getType());
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal descriptor of the method.
	 */
	public static String getMethodDesc(ResolvedMethodDeclaration dec) {
		StringBuilder sb = new StringBuilder("(");
		for(int i = 0; i < dec.getNumberOfParams(); i++)
			sb.append(toInternalDesc(dec.getParam(i).getType()));
		sb.append(")");
		sb.append(toInternalDesc(dec.getReturnType()));
		return sb.toString();
	}

	/**
	 * Converts the resolved type to an internal representation for usage in type descriptors.
	 *
	 * @param type
	 * 		JavaParser resolved type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternalDesc(ResolvedType type) {
		if(type.isVoid())
			return "V";
		if(type.isArray()) {
			ResolvedArrayType array = type.asArrayType();
			return "[" + toInternalDesc(array.getComponentType());
		}
		if(type.isPrimitive()) {
			String name = type.asPrimitive().name().toLowerCase();
			switch(name) {
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
					throw new IllegalStateException("Unknown primitive type: " + name);
			}
		}
		if(type.isReference())
			return "L" + type.asReferenceType().getQualifiedName().replace(".", "/") + ";";
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
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
		if(type.isArray()) {
			ResolvedArrayType array = type.asArrayType();
			return toInternal(array.getComponentType());
		}
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
		if(type.isClass() || type.isEnum() || type.isInterface())
			return type.asReferenceType().getQualifiedName().replace(".", "/");
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}
}
