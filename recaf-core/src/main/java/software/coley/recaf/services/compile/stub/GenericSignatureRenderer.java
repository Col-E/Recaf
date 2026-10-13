package software.coley.recaf.services.compile.stub;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import software.coley.recaf.RecafConstants;
import software.coley.recaf.util.Types;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Renders generic signatures as Java source types.
 *
 * @author Matt Coley
 */
public final class GenericSignatureRenderer {
	private GenericSignatureRenderer() {}

	/**
	 * @param signature
	 * 		Class signature.
	 *
	 * @return Parsed class signature, or {@code null} when the signature is absent or malformed.
	 */
	@Nullable
	public static ClassSignature renderClass(@Nullable String signature) {
		return renderClass(signature, Map.of());
	}

	/**
	 * @param signature
	 * 		Class signature.
	 * @param enclosingTypeVariableErasures
	 * 		Erased descriptors of enclosing class type variables.
	 *
	 * @return Parsed class signature, or {@code null} when the signature is absent or malformed.
	 */
	@Nullable
	public static ClassSignature renderClass(@Nullable String signature,
	                                         @Nonnull Map<String, String> enclosingTypeVariableErasures) {
		if (signature == null || signature.isBlank())
			return null;
		try {
			ClassSignatureVisitor visitor = new ClassSignatureVisitor();
			new SignatureReader(signature).accept(visitor);
			return visitor.finish(enclosingTypeVariableErasures);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	/**
	 * @param signature
	 * 		Method signature.
	 * @param enclosingTypeVariableErasures
	 * 		Erased descriptors of class and enclosing type variables.
	 *
	 * @return Parsed method signature, or {@code null} when the signature is absent or malformed.
	 */
	@Nullable
	public static MethodSignature renderMethod(@Nullable String signature,
	                                           @Nonnull Map<String, String> enclosingTypeVariableErasures) {
		if (signature == null || signature.isBlank())
			return null;
		try {
			MethodSignatureVisitor visitor = new MethodSignatureVisitor();
			new SignatureReader(signature).accept(visitor);
			return visitor.finish(enclosingTypeVariableErasures);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	/**
	 * @param signature
	 * 		Field or local variable signature.
	 * @param typeVariableErasures
	 * 		Erased descriptors of in-scope type variables.
	 *
	 * @return Parsed type signature, or {@code null} when the signature is absent or malformed.
	 */
	@Nullable
	public static TypeSignature renderType(@Nullable String signature,
	                                       @Nonnull Map<String, String> typeVariableErasures) {
		if (signature == null || signature.isBlank())
			return null;
		try {
			TypeCapture capture = new TypeCapture();
			new SignatureReader(signature).acceptType(new TypeVisitor(capture::set));
			return capture.finish(typeVariableErasures);
		} catch (RuntimeException ex) {
			return null;
		}
	}

	/**
	 * @param typeParameters
	 * 		Formal type parameters.
	 *
	 * @return Java source spelling of the formal type parameters, or an empty string when there are none.
	 */
	@Nonnull
	private static String renderTypeParameters(@Nonnull List<TypeParameter> typeParameters) {
		if (typeParameters.isEmpty())
			return "";
		StringBuilder builder = new StringBuilder("<");
		for (int i = 0; i < typeParameters.size(); i++) {
			if (i > 0)
				builder.append(", ");
			TypeParameter typeParameter = typeParameters.get(i);
			builder.append(typeParameter.name());
			if (!typeParameter.bounds().isEmpty())
				builder.append(" extends ").append(String.join(" & ", typeParameter.bounds().stream().map(TypeSignature::sourceType).toList()));
		}
		return builder.append('>').toString();
	}

	/**
	 * @param typeParameters
	 * 		Formal type parameters.
	 * @param enclosingErasures
	 * 		Erased descriptors of enclosing class type variables.
	 *
	 * @return Map of type parameter names to their erased descriptors.
	 */
	@Nonnull
	private static Map<String, String> resolveTypeParameterErasures(@Nonnull List<TypeParameter> typeParameters,
	                                                                @Nonnull Map<String, String> enclosingErasures) {
		// Ordered map to preserve the order of type parameters in the signature.
		Map<String, TypeParameter> parametersByName = new LinkedHashMap<>();
		for (TypeParameter parameter : typeParameters) {
			if (parametersByName.putIfAbsent(parameter.name(), parameter) != null)
				throw new IllegalArgumentException("Duplicate type parameter: " + parameter.name());
		}

		// Resolve erasures of type parameters, using enclosing erasures as a fallback for unbound type variables.
		Map<String, String> resolved = new LinkedHashMap<>(enclosingErasures);
		for (TypeParameter parameter : typeParameters)
			resolved.remove(parameter.name());
		Set<String> resolving = new HashSet<>();
		for (TypeParameter parameter : typeParameters) {
			String erasure = resolveTypeParameterErasure(parameter.name(), parametersByName, enclosingErasures, resolved, resolving);
			if (erasure == null)
				throw new IllegalArgumentException("Cannot resolve erasure of type parameter: " + parameter.name());
			resolved.put(parameter.name(), erasure);
		}
		return Collections.unmodifiableMap(resolved);
	}

	/**
	 * @param name
	 * 		Name of the type parameter to resolve.
	 * @param parametersByName
	 * 		Map of type parameter names to their definitions.
	 * @param enclosingErasures
	 * 		Erased descriptors of enclosing class type variables.
	 * @param resolved
	 * 		Map of already resolved type parameter erasures.
	 * @param resolving
	 * 		Set of type parameters currently being resolved, used to detect cycles.
	 *
	 * @return Erased descriptor of the type parameter, or {@code null} if it cannot be resolved.
	 */
	@Nullable
	private static String resolveTypeParameterErasure(@Nonnull String name,
	                                                  @Nonnull Map<String, TypeParameter> parametersByName,
	                                                  @Nonnull Map<String, String> enclosingErasures,
	                                                  @Nonnull Map<String, String> resolved,
	                                                  @Nonnull Set<String> resolving) {
		// Check if the parameter isn't declared in this signature, and if so, fallback to enclosing erasures.
		TypeParameter parameter = parametersByName.get(name);
		if (parameter == null) {
			String existing = resolved.get(name);
			return existing == null ? enclosingErasures.get(name) : existing;
		}

		// If the parameter is already resolved, return the cached value.
		String existing = resolved.get(name);
		if (existing != null)
			return existing;

		// If the parameter is currently being resolved, we have a cycle, and we cannot resolve it.
		if (!resolving.add(name))
			return null;
		String erasure = parameter.bounds().isEmpty()
				? "Ljava/lang/Object;"
				: resolveErasure(parameter.bounds().getFirst().erasedDescriptor(), parametersByName,
				enclosingErasures, resolved, resolving);
		resolving.remove(name);
		if (erasure != null)
			resolved.put(name, erasure);
		return erasure;
	}

	/**
	 * @param descriptor
	 * 		Descriptor to resolve.
	 * @param parametersByName
	 * 		Map of type parameter names to their definitions.
	 * @param enclosingErasures
	 * 		Erased descriptors of enclosing class type variables.
	 * @param resolved
	 * 		Map of already resolved type parameter erasures.
	 * @param resolving
	 * 		Set of type parameters currently being resolved, used to detect cycles.
	 *
	 * @return Erased descriptor of the type, or {@code null} if it cannot be resolved.
	 */
	@Nullable
	private static String resolveErasure(@Nullable String descriptor,
	                                     @Nonnull Map<String, TypeParameter> parametersByName,
	                                     @Nonnull Map<String, String> enclosingErasures,
	                                     @Nonnull Map<String, String> resolved,
	                                     @Nonnull Set<String> resolving) {
		// Junk empty descriptors are not valid, and cannot be resolved.
		if (descriptor == null || descriptor.isEmpty())
			return null;

		// Array descriptors are resolved by resolving the component type, and prepending the array dimension.
		if (descriptor.charAt(0) == '[') {
			String component = resolveErasure(descriptor.substring(1), parametersByName, enclosingErasures, resolved, resolving);
			return component == null ? null : '[' + component;
		}

		// Type variable descriptors are resolved by resolving the type parameter erasure.
		if (descriptor.charAt(0) == 'T' && descriptor.endsWith(";")) {
			String name = descriptor.substring(1, descriptor.length() - 1);
			return resolveTypeParameterErasure(name, parametersByName, enclosingErasures, resolved, resolving);
		}

		return descriptor;
	}

	/**
	 * @param signature
	 * 		Type signature to resolve.
	 * @param typeVariableErasures
	 * 		Erased descriptors of in-scope type variables.
	 *
	 * @return Resolved type signature, with erased descriptor filled in.
	 */
	@Nonnull
	private static TypeSignature resolveType(@Nonnull TypeSignature signature,
	                                         @Nonnull Map<String, String> typeVariableErasures) {
		String erasure = resolveErasure(signature.erasedDescriptor(), Map.of(), typeVariableErasures,
				new HashMap<>(typeVariableErasures), new HashSet<>());
		return new TypeSignature(signature.sourceType(), erasure, signature.typeVariables());
	}

	/**
	 * @param name
	 * 		Internal class name to check.
	 *
	 * @throws IllegalArgumentException
	 * 		When the internal class name is unsafe.
	 */
	private static void ensureSafeInternalClassName(@Nonnull String name) {
		for (String part : name.split("/", -1))
			ensureSafeIdentifier(part);
	}

	/**
	 * @param name
	 * 		Identifier to check.
	 *
	 * @throws IllegalArgumentException
	 * 		When the identifier is unsafe.
	 */
	private static void ensureSafeIdentifier(@Nonnull String name) {
		if (!ClassStubGenerator.isSafeName(name))
			throw new IllegalArgumentException("Unsafe identifier in generic signature: " + name);
	}

	/**
	 * Parsed type signature.
	 *
	 * @param sourceType
	 * 		Java source spelling of the type.
	 * @param erasedDescriptor
	 * 		Erased descriptor, or {@code null} if a type variable could not be resolved.
	 * @param typeVariables
	 * 		Type variable names referenced by this type.
	 */
	public record TypeSignature(@Nonnull String sourceType,
	                            @Nullable String erasedDescriptor,
	                            @Nonnull Set<String> typeVariables) {}

	/**
	 * Parsed class signature.
	 *
	 * @param typeParameters
	 * 		Java source spelling of the formal type parameters.
	 * @param typeVariableErasures
	 * 		Erased descriptors of class and enclosing type variables.
	 * @param superclass
	 * 		Generic superclass type.
	 * @param interfaces
	 * 		Generic interface types in class-file order.
	 */
	public record ClassSignature(@Nonnull String typeParameters,
	                             @Nonnull Map<String, String> typeVariableErasures,
	                             @Nonnull TypeSignature superclass,
	                             @Nonnull List<TypeSignature> interfaces) {}

	/**
	 * Parsed method signature.
	 *
	 * @param typeParameters
	 * 		Java source spelling of the formal type parameters.
	 * @param typeVariableErasures
	 * 		Erased descriptors of enclosing and method type variables.
	 * @param parameterTypes
	 * 		Method parameter types.
	 * @param returnType
	 * 		Method return type.
	 */
	public record MethodSignature(@Nonnull String typeParameters,
	                              @Nonnull Map<String, String> typeVariableErasures,
	                              @Nonnull List<TypeSignature> parameterTypes,
	                              @Nonnull TypeSignature returnType) {}

	/**
	 * Parsed type parameter.
	 *
	 * @param name
	 * 		Name of the type parameter.
	 * @param bounds
	 * 		Type bounds of the type parameter, in class-file order.
	 */
	private record TypeParameter(@Nonnull String name, @Nonnull List<TypeSignature> bounds) {}

	/**
	 * Helper for capturing a single type signature from a visitor.
	 */
	private static final class TypeCapture {
		private TypeSignature value;

		private void set(@Nonnull TypeSignature value) {
			if (this.value != null)
				throw new IllegalArgumentException("Multiple types in type signature");
			this.value = value;
		}

		@Nonnull
		private TypeSignature finish(@Nonnull Map<String, String> typeVariableErasures) {
			if (value == null)
				throw new IllegalArgumentException("Missing type signature");
			return resolveType(value, typeVariableErasures);
		}
	}

	/**
	 * Builder for a possibly bound type parameter.
	 */
	private static final class TypeParameterBuilder {
		private final List<TypeSignature> bounds = new ArrayList<>();
		private final String name;

		private TypeParameterBuilder(@Nonnull String name) {
			this.name = name;
		}

		@Nonnull
		private TypeParameter build() {
			return new TypeParameter(name, List.copyOf(bounds));
		}

		private void add(@Nonnull TypeSignature type) {
			bounds.add(type);
		}
	}

	/**
	 * Visitor for parsing type parameters from a generic signature.
	 */
	private abstract static class TypeParameterVisitor extends SignatureVisitor {
		private final List<TypeParameterBuilder> typeParameters = new ArrayList<>();
		private TypeParameterBuilder current;

		protected TypeParameterVisitor() {
			super(RecafConstants.getAsmVersion());
		}

		@Override
		public void visitFormalTypeParameter(String name) {
			ensureSafeIdentifier(name);
			if (typeParameters.stream().anyMatch(parameter -> parameter.name.equals(name)))
				throw new IllegalArgumentException("Duplicate type parameter: " + name);
			current = new TypeParameterBuilder(name);
			typeParameters.add(current);
		}

		@Override
		public SignatureVisitor visitClassBound() {
			return addBound();
		}

		@Override
		public SignatureVisitor visitInterfaceBound() {
			return addBound();
		}

		@Nonnull
		private SignatureVisitor addBound() {
			if (current == null)
				throw new IllegalArgumentException("Type bound without a formal parameter");
			return new TypeVisitor(current::add);
		}

		@Nonnull
		protected final List<TypeParameter> typeParameters() {
			return typeParameters.stream().map(TypeParameterBuilder::build).toList();
		}
	}

	/**
	 * Visitor for parsing a class signature.
	 */
	private static final class ClassSignatureVisitor extends TypeParameterVisitor {
		private final List<TypeSignature> interfaces = new ArrayList<>();
		private TypeSignature superclass;

		@Override
		public SignatureVisitor visitSuperclass() {
			if (superclass != null)
				throw new IllegalArgumentException("Duplicate superclass in class signature");
			return new TypeVisitor(value -> superclass = value);
		}

		@Override
		public SignatureVisitor visitInterface() {
			return new TypeVisitor(interfaces::add);
		}

		@Nonnull
		private ClassSignature finish(@Nonnull Map<String, String> enclosingTypeVariableErasures) {
			if (superclass == null)
				throw new IllegalArgumentException("Class signature missing superclass");

			// Compute erasures of type parameters, and validate that all type variables are bound.
			List<TypeParameter> parameters = typeParameters();
			Map<String, String> erasures = resolveTypeParameterErasures(parameters, enclosingTypeVariableErasures);

			// Validate that all type variables in bounds are declared.
			Set<String> declaredNames = new HashSet<>(erasures.keySet());
			List<TypeSignature> bounds = parameters.stream().flatMap(parameter -> parameter.bounds().stream()).toList();
			if (bounds.stream().anyMatch(bound -> !declaredNames.containsAll(bound.typeVariables())))
				throw new IllegalArgumentException("Unbound type variable in class bounds");

			// Validate that all type variables in the superclass and interfaces are declared.
			TypeSignature resolvedSuperclass = resolveType(superclass, erasures);
			List<TypeSignature> resolvedInterfaces = interfaces.stream().map(type -> resolveType(type, erasures)).toList();
			if (!declaredNames.containsAll(resolvedSuperclass.typeVariables())
					|| resolvedInterfaces.stream().anyMatch(type -> !declaredNames.containsAll(type.typeVariables())))
				throw new IllegalArgumentException("Unbound type variable in class signature");

			return new ClassSignature(renderTypeParameters(parameters), erasures, resolvedSuperclass, resolvedInterfaces);
		}
	}

	/**
	 * Visitor for parsing a method signature.
	 */
	private static final class MethodSignatureVisitor extends TypeParameterVisitor {
		private final List<TypeSignature> parameterTypes = new ArrayList<>();
		private final List<TypeSignature> exceptionTypes = new ArrayList<>();
		private TypeSignature returnType;

		@Override
		public SignatureVisitor visitParameterType() {
			return new TypeVisitor(parameterTypes::add);
		}

		@Override
		public SignatureVisitor visitReturnType() {
			if (returnType != null)
				throw new IllegalArgumentException("Duplicate return type in method signature");
			return new TypeVisitor(value -> returnType = value);
		}

		@Override
		public SignatureVisitor visitExceptionType() {
			return new TypeVisitor(exceptionTypes::add);
		}

		@Nonnull
		private MethodSignature finish(@Nonnull Map<String, String> enclosingTypeVariableErasures) {
			if (returnType == null)
				throw new IllegalArgumentException("Method signature missing return type");

			// Compute erasures of type parameters, and validate that all type variables are bound.
			List<TypeParameter> parameters = typeParameters();
			Map<String, String> erasures = resolveTypeParameterErasures(parameters, enclosingTypeVariableErasures);

			// Validate that all type variables in bounds are declared.
			Set<String> declaredNames = new HashSet<>(erasures.keySet());
			List<TypeSignature> bounds = parameters.stream().flatMap(parameter -> parameter.bounds().stream()).toList();
			if (bounds.stream().anyMatch(bound -> !declaredNames.containsAll(bound.typeVariables())))
				throw new IllegalArgumentException("Unbound type variable in method bounds");

			// Validate that all type variables in the parameters, return type, and exceptions are declared.
			List<TypeSignature> resolvedParameters = parameterTypes.stream().map(type -> resolveType(type, erasures)).toList();
			TypeSignature resolvedReturn = resolveType(returnType, erasures);
			if (resolvedParameters.stream().anyMatch(type -> !declaredNames.containsAll(type.typeVariables()))
					|| !declaredNames.containsAll(resolvedReturn.typeVariables())
					|| exceptionTypes.stream().anyMatch(type -> !declaredNames.containsAll(type.typeVariables())))
				throw new IllegalArgumentException("Unbound type variable in method signature");
			return new MethodSignature(renderTypeParameters(parameters), erasures, resolvedParameters, resolvedReturn);
		}
	}

	/**
	 * Visitor for parsing a type signature.
	 */
	private static final class TypeVisitor extends SignatureVisitor {
		private final Set<String> typeVariables = new LinkedHashSet<>();
		private final StringBuilder source = new StringBuilder();
		private final StringBuilder internalName = new StringBuilder();
		private final Consumer<TypeSignature> consumer;
		private boolean classType;
		private boolean typeArgumentsOpen;
		private boolean completed;

		private TypeVisitor(@Nonnull Consumer<TypeSignature> consumer) {
			super(RecafConstants.getAsmVersion());
			this.consumer = consumer;
		}

		@Override
		public void visitBaseType(char descriptor) {
			String type = Types.primitiveToSource(descriptor);
			if (type == null)
				throw new IllegalArgumentException("Invalid base type descriptor: " + descriptor);
			complete(type, String.valueOf(descriptor));
		}

		@Override
		public void visitTypeVariable(String name) {
			ensureSafeIdentifier(name);
			typeVariables.add(name);
			complete(name, 'T' + name + ';');
		}

		@Override
		public SignatureVisitor visitArrayType() {
			ensureNotComplete();
			return new TypeVisitor(value -> {
				typeVariables.addAll(value.typeVariables());
				complete(value.sourceType() + "[]", value.erasedDescriptor() == null ? null : '[' + value.erasedDescriptor());
			});
		}

		@Override
		public void visitClassType(String name) {
			ensureNotComplete();
			ensureSafeInternalClassName(name);
			classType = true;
			internalName.append(name);
			source.append(ClassStubGenerator.cleanType(name));
		}

		@Override
		public void visitInnerClassType(String name) {
			ensureNotComplete();
			if (!classType)
				throw new IllegalArgumentException("Inner class without outer class");
			ensureSafeIdentifier(name);
			closeTypeArguments();
			internalName.append('$').append(name);
			source.append('.').append(name);
		}

		@Override
		public void visitTypeArgument() {
			startTypeArgument();
			source.append('?');
		}

		@Override
		public SignatureVisitor visitTypeArgument(char wildcard) {
			startTypeArgument();
			String prefix = switch (wildcard) {
				case SignatureVisitor.EXTENDS -> "? extends ";
				case SignatureVisitor.SUPER -> "? super ";
				case SignatureVisitor.INSTANCEOF -> "";
				default -> throw new IllegalArgumentException("Invalid wildcard: " + wildcard);
			};
			return new TypeVisitor(value -> {
				typeVariables.addAll(value.typeVariables());
				source.append(prefix).append(value.sourceType());
			});
		}

		@Override
		public void visitEnd() {
			ensureNotComplete();
			if (!classType)
				throw new IllegalArgumentException("End without a class type");
			closeTypeArguments();
			complete(source.toString(), "L" + internalName + ";");
		}

		private void startTypeArgument() {
			ensureNotComplete();
			if (!classType)
				throw new IllegalArgumentException("Type arguments without a class type");
			if (!typeArgumentsOpen) {
				source.append('<');
				typeArgumentsOpen = true;
			} else {
				source.append(", ");
			}
		}

		private void closeTypeArguments() {
			if (typeArgumentsOpen) {
				source.append('>');
				typeArgumentsOpen = false;
			}
		}

		private void ensureNotComplete() {
			if (completed)
				throw new IllegalArgumentException("Unexpected signature content after type completion");
		}

		private void complete(@Nonnull String sourceType, @Nullable String erasedDescriptor) {
			ensureNotComplete();
			completed = true;
			consumer.accept(new TypeSignature(sourceType, erasedDescriptor, Set.copyOf(typeVariables)));
		}
	}
}