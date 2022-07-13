package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;

import java.util.Objects;

public class RecafResolvedFieldDeclaration implements ResolvedFieldDeclaration {
	private final WorkspaceTypeSolver typeSolver;
	private final CommonClassInfo declaring;
	private final FieldInfo fieldInfo;

	public RecafResolvedFieldDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo declaring, FieldInfo fieldInfo) {
		this.typeSolver = typeSolver;
		this.declaring = declaring;
		this.fieldInfo = fieldInfo;
	}

	public CommonClassInfo getDeclaringClassInfo() {
		return declaring;
	}

	public FieldInfo getFieldInfo() {
		return fieldInfo;
	}

	@Override
	public boolean isStatic() {
		return AccessFlag.isStatic(fieldInfo.getAccess());
	}

	@Override
	public boolean isVolatile() {
		return AccessFlag.isVolatile(fieldInfo.getAccess());
	}

	@Override
	public ResolvedTypeDeclaration declaringType() {
		return RecafResolvedTypeDeclaration.from(typeSolver, declaring);
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		int acc = fieldInfo.getAccess();
		if (AccessFlag.isPublic(acc))
			return AccessSpecifier.PUBLIC;
		else if (AccessFlag.isProtected(acc))
			return AccessSpecifier.PROTECTED;
		else if (AccessFlag.isPrivate(acc))
			return AccessSpecifier.PRIVATE;
		return AccessSpecifier.PACKAGE_PRIVATE;
	}

	@Override
	public ResolvedType getType() {
		return ResolvedTypeUtil.fromDescriptor(typeSolver, fieldInfo.getDescriptor());
	}

	@Override
	public String getName() {
		return fieldInfo.getName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass() == RecafResolvedFieldDeclaration.class) {
			RecafResolvedFieldDeclaration that = (RecafResolvedFieldDeclaration) o;
			return declaring.equals(that.declaring) && fieldInfo.equals(that.fieldInfo);
		} else if (o instanceof ResolvedFieldDeclaration) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(declaring, fieldInfo);
	}

	@Override
	public String toString() {
		return getName();
	}
}