package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import me.coley.recaf.code.MethodInfo;

/**
 * Resolved member declaration constructor methods.
 *
 * @author Matt Coley
 */
public class RecafResolvedConstructorDeclaration extends RecafResolvedMethodLikeDeclaration implements ResolvedConstructorDeclaration {
	/**
	 * @param declaringType
	 * 		Declaring type of this constructor.
	 * @param methodInfo
	 * 		Information about the constructor.
	 */
	public RecafResolvedConstructorDeclaration(RecafResolvedTypeDeclaration declaringType, MethodInfo methodInfo) {
		super(declaringType, methodInfo);
	}
}
