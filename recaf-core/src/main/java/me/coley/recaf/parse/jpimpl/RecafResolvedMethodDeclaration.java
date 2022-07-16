package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.Context;
import com.github.javaparser.symbolsolver.core.resolution.TypeVariableResolutionCapability;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;

import java.util.List;

public class RecafResolvedMethodDeclaration extends RecafResolvedMethodLikeDeclaration
		implements ResolvedMethodDeclaration, TypeVariableResolutionCapability {
	public RecafResolvedMethodDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		super(declaringType, methodInfo);
	}

	@Override
	public MethodUsage resolveTypeVariables(Context context, List<ResolvedType> parameterTypes) {
		return new MethodUsage(this);
	}

	@Override
	public ResolvedType getReturnType() {
		WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
		String methodSignature = methodInfo.getSignature();
		if (methodSignature != null) {
			try {
				SignatureAttribute.Type returnType =
						SignatureAttribute.toMethodSignature(methodSignature).getReturnType();
				return ResolvedTypeUtil.fromGenericType(typeSolver, returnType, this);
			} catch (Throwable ignored) {
				// fall-through to raw-type parse
			}
		}
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
