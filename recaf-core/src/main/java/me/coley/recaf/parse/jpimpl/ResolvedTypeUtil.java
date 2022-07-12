package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.types.ResolvedArrayType;
import com.github.javaparser.resolution.types.ResolvedPrimitiveType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedVoidType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.workspace.Workspace;
import org.objectweb.asm.Type;

public class ResolvedTypeUtil {
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

	public static ResolvedType fromInternal(WorkspaceTypeSolver typeSolver, String internal) {
		// Handle primitives
		if (internal.length() == 1)
			return fromDescriptor(typeSolver, internal);
		// Handle object types
		Workspace workspace = typeSolver.getWorkspace();
		ClassInfo result = workspace.getResources().getClass(internal);
		if (result != null)
			return new ReferenceTypeImpl(RecafResolvedTypeDeclaration.from(typeSolver, result), typeSolver);
		throw new ResolveLookupException("Cannot resolve '" + internal + "' to workspace class");
	}
}
