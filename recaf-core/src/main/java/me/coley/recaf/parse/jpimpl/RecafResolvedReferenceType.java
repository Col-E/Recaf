package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeTransformer;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RecafResolvedReferenceType extends ResolvedReferenceType {
	private final RecafResolvedTypeDeclaration declaration;

	public RecafResolvedReferenceType(RecafResolvedTypeDeclaration declaration) {
		super(declaration);
		this.declaration = declaration;
	}

	public RecafResolvedTypeDeclaration getDeclaration() {
		return declaration;
	}

	@Override
	public ResolvedType transformTypeParameters(ResolvedTypeTransformer transformer) {
		return this;
	}

	@Override
	public boolean isAssignableBy(ResolvedType other) {
		return declaration.isAssignableBy(other);
	}

	@Override
	public List<ResolvedMethodDeclaration> getAllMethods() {
		return declaration.getAllMethods().stream()
				.map(MethodUsage::getDeclaration)
				.collect(Collectors.toList());
	}

	@Override
	public List<ResolvedReferenceType> getAllAncestors() {
		return declaration.getAllAncestors();
	}

	@Override
	public List<ResolvedReferenceType> getDirectAncestors() {
		List<ResolvedReferenceType> list = new ArrayList<>();
		declaration.getSuperClass().ifPresent(list::add);
		list.addAll(declaration.getAllInterfaces());
		return list;
	}

	@Override
	public Set<MethodUsage> getDeclaredMethods() {
		return declaration.getDeclaredMethods()
				.stream()
				.map(MethodUsage::new)
				.collect(Collectors.toSet());
	}

	@Override
	public Set<ResolvedFieldDeclaration> getDeclaredFields() {
		return new HashSet<>(declaration.getDeclaredFields());
	}

	@Override
	public ResolvedType toRawType() {
		return this;
	}

	@Override
	protected ResolvedReferenceType create(ResolvedReferenceTypeDeclaration typeDeclaration, List<ResolvedType> typeParameters) {
		if (typeDeclaration.equals(getDeclaration()))
			return this;
		if (typeDeclaration instanceof RecafResolvedTypeDeclaration)
			return new RecafResolvedReferenceType((RecafResolvedTypeDeclaration) typeDeclaration);
		return new ReferenceTypeImpl(typeDeclaration, typeParameters, declaration.typeSolver);
	}

	@Override
	protected ResolvedReferenceType create(ResolvedReferenceTypeDeclaration typeDeclaration) {
		if (typeDeclaration.equals(getDeclaration()))
			return this;
		if (typeDeclaration instanceof RecafResolvedTypeDeclaration)
			return new RecafResolvedReferenceType((RecafResolvedTypeDeclaration) typeDeclaration);
		return new ReferenceTypeImpl(typeDeclaration, declaration.typeSolver);
	}

	@Override
	public ResolvedReferenceType deriveTypeParameters(ResolvedTypeParametersMap typeParametersMap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;
		if (o.getClass() == RecafResolvedReferenceType.class) {
			RecafResolvedReferenceType that = (RecafResolvedReferenceType) o;
			return declaration.equals(that.declaration);
		} else if (o instanceof ResolvedReferenceType) {
			return super.equals(o);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return declaration.hashCode();
	}

	@Override
	public String toString() {
		return "RecafResolvedReferenceType{" +
				declaration +
				'}';
	}
}
