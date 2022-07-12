package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedAnnotationMemberDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

import java.util.List;
import java.util.stream.Collectors;

public class RecafResolvedAnnotationDeclaration extends RecafResolvedTypeDeclaration implements ResolvedAnnotationDeclaration {
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
				.filter(m -> m.getName().charAt(0) != '<')
				.map(m -> new RecafResolvedAnnotationMemberDeclaration(typeSolver, classInfo, m))
				.collect(Collectors.toList());
	}

	@Override
	public boolean isInheritable() {
		// To implement this, check if the class is annotated with 'java/lang/annotation/Inherited'
		return false;
	}

	private static class RecafResolvedAnnotationMemberDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedAnnotationMemberDeclaration {
		public RecafResolvedAnnotationMemberDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo declaring, MethodInfo methodInfo) {
			super(typeSolver, declaring, methodInfo);
		}

		@Override
		public Expression getDefaultValue() {
			// We don't need this
			return null;
		}

		@Override
		public ResolvedType getType() {
			return ResolvedTypeUtil.fromInternal(typeSolver, methodType.getReturnType().getInternalName());
		}
	}
}
