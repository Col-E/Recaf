package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.model.SymbolReference;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javassistmodel.JavassistTypeParameter;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Outline for resolved member declarations that are methods.
 * JavaParser treats different "kinds" of methods differently, hence
 * we have this common type which satisfied basically everything off the bat.
 *
 * @author Matt Coley
 */
public abstract class RecafResolvedMethodLikeDeclaration implements ResolvedMethodLikeDeclaration {
	protected final Map<Integer, ResolvedParameterDeclaration> parameterMap = new HashMap<>();
	protected final RecafResolvedTypeDeclaration declaringType;
	protected final MethodInfo methodInfo;
	protected final Type methodType;
	protected List<ResolvedTypeParameterDeclaration> typeParameters;

	/**
	 * @param declaringType
	 * 		Declaring type of this method.
	 * @param methodInfo
	 * 		Information about the method.
	 */
	public RecafResolvedMethodLikeDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		this.declaringType = declaringType;
		this.methodInfo = methodInfo;
		methodType = Type.getMethodType(methodInfo.getDescriptor());
	}

	/**
	 * Source to pull information from.
	 *
	 * @return Information about the method.
	 */
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
	public ResolvedParameterDeclaration getParam(int p) {
		return parameterMap.computeIfAbsent(p, i -> {
			// Parameters need to have insight to type-arguments for proper resolving of generic
			// method usage. Consider streams and lambda types. Resolving an item in a chain of those
			// requires determining the type of the scope prior to the item.
			// A stream of strings with a forEach at the end would allow the element of the forEach
			// to call string methods, despite the actual forEach consuming an object raw type if you look
			// solely at the descriptor.
			WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
			String genericSignature = methodInfo.getSignature();
			if (genericSignature != null) {
				try {
					SignatureAttribute.MethodSignature methodSignature =
							SignatureAttribute.toMethodSignature(genericSignature);
					SignatureAttribute.Type parameterType = methodSignature.getParameterTypes()[i];
					ResolvedType resolvedParameterType = ResolvedTypeUtil.fromGenericType(typeSolver, parameterType, this);
					return new RecafResolvedParameterDeclaration(this, i, resolvedParameterType);
				} catch (Throwable ignored) {
					// fall-through to raw-type parse
				}
			}
			Type argumentType = methodType.getArgumentTypes()[i];
			ResolvedType parameterType = ResolvedTypeUtil.fromDescriptor(typeSolver, argumentType.getDescriptor());
			return new RecafResolvedParameterDeclaration(this, i, parameterType);
		});
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
					return new ReferenceTypeImpl(declaration);
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
		return AccessSpecifier.NONE;
	}

	@Override
	public String getName() {
		return methodInfo.getName();
	}

	@Override
	public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
		if (typeParameters == null) {
			String signature = methodInfo.getSignature();
			if (signature == null)
				typeParameters = Collections.emptyList();
			else {
				try {
					WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
					SignatureAttribute.MethodSignature methodSignature =
							SignatureAttribute.toMethodSignature(signature);
					typeParameters = Arrays.stream(methodSignature.getTypeParameters())
							.map(tp -> new JavassistTypeParameter(tp, this, typeSolver))
							.collect(Collectors.toList());
				} catch (Exception ex) {
					typeParameters = Collections.emptyList();
				}
			}
		}
		return typeParameters;
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
