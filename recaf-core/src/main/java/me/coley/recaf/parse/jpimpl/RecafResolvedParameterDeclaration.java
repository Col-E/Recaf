package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.util.AccessFlag;

/**
 * Resolved parameter declaration implementation for methods.
 *
 * @author Matt Coley
 */
public class RecafResolvedParameterDeclaration implements ResolvedParameterDeclaration {
	private final RecafResolvedMethodLikeDeclaration methodDeclaration;
	private final ResolvedType argumentType;
	private final int argumentIndex;

	/**
	 * @param methodDeclaration
	 * 		The method declaration containing the parameter.
	 * @param argumentIndex
	 * 		Parameter index.
	 * @param argumentType
	 * 		Parameter type.
	 */
	public RecafResolvedParameterDeclaration(RecafResolvedMethodLikeDeclaration methodDeclaration,
											 int argumentIndex, ResolvedType argumentType) {
		this.methodDeclaration = methodDeclaration;
		this.argumentIndex = argumentIndex;
		this.argumentType = argumentType;
	}

	@Override
	public boolean isVariadic() {
		return argumentIndex == methodDeclaration.getNumberOfParams() - 1 &&
				AccessFlag.isVarargs(methodDeclaration.getMethodInfo().getAccess()) &&
				argumentType.isArray();
	}

	@Override
	public ResolvedType getType() {
		return argumentType;
	}

	@Override
	public boolean hasName() {
		// JavaParser's resolving logic does not rely on this type supplying a name.
		// So we do not need to implement this feature.
		return false;
	}

	@Override
	public String getName() {
		// Dummy name.
		return "p" + argumentIndex;
	}
}
