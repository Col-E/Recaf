package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedTypeParametrizable;
import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedVoidType;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Utilities for creating {@link ResolvedType}.
 *
 * @author Matt Coley
 */
public class ResolvedTypeUtil {
	private static final Logger logger = Logging.get(ResolvedTypeUtil.class);
	private static MethodHandle genericResolve;

	/**
	 * @param typeSolver
	 * 		Recaf workspace solver.
	 * @param desc
	 * 		Descriptor to create a resolved type from.
	 *
	 * @return Resolved type of the descriptor.
	 */
	public static ResolvedType fromDescriptor(WorkspaceTypeSolver typeSolver, String desc) {
		Type type = Type.getType(desc);
		switch (type.getSort()) {
			case Type.VOID:
				return ResolvedVoidType.INSTANCE;
			case Type.BOOLEAN:
			case Type.CHAR:
			case Type.BYTE:
			case Type.SHORT:
			case Type.INT:
			case Type.FLOAT:
			case Type.LONG:
			case Type.DOUBLE:
				return ResolvedPrimitiveType.byName(type.getClassName());
			case Type.ARRAY:
				return new ResolvedArrayType(fromInternal(typeSolver, type.getElementType().getInternalName()));
			default:
				return fromInternal(typeSolver, type.getInternalName());
		}
	}

	/**
	 * @param typeSolver
	 * 		Recaf workspace solver.
	 * @param internal
	 * 		Internal name to create a resolved type from.
	 *
	 * @return Resolved type of the class or primitive.
	 */
	public static ResolvedType fromInternal(WorkspaceTypeSolver typeSolver, String internal) {
		// Handle primitives
		if (internal.length() == 1)
			return fromDescriptor(typeSolver, internal);
		// Handle object types
		Workspace workspace = typeSolver.getWorkspace();
		ClassInfo result = workspace.getResources().getClass(internal);
		if (result != null)
			return new RecafResolvedReferenceType(RecafResolvedTypeDeclaration.from(typeSolver, result));
		throw new ResolveLookupException("Cannot resolve '" + internal + "' to workspace class");
	}

	/**
	 * @param typeSolver
	 * 		Recaf workspace solver.
	 * @param parameterType
	 * 		Signature.
	 * @param parametrizable
	 * 		Parameterizable context.
	 *
	 * @return Resolved type of the signature.
	 *
	 * @throws Throwable
	 * 		If the type could not be resolved.
	 */
	public static ResolvedType fromGenericType(TypeSolver typeSolver, SignatureAttribute.Type parameterType,
											   ResolvedTypeParametrizable parametrizable) throws Throwable {
		// I cannot be fucked to create our own type-parsing necessary to compute this information.
		return (ResolvedType) genericResolve.invoke(parameterType, typeSolver, parametrizable);
	}

	static {
		try {
			Class<?> util = Class.forName("com.github.javaparser.symbolsolver.javassistmodel.JavassistUtils");
			Method signatureTypeToType = util.getDeclaredMethod("signatureTypeToType",
					SignatureAttribute.Type.class, TypeSolver.class, ResolvedTypeParametrizable.class);
			signatureTypeToType.setAccessible(true);
			genericResolve = MethodHandles.lookup().unreflect(signatureTypeToType);
		} catch (Exception ex) {
			logger.error("Failed to unreflect 'JavassistUtils.signatureTypeToType(...)'");
		}
	}
}
