package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import javassist.bytecode.SignatureAttribute;
import me.coley.recaf.code.FieldInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;

/**
 * Resolved member declaration implementation for fields.
 *
 * @author Matt Coley
 */
public class RecafResolvedFieldDeclaration implements ResolvedFieldDeclaration {
	private final RecafResolvedTypeDeclaration declaringType;
	private final FieldInfo fieldInfo;
	private ResolvedType resolvedType;

	public RecafResolvedFieldDeclaration(RecafResolvedTypeDeclaration declaringType, FieldInfo fieldInfo) {
		this.declaringType = declaringType;
		this.fieldInfo = fieldInfo;
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
		return declaringType;
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
		return AccessSpecifier.NONE;
	}

	@Override
	public ResolvedType getType() {
		if (resolvedType == null) {
			WorkspaceTypeSolver typeSolver = declaringType.typeSolver;
			String methodSignature = fieldInfo.getSignature();
			if (methodSignature != null) {
				try {
					SignatureAttribute.Type returnType =
							SignatureAttribute.toFieldSignature(methodSignature);
					resolvedType = ResolvedTypeUtil.fromGenericType(typeSolver, returnType, declaringType);
					return resolvedType;
				} catch (Throwable ignored) {
					// fall-through to raw-type parse
				}
			}
			resolvedType = ResolvedTypeUtil.fromDescriptor(typeSolver, fieldInfo.getDescriptor());
		}
		return resolvedType;
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
			return fieldInfo.equals(that.fieldInfo);
		} else if (o instanceof ResolvedFieldDeclaration) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return fieldInfo.hashCode();
	}

	@Override
	public String toString() {
		return getName();
	}
}