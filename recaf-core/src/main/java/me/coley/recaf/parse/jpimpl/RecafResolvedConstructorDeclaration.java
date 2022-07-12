package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

public class RecafResolvedConstructorDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedConstructorDeclaration {
	public RecafResolvedConstructorDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo declaring, MethodInfo methodInfo) {
		super(typeSolver, declaring, methodInfo);
	}
}
