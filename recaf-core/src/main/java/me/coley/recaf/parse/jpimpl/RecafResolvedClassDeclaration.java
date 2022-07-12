package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

import java.util.List;
import java.util.Optional;

public class RecafResolvedClassDeclaration extends RecafResolvedTypeDeclaration implements ResolvedClassDeclaration {
	public RecafResolvedClassDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo classInfo) {
		super(typeSolver, classInfo);
	}

	@Override
	public Optional<ResolvedReferenceType> getSuperClass() {
		return super.getSuperClass();
	}

	@Override
	public List<ResolvedReferenceType> getInterfaces() {
		return super.getInterfaces();
	}

	@Override
	public List<ResolvedReferenceType> getAllSuperClasses() {
		return super.getAllSuperClasses();
	}

	@Override
	public List<ResolvedReferenceType> getAllInterfaces() {
		return super.getAllInterfaces();
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		return super.accessSpecifier();
	}
}
