package software.coley.recaf.services.phantom.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.InnerClassInfo;
import software.coley.recaf.services.phantom.PhantomHint;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Aggregated requirements collected for a single phantom type.
 * <p>
 * Supertype requirements are kept in overlapping sets, by how far they can be trusted:
 * <ul>
 *     <li>{@link #getRequiredSupertypes() Required}: every supertype the type must have, from any source. Most come
 *     from approximate flow analysis, so not all of them may be satisfiable together. The other sets below are
 *     subsets of this one.</li>
 *     <li>{@link #getHardSupertypes() Hard}: structurally required, such as exception bases, enum bases, and sealed
 *     parents. These are never given up in favor of a flow-inferred supertype.</li>
 *     <li>{@link #getRequiredDirectSupertypes() Required direct}: hard supertypes that must be the direct parent,
 *     such as a sealed parent that permits this type.</li>
 *     <li>{@link #getHintedSupertypes() Hinted}: superclasses supplied by a {@link PhantomHint}.</li>
 * </ul>
 * Resolution then fills in the outcome: the {@link #getResolvedSuperName() resolved superclass}, the
 * {@link #getResolvedInterfaces() resolved interfaces}, and the {@link #getDroppedSupertypes() dropped superclasses}
 * that were required but not chosen. Members of dropped superclasses are copied into the type.
 *
 * @author Matt Coley
 */
public class PhantomClassConstraint {
	private final String name;
	private final Map<String, PhantomFieldRequirement> fields = new TreeMap<>();
	private final Map<String, PhantomMethodRequirement> methods = new TreeMap<>();
	private final Map<String, PhantomInnerRequirement> declaredInners = new TreeMap<>();
	private final Set<String> requiredSupertypes = new TreeSet<>();
	private final Set<String> requiredDirectSupertypes = new TreeSet<>();
	private final Set<String> hardSupertypes = new TreeSet<>();
	private final Set<String> hintedSupertypes = new TreeSet<>();
	private final Set<String> droppedSupertypes = new TreeSet<>();
	private final Set<String> resolvedInterfaces = new TreeSet<>();
	private boolean interfaceEvidence;
	private boolean classEvidence;
	private boolean annotationEvidence;
	private boolean enumEvidence;
	private boolean runtimeVisibleAnnotationEvidence;
	private final Set<String> enumConstants = new TreeSet<>();
	private int genericParameterCount;
	private String outerName;
	private String innerSimpleName;
	private int innerClassAccess;
	private final Map<String, Integer> declaredInnerAccess = new TreeMap<>();
	private String resolvedSuperName = "java/lang/Object";

	/**
	 * @param name
	 * 		Class name.
	 */
	public PhantomClassConstraint(@Nonnull String name) {
		this.name = name;
	}

	/**
	 * @return Resolved superclass internal name.
	 */
	@Nonnull
	public String getResolvedSuperName() {
		return resolvedSuperName;
	}

	/**
	 * @param resolvedSuperName
	 * 		Resolved superclass internal name.
	 */
	public void setResolvedSuperName(@Nonnull String resolvedSuperName) {
		this.resolvedSuperName = Objects.requireNonNull(resolvedSuperName);
	}

	/**
	 * @return Class name.
	 */
	@Nonnull
	public String getName() {
		return name;
	}

	/**
	 * @return Collected field requirements.
	 */
	@Nonnull
	public Collection<PhantomFieldRequirement> getFieldRequirements() {
		return Collections.unmodifiableCollection(fields.values());
	}

	/**
	 * @return Collected method requirements.
	 */
	@Nonnull
	public Collection<PhantomMethodRequirement> getMethodRequirements() {
		return Collections.unmodifiableCollection(methods.values());
	}

	/**
	 * @return Collected inner class requirements.
	 */
	@Nonnull
	public Collection<PhantomInnerRequirement> getDeclaredInners() {
		return Collections.unmodifiableCollection(declaredInners.values());
	}

	/**
	 * @return Required supertypes inferred from use sites.
	 */
	@Nonnull
	public Set<String> getRequiredSupertypes() {
		return Collections.unmodifiableSet(requiredSupertypes);
	}

	/**
	 * @return Supertypes that are structurally required, such as exception bases, enum bases, and sealed parents.
	 * Unlike {@link #getRequiredSupertypes()}, these are not inferred from approximate flow analysis.
	 */
	@Nonnull
	public Set<String> getHardSupertypes() {
		return Collections.unmodifiableSet(hardSupertypes);
	}

	/**
	 * @return Superclasses supplied by external evidence, such as a compiler diagnostic. These are kept by the
	 * hierarchy solver rather than treated as flow-inferred guesses.
	 */
	@Nonnull
	public Set<String> getHintedSupertypes() {
		return Collections.unmodifiableSet(hintedSupertypes);
	}

	/**
	 * @return Supertypes that must be direct parents, such as sealed permitted parents.
	 */
	@Nonnull
	public Set<String> getRequiredDirectSupertypes() {
		return Collections.unmodifiableSet(requiredDirectSupertypes);
	}

	/**
	 * @return Superclasses not chosen as the parent, whose members are copied into this type.
	 */
	@Nonnull
	public Set<String> getDroppedSupertypes() {
		return Collections.unmodifiableSet(droppedSupertypes);
	}

	/**
	 * @return Resolved interface names.
	 */
	@Nonnull
	public Set<String> getResolvedInterfaces() {
		return Collections.unmodifiableSet(resolvedInterfaces);
	}

	/**
	 * @return {@code true} when some usage implies this class is an interface.
	 */
	public boolean hasInterfaceEvidence() {
		return interfaceEvidence;
	}

	/**
	 * @return {@code true} when some usage implies this class is a standard class.
	 */
	public boolean hasClassEvidence() {
		return classEvidence;
	}

	/**
	 * @return {@code true} when some usage implies this class is an annotation interface.
	 */
	public boolean hasAnnotationEvidence() {
		return annotationEvidence;
	}

	/**
	 * @return Outer class name, if evidence was collected that this is an inner class, otherwise {@code null}.
	 */
	@Nullable
	public String getOuterName() {
		return outerName;
	}

	/**
	 * @return Simple inner class name, if evidence was collected that this is an inner class, otherwise {@code null}.
	 */
	@Nullable
	public String getInnerSimpleName() {
		return innerSimpleName;
	}

	/**
	 * @return Access flags observed for this type's inner-class entry.
	 */
	public int getInnerClassAccess() {
		return innerClassAccess;
	}

	/**
	 * @param innerName
	 * 		Inner class internal name.
	 *
	 * @return Access flags observed for the declared inner class, or {@code 0} when unavailable.
	 */
	public int getDeclaredInnerAccess(@Nonnull String innerName) {
		return declaredInnerAccess.getOrDefault(innerName, 0);
	}

	/**
	 * Marks the type as an interface.
	 */
	public void markInterface() {
		interfaceEvidence = true;
	}

	/**
	 * Marks the type as a class.
	 */
	public void markClass() {
		classEvidence = true;
	}

	/**
	 * Marks the type as an annotation.
	 *
	 * @param visible
	 * 		Whether runtime-visible annotation use was observed.
	 */
	public void markAnnotation(boolean visible) {
		annotationEvidence = true;
		interfaceEvidence = true;
		runtimeVisibleAnnotationEvidence |= visible;
	}

	/**
	 * Marks the type as an enum.
	 */
	public void markEnum() {
		enumEvidence = true;
		classEvidence = true;
		addHardSupertype("java/lang/Enum");
	}

	/**
	 * @param name
	 * 		Observed enum constant name.
	 */
	public void addEnumConstant(@Nonnull String name) {
		markEnum();
		enumConstants.add(name);
	}

	/**
	 * @return {@code true} when the constraint should be emitted as an annotation.
	 */
	public boolean isAnnotation() {
		return annotationEvidence && !classEvidence;
	}

	/**
	 * @return {@code true} when the constraint should be emitted as an enum.
	 */
	public boolean isEnum() {
		return enumEvidence && !annotationEvidence;
	}

	/**
	 * @return Observed enum constant names.
	 */
	@Nonnull
	public Set<String> getEnumConstants() {
		return Collections.unmodifiableSet(enumConstants);
	}

	/**
	 * @return {@code true} when the constraint should be emitted as an interface.
	 */
	public boolean isInterface() {
		return isAnnotation() || (!isEnum() && interfaceEvidence && !classEvidence);
	}

	/**
	 * @return {@code true} when runtime-visible annotation evidence was observed.
	 */
	public boolean hasRuntimeVisibleAnnotationEvidence() {
		return runtimeVisibleAnnotationEvidence;
	}

	/**
	 * @return Number of generic type parameters required by observed signatures.
	 */
	public int getGenericParameterCount() {
		return genericParameterCount;
	}

	/**
	 * Records generic arity inferred from a JVM signature.
	 *
	 * @param count
	 *		Number of type arguments observed.
	 */
	public void markGenericParameterCount(int count) {
		if (count > genericParameterCount)
			genericParameterCount = count;
	}

	/**
	 * @param internalName
	 * 		Supertype name inferred from usage.
	 */
	public void addRequiredSupertype(@Nonnull String internalName) {
		requiredSupertypes.add(internalName);
	}

	/**
	 * @param internalName
	 * 		Supertype that must be a direct parent.
	 */
	public void addRequiredDirectSupertype(@Nonnull String internalName) {
		requiredDirectSupertypes.add(internalName);
		addHardSupertype(internalName);
	}

	/**
	 * @param internalName
	 * 		Internal class name of the required superclass.
	 */
	public void addHintedSupertype(@Nonnull String internalName) {
		hintedSupertypes.add(internalName);
		addRequiredSupertype(internalName);
	}

	/**
	 * @param internalName
	 * 		Internal class name of the required supertype.
	 */
	public void addHardSupertype(@Nonnull String internalName) {
		hardSupertypes.add(internalName);
		addRequiredSupertype(internalName);
	}

	/**
	 * @param fieldName
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param isStatic
	 * 		Whether the field must be static.
	 */
	public void addField(@Nonnull String fieldName, @Nonnull String descriptor, boolean isStatic) {
		PhantomFieldRequirement field = fields.computeIfAbsent(fieldName + descriptor,
				key -> new PhantomFieldRequirement(fieldName, descriptor, isStatic));
		if (isStatic)
			field.markStatic();

		// Interfaces can't have instance fields.
		if (!field.isStatic())
			classEvidence = true;
	}

	/**
	 * Declares a field, replacing any earlier requirement for a field with the same name. Used when a later hint states
	 * the field's type more precisely.
	 *
	 * @param fieldName
	 * 		Field name.
	 * @param descriptor
	 * 		Field descriptor.
	 * @param isStatic
	 * 		Whether the field must be static.
	 */
	public void replaceField(@Nonnull String fieldName, @Nonnull String descriptor, boolean isStatic) {
		fields.values().removeIf(field -> field.getName().equals(fieldName));
		addField(fieldName, descriptor, isStatic);
	}

	/**
	 * Declares a method, replacing any earlier requirement with the same name and parameters. Used when a later hint
	 * states the method's return type more precisely.
	 *
	 * @param methodName
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param isStatic
	 * 		Whether the method must be static.
	 */
	public void replaceMethod(@Nonnull String methodName, @Nonnull String descriptor, boolean isStatic) {
		String parameters = descriptor.substring(0, descriptor.indexOf(')') + 1);
		Set<String> kept = new TreeSet<>();
		methods.values().removeIf(method -> {
			boolean replaced = method.getName().equals(methodName) && method.getDescriptor().startsWith(parameters);
			if (replaced)
				kept.addAll(method.getExceptions());
			return replaced;
		});
		addMethod(methodName, descriptor, isStatic);
		for (String exception : kept)
			addMethodException(methodName, descriptor, exception);
	}

	/**
	 * @param methodName
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor of a method already declared on this type.
	 * @param exception
	 * 		Internal name of an exception the method declares.
	 */
	public void addMethodException(@Nonnull String methodName, @Nonnull String descriptor, @Nonnull String exception) {
		PhantomMethodRequirement method = methods.get(methodName + descriptor);
		if (method != null)
			method.addException(exception);
	}

	/**
	 * @param methodName
	 * 		Method name.
	 * @param descriptor
	 * 		Method descriptor.
	 * @param isStatic
	 * 		Whether the method must be static.
	 */
	public void addMethod(@Nonnull String methodName, @Nonnull String descriptor, boolean isStatic) {
		PhantomMethodRequirement method = methods.computeIfAbsent(methodName + descriptor,
				key -> new PhantomMethodRequirement(methodName, descriptor, isStatic));
		if (isStatic)
			method.markStatic();

		// Interfaces can't have constructors.
		if ("<init>".equals(methodName))
			classEvidence = true;
	}

	/**
	 * @param elementName
	 * 		Annotation element name.
	 * @param descriptor
	 * 		Annotation element descriptor.
	 */
	public void addAnnotationElement(@Nonnull String elementName, @Nonnull String descriptor) {
		markAnnotation(false);
		addMethod(elementName, "()" + descriptor, false);
	}

	/**
	 * @param outerName
	 * 		Full outer class name.
	 * @param innerSimpleName
	 * 		Simple inner class name.
	 *
	 * @see InnerClassInfo#getOuterClassName()
	 * @see InnerClassInfo#getInnerClassName()
	 */
	public void markInnerClassOf(@Nonnull String outerName, @Nonnull String innerSimpleName) {
		markInnerClassOf(outerName, innerSimpleName, 0);
	}

	/**
	 * @param outerName
	 * 		Full outer class name.
	 * @param innerSimpleName
	 * 		Simple inner class name.
	 * @param access
	 * 		Access flags from the inner-class entry.
	 */
	public void markInnerClassOf(@Nonnull String outerName, @Nonnull String innerSimpleName, int access) {
		this.outerName = outerName;
		this.innerSimpleName = innerSimpleName;
		if (access != 0 || innerClassAccess == 0)
			this.innerClassAccess = access;
	}

	/**
	 * @param innerName
	 * 		Full inner class name.
	 * @param innerSimpleName
	 * 		Simple inner class name.
	 *
	 * @see InnerClassInfo#getInnerName()
	 * @see InnerClassInfo#getInnerClassName()
	 */
	public void addDeclaredInner(@Nonnull String innerName, @Nonnull String innerSimpleName) {
		addDeclaredInner(innerName, innerSimpleName, 0);
	}

	/**
	 * @param innerName
	 * 		Full inner class name.
	 * @param innerSimpleName
	 * 		Simple inner class name.
	 * @param access
	 * 		Access flags from the inner-class entry.
	 */
	public void addDeclaredInner(@Nonnull String innerName, @Nonnull String innerSimpleName, int access) {
		declaredInners.put(innerName, new PhantomInnerRequirement(innerName, innerSimpleName));
		if (access != 0 || !declaredInnerAccess.containsKey(innerName))
			declaredInnerAccess.put(innerName, access);
	}

	/**
	 * Clears the dropped superclasses.
	 */
	public void clearDroppedSupertypes() {
		droppedSupertypes.clear();
	}

	/**
	 * @param internalName
	 * 		Dropped superclass name.
	 */
	public void addDroppedSupertype(@Nonnull String internalName) {
		droppedSupertypes.add(internalName);
	}

	/**
	 * Replaces the current dropped supertypes.
	 *
	 * @param supertypes
	 * 		New dropped supertypes.
	 */
	public void setDroppedSupertypes(@Nonnull Collection<String> supertypes) {
		droppedSupertypes.clear();
		droppedSupertypes.addAll(supertypes);
	}

	/**
	 * Clears the resolved interfaces.
	 */
	public void clearResolvedInterfaces() {
		resolvedInterfaces.clear();
	}

	/**
	 * @param interfaceName
	 * 		Resolved interface name.
	 */
	public void addResolvedInterface(@Nonnull String interfaceName) {
		resolvedInterfaces.add(interfaceName);
	}
}
