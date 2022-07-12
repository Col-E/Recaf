package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedInterfaceDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

import java.util.List;

public class RecafResolvedInterfaceDeclaration extends RecafResolvedTypeDeclaration implements ResolvedInterfaceDeclaration {
	public RecafResolvedInterfaceDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo classInfo) {
		super(typeSolver, classInfo);
	}

	@Override
	public List<ResolvedReferenceType> getInterfacesExtended() {
		return getInterfaces();
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		return super.accessSpecifier();
	}
}
