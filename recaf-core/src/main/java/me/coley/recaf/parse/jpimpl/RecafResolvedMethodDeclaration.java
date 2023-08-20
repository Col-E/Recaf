package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.Context;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.core.resolution.TypeVariableResolutionCapability;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;

import java.util.List;

/**
 * Resolved member declaration implementation for methods.
 *
 * @author Matt Coley
 */
public class RecafResolvedMethodDeclaration extends RecafResolvedMethodLikeDeclaration
		implements ResolvedMethodDeclaration, TypeVariableResolutionCapability {
	private ResolvedType resolvedReturnType;

	public RecafResolvedMethodDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		super(declaringType, methodInfo);
	}

	@Override
	public MethodUsage resolveTypeVariables(Context context, List<ResolvedType> parameterTypes) {
		// Seems to be valid in tested cases with parameterized types and lambdas.
		return new MethodUsage(this);
	}

	@Override
	public ResolvedType getReturnType() {
		if (resolvedReturnType == null) {
			WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
			String methodSignature = methodInfo.getSignature();
			if (methodSignature != null) {
				try {
					SignatureAttribute.Type returnType =
							SignatureAttribute.toMethodSignature(methodSignature).getReturnType();
					resolvedReturnType = ResolvedTypeUtil.fromGenericType(typeSolver, returnType, this);
					return resolvedReturnType;
				} catch (Throwable ignored) {
					// fall-through to raw-type parse
				}
			}
			resolvedReturnType = ResolvedTypeUtil.fromDescriptor(typeSolver, methodType.getReturnType().getDescriptor());
		}
		return resolvedReturnType;
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

	@Override
	public String toDescriptor() {
		return methodInfo.getDescriptor();
	}
}
