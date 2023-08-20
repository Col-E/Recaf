package me.coley.recaf.parse.jpimpl;

import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeTransformer;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wrapper for {@link RecafResolvedTypeDeclaration} for reference types.
 * Used mostly for resolving purposes by JavaParser.
 *
 * @author Matt Coley
 */
public class RecafResolvedReferenceType extends ResolvedReferenceType {
	private final RecafResolvedTypeDeclaration declaration;

	/**
	 * @param declaration
	 * 		Wrapped declaration.
	 */
	public RecafResolvedReferenceType(RecafResolvedTypeDeclaration declaration) {
		super(declaration);
		this.declaration = declaration;
	}

	/**
	 * @return Wrapped declaration.
	 */
	public RecafResolvedTypeDeclaration getDeclaration() {
		return declaration;
	}

	@Override
	public ResolvedType transformTypeParameters(ResolvedTypeTransformer transformer) {
		ResolvedType result = this;
		int i = 0;
		for (ResolvedType tp : typeParametersValues()) {
			ResolvedType transformedTp = transformer.transform(tp);
			// Identity comparison on purpose
			if (transformedTp != tp) {
				List<ResolvedType> typeParametersCorrected = result.asReferenceType().typeParametersValues();
				typeParametersCorrected.set(i, transformedTp);
				result = create(typeDeclaration, typeParametersCorrected);
			}
			i++;
		}
		return result;
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
	public List<ResolvedReferenceType> getAllAncestors(Function<ResolvedReferenceTypeDeclaration, List<ResolvedReferenceType>> traverser) {
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
		// Don't need to really implement this.
		return this;
	}

	@Override
	public boolean isRawType() {
		// Modified from the base impl slightly to be more legible for debugging sanity.
		if (typeDeclaration.getTypeParameters().isEmpty())
			return true;
		if (typeParametersMap().isEmpty())
			return true;
		for (String name : typeParametersMap().getNames()) {
			Optional<ResolvedType> value = typeParametersMap().getValueBySignature(name);
			if (value.isPresent()) {
				ResolvedType resolvedType = value.get();
				if (resolvedType.isTypeVariable() && resolvedType.asTypeVariable().qualifiedName().equals(name))
					return false;
			}
		}
		return true;
	}

	@Override
	protected ResolvedReferenceType create(ResolvedReferenceTypeDeclaration typeDeclaration, List<ResolvedType> typeParameters) {
		if (typeDeclaration.equals(getDeclaration()))
			return this;
		if (typeDeclaration instanceof RecafResolvedTypeDeclaration)
			return new RecafResolvedReferenceType((RecafResolvedTypeDeclaration) typeDeclaration);
		return new ReferenceTypeImpl(typeDeclaration, typeParameters);
	}

	@Override
	protected ResolvedReferenceType create(ResolvedReferenceTypeDeclaration typeDeclaration) {
		if (typeDeclaration.equals(getDeclaration()))
			return this;
		if (typeDeclaration instanceof RecafResolvedTypeDeclaration)
			return new RecafResolvedReferenceType((RecafResolvedTypeDeclaration) typeDeclaration);
		return new ReferenceTypeImpl(typeDeclaration);
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
		if (isRawType()) {
			return "RecafResolvedReferenceType{" +
					declaration +
					'}';
		} else {
			return "RecafResolvedReferenceType{" +
					declaration + "<" + typeParametersMap.getTypes().stream().map(ResolvedType::describe).collect(Collectors.joining(", ")) + ">" +
					'}';
		}
	}
}
