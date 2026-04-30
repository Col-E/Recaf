package software.coley.recaf.services.phantom.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.InnerClassInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregated requirements collected for a single phantom type.
 *
 * @author Matt Coley
 */
public class PhantomClassConstraint {
	private final String name;
	private final Map<String, PhantomFieldRequirement> fields = new HashMap<>();
	private final Map<String, PhantomMethodRequirement> methods = new HashMap<>();
	private final Map<String, PhantomInnerRequirement> declaredInners = new HashMap<>();
	private final Set<String> requiredSupertypes = new HashSet<>();
	private final Set<String> droppedSupertypes = new HashSet<>();
	private final Set<String> resolvedInterfaces = new HashSet<>();
	private boolean interfaceEvidence;
	private boolean classEvidence;
	private boolean annotationEvidence;
	private boolean runtimeVisibleAnnotationEvidence;
	private String outerName;
	private String innerSimpleName;
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
	 * @return Dropped supertypes retained for lenient hierarchy completion.
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
	 * @return {@code true} when the constraint should be emitted as an annotation.
	 */
	public boolean isAnnotation() {
		return annotationEvidence && !classEvidence;
	}

	/**
	 * @return {@code true} when the constraint should be emitted as an interface.
	 */
	public boolean isInterface() {
		return isAnnotation() || (interfaceEvidence && !classEvidence);
	}

	/**
	 * @return {@code true} when runtime-visible annotation evidence was observed.
	 */
	public boolean hasRuntimeVisibleAnnotationEvidence() {
		return runtimeVisibleAnnotationEvidence;
	}

	/**
	 * @param internalName
	 * 		Supertype name inferred from usage.
	 */
	public void addRequiredSupertype(@Nonnull String internalName) {
		requiredSupertypes.add(internalName);
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
		this.outerName = outerName;
		this.innerSimpleName = innerSimpleName;
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
		declaredInners.put(innerName, new PhantomInnerRequirement(innerName, innerSimpleName));
	}

	/**
	 * Clears the lenient hierarchy leftovers.
	 */
	public void clearDroppedSupertypes() {
		droppedSupertypes.clear();
	}

	/**
	 * @param internalName
	 * 		Leniently dropped supertype name.
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
