package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import me.coley.recaf.code.CommonClassInfo;
import me.coley.recaf.code.MethodInfo;
import me.coley.recaf.parse.WorkspaceTypeSolver;
import me.coley.recaf.util.AccessFlag;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RecafResolvedMethodLikeDeclaration implements ResolvedMethodLikeDeclaration {
	protected final WorkspaceTypeSolver typeSolver;
	protected final CommonClassInfo declaring;
	protected final MethodInfo methodInfo;
	protected final Type methodType;

	public RecafResolvedMethodLikeDeclaration(WorkspaceTypeSolver typeSolver, CommonClassInfo declaring, MethodInfo methodInfo) {
		this.typeSolver = typeSolver;
		this.declaring = declaring;
		this.methodInfo = methodInfo;
		methodType = Type.getMethodType(methodInfo.getDescriptor());
	}

	public CommonClassInfo getDeclaringClassInfo() {
		return declaring;
	}

	public MethodInfo getMethodInfo() {
		return methodInfo;
	}

	@Override
	public ResolvedReferenceTypeDeclaration declaringType() {
		return RecafResolvedTypeDeclaration.from(typeSolver, declaring);
	}

	@Override
	public int getNumberOfParams() {
		return methodType.getArgumentTypes().length;
	}

	@Override
	public ResolvedParameterDeclaration getParam(int i) {
		Type argumentType = methodType.getArgumentTypes()[i];
		return new RecafResolvedParameterDeclaration(this, typeSolver, i, argumentType);
	}

	@Override
	public int getNumberOfSpecifiedExceptions() {
		// Is it worth refactoring MethodInfo to include this info?
		return 0;
	}

	@Override
	public ResolvedType getSpecifiedException(int index) {
		return null;
	}

	@Override
	public AccessSpecifier accessSpecifier() {
		int acc = methodInfo.getAccess();
		if (AccessFlag.isPublic(acc))
			return AccessSpecifier.PUBLIC;
		else if (AccessFlag.isProtected(acc))
			return AccessSpecifier.PROTECTED;
		else if (AccessFlag.isPrivate(acc))
			return AccessSpecifier.PRIVATE;
		return AccessSpecifier.PACKAGE_PRIVATE;
	}

	@Override
	public String getName() {
		return methodInfo.getName();
	}

	@Override
	public List<ResolvedTypeParameterDeclaration> getTypeParameters() {
		return Collections.emptyList();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o instanceof RecafResolvedMethodLikeDeclaration) {
			RecafResolvedMethodLikeDeclaration that = (RecafResolvedMethodLikeDeclaration) o;
			return declaring.equals(that.declaring) && methodInfo.equals(that.methodInfo);
		} else if (o instanceof ResolvedMethodLikeDeclaration) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(declaring, methodInfo);
	}

	@Override
	public String toString() {
		return "RecafResolvedMethodLikeDeclaration{" +
				getQualifiedName() +
				'}';
	}
}
