package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

public class RecafResolvedParameterDeclaration implements ResolvedParameterDeclaration {
	private final RecafResolvedMethodLikeDeclaration methodDeclaration;
	private final WorkspaceTypeSolver typeSolver;
	private final Type argumentType;
	private final int argumentIndex;

	public RecafResolvedParameterDeclaration(RecafResolvedMethodLikeDeclaration methodDeclaration, WorkspaceTypeSolver typeSolver, int argumentIndex, Type argumentType) {
		this.methodDeclaration = methodDeclaration;
		this.typeSolver = typeSolver;
		this.argumentIndex = argumentIndex;
		this.argumentType = argumentType;
	}

	@Override
	public boolean isVariadic() {
		return argumentIndex == methodDeclaration.getNumberOfParams() - 1 &&
				AccessFlag.isVarargs(methodDeclaration.getMethodInfo().getAccess()) &&
				argumentType.getSort() == Type.ARRAY;
	}

	@Override
	public ResolvedType getType() {
		return ResolvedTypeUtil.fromDescriptor(typeSolver, argumentType.getDescriptor());
	}

	@Override
	public boolean hasName() {
		return false;
	}

	@Override
	public String getName() {
		return "p" + argumentIndex;
	}
}
