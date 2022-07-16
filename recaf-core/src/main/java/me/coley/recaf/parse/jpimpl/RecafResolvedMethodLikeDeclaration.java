package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistTypeParameter;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RecafResolvedMethodLikeDeclaration implements ResolvedMethodLikeDeclaration {
	protected final RecafResolvedTypeDeclaration declaringType;
	protected final MethodInfo methodInfo;
	protected final Type methodType;

	public RecafResolvedMethodLikeDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		this.declaringType = declaringType;
		this.methodInfo = methodInfo;
		methodType = Type.getMethodType(methodInfo.getDescriptor());
	}

	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Override
	public ResolvedReferenceTypeDeclaration declaringType() {
		return declaringType;
	}

	@Override
	public int getNumberOfParams() {
		return methodType.getArgumentTypes().length;
	}

	@Override
	public ResolvedParameterDeclaration getParam(int i) {
		WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
		String genericSignature = methodInfo.getSignature();
		if (genericSignature != null) {
			// TODO: Cache and/or switch to ASM
			try {
				SignatureAttribute.MethodSignature methodSignature =
						SignatureAttribute.toMethodSignature(genericSignature);
				SignatureAttribute.Type parameterType = methodSignature.getParameterTypes()[i];
				ResolvedType resolvedParameterType = ResolvedTypeUtil.fromGenericType(typeSolver, parameterType, this);
				return new RecafResolvedParameterDeclaration(this, typeSolver, i, resolvedParameterType);
			} catch (Throwable ignored) {
				// fall-through to raw-type parse
			}
		}
		Type argumentType = methodType.getArgumentTypes()[i];
		ResolvedType parameterType = ResolvedTypeUtil.fromDescriptor(typeSolver, argumentType.getDescriptor());
		return new RecafResolvedParameterDeclaration(this, typeSolver, i, parameterType);
	}

	@Override
	public int getNumberOfSpecifiedExceptions() {
		return methodInfo.getExceptions().size();
	}

	@Override
	public ResolvedType getSpecifiedException(int index) {
		if (index < getNumberOfSpecifiedExceptions()) {
			WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
			String exceptionType = methodInfo.getExceptions().get(index);
			SymbolReference<ResolvedReferenceTypeDeclaration> reference =
					typeSolver.tryToSolveType(exceptionType);
			if (reference.isSolved()) {
				ResolvedReferenceTypeDeclaration declaration = reference.getCorrespondingDeclaration();
				if (declaration instanceof RecafResolvedTypeDeclaration)
					return new RecafResolvedReferenceType((RecafResolvedTypeDeclaration) declaration);
				else
					return new ReferenceTypeImpl(declaration, typeSolver);
			}
			throw new ResolveLookupException("Failed to resolve: " + exceptionType);
		} else {
			throw new IllegalArgumentException("Invalid exception index: " + index);
		}
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		int acc = methodInfo.getAccess();
		if (AccessFlag.isPublic(acc))
			return AccessSpecifier.PUBLIC;
		else if (AccessFlag.isProtected(acc))
			return AccessSpecifier.PROTECTED;
		else if (AccessFlag.isPrivate(acc))
			return AccessSpecifier.PRIVATE;
		return AccessSpecifier.PACKAGE_PRIVATE;
	}

	@Override
	public String getName() {
		return methodInfo.getName();
	}

	@Override
	public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
		String signature = methodInfo.getSignature();
		if (signature == null)
			return Collections.emptyList();
		else {
			// TODO: Cache and/or switch to ASM
			try {
				WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
				SignatureAttribute.MethodSignature methodSignature =
						SignatureAttribute.toMethodSignature(signature);
				return Arrays.stream(methodSignature.getTypeParameters())
						.map(tp -> new JavassistTypeParameter(tp, this, typeSolver))
						.collect(Collectors.toList());
			} catch (Exception ex) {
				return Collections.emptyList();
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof RecafResolvedMethodLikeDeclaration) {
			RecafResolvedMethodLikeDeclaration that = (RecafResolvedMethodLikeDeclaration) o;
			return methodInfo.equals(that.methodInfo);
		} else if (o instanceof ResolvedMethodLikeDeclaration) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return methodInfo.hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}
