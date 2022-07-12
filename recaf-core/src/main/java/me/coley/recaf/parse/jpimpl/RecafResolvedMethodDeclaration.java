package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;

public class RecafResolvedMethodDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedMethodDeclaration {
	public RecafResolvedMethodDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo declaring, MethodInfo methodInfo) {
		super(typeSolver, declaring, methodInfo);
	}

	@Override
	public ResolvedType getReturnType() {
		return ResolvedTypeUtil.fromDescriptor(typeSolver, methodType.getReturnType().getDescriptor());
	}

	@Override
	public boolean isAbstract() {
		return AccessFlag.isAbstract(methodInfo.getAccess());
	}

	@Override
	public boolean isDefaultMethod() {
		return AccessFlag.hasNone(methodInfo.getAccess(),
				AccessFlag.ACC_PUBLIC, AccessFlag.ACC_PROTECTED, AccessFlag.ACC_PRIVATE);
	}

	@Override
	public boolean isStatic() {
		return AccessFlag.isStatic(methodInfo.getAccess());
	}
}
