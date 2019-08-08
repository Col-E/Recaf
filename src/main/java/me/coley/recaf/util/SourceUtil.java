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
		return owner.getPackageName().replace(".", "/")  + "/" + owner.getClassName().replace(".", "$");
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal name of the method's owner.
	 */
	public static String getMethodOwner(ResolvedMethodDeclaration dec) {
		return dec.getPackageName().replace(".", "/")  + "/" + dec.getClassName().replace(".", "$");
	}

	/**
	 * @param dec
	 * 		Resolved value declaration.
	 *
	 * @return Internal descriptor of the value's type.
	 */
	public static String getValueDesc(ResolvedValueDeclaration dec) {
		return toInternal(dec.getType());
	}

	/**
	 * @param dec
	 * 		Resolved method declaration.
	 *
	 * @return Internal descriptor of the method.
	 */
	public static String getMethodDesc(ResolvedMethodDeclaration dec) {
		StringBuilder sb = new StringBuilder("(");
		for (int i = 0; i < dec.getNumberOfParams(); i++)
			sb.append(toInternal(dec.getParam(i).getType()));
		sb.append(")");
		sb.append(toInternal(dec.getReturnType()));
		return sb.toString();
	}

	/**
	 * Converts the resolved type to an internal representation.
	 *
	 * @param type
	 * 		JavaParser resolved type.
	 *
	 * @return Internalized representation.
	 */
	public static String toInternal(ResolvedType type) {
		if (type.isVoid())
			return "V";
		if (type.isArray()) {
			ResolvedArrayType array = type.asArrayType();
			return "[" + toInternal(array.getComponentType());
		}
		if (type.isPrimitive()) {
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
		if (type.isReference())
			return "L" + type.asReferenceType().getQualifiedName().replace(".", "/") + ";";
		// The above cases should have internalized the name...
		// If not lets be alerted of a uncaught case.
		throw new IllegalStateException("Cannot internalize type: " + type);
	}
}
