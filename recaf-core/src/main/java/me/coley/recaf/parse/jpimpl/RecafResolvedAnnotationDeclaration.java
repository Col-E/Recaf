package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolved type declaration implementation for annotations.
 *
 * @author Matt Coley
 */
public class RecafResolvedAnnotationDeclaration extends RecafResolvedTypeDeclaration implements ResolvedAnnotationDeclaration {
	/**
	 * @param typeSolver
	 * 		Recaf workspace solver.
	 * @param classInfo
	 * 		Recaf class info for the annotation type.
	 */
	public RecafResolvedAnnotationDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo classInfo) {
		super(typeSolver, classInfo);
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		return super.accessSpecifier();
	}

	@Override
	public List<ResolvedAnnotationMemberDeclaration> getAnnotationMembers() {
		return classInfo.getMethods().stream()
				.filter(m -> m.getName().charAt(0) != '<') // shouldn't occur but no harm in checking
				.map(m -> new RecafResolvedAnnotationMemberDeclaration(this, m))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isInheritable() {
		// To implement this, check if the class is annotated with 'java/lang/annotation/Inherited'
		return false;
	}

	/**
	 * Annotation 'fields' are actually methods.
	 */
	private static class RecafResolvedAnnotationMemberDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedAnnotationMemberDeclaration {
		public RecafResolvedAnnotationMemberDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
			super(declaringType, methodInfo);
		}

		@Override
		public Expression getDefaultValue() {
			// We don't need this
			return null;
		}

		@Override
		public ResolvedType getType() {
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
	}
}
