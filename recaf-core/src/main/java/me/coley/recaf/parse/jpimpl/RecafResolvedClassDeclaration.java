package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;

import java.util.List;
import java.util.Optional;

/**
 * Resolved type declaration implementation for classes.
 *
 * @author Matt Coley
 */
public class RecafResolvedClassDeclaration extends RecafResolvedTypeDeclaration implements ResolvedClassDeclaration {
	/**
	 * @param typeSolver
	 * 		Recaf workspace solver.
	 * @param classInfo
	 * 		Recaf class info for the class type.
	 */
	public RecafResolvedClassDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo classInfo) {
		super(typeSolver, classInfo);
	}

	@Override
	public Optional<ResolvedReferenceType> getSuperClass() {
		// Everything is implemented in the super-type, but under a less strict generic type.
		// So these methods with super calls actually are required to satisfy the more strict types.
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
