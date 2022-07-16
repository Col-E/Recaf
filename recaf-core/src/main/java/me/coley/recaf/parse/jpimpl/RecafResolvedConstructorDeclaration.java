package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import me.coley.recaf.code.MethodInfo;

public class RecafResolvedConstructorDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedConstructorDeclaration {
	public RecafResolvedConstructorDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		super(declaringType, methodInfo);
	}
}
