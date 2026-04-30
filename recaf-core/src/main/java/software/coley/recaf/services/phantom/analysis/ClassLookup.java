package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Shared class/type lookup utility for phantom analysis stages.
 *
 * @author Matt Coley
 */
public class ClassLookup {
	private final Workspace workspace;
	private final Map<String, JvmClassInfo> providedClasses;
	private final Map<String, PhantomClassConstraint> constraints;

	/**
	 * @param workspace
	 * 		Workspace to pull classes from.
	 * @param providedClasses
	 * 		Input classes currently being analyzed.
	 * @param constraints
	 * 		Shared phantom constraints for the current analysis run.
	 */
	public ClassLookup(@Nonnull Workspace workspace,
	                   @Nonnull Map<String, JvmClassInfo> providedClasses,
	                   @Nonnull Map<String, PhantomClassConstraint> constraints) {
		this.workspace = workspace;
		this.providedClasses = new HashMap<>(providedClasses);
		this.constraints = constraints;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return {@code true} when the type is already known from the inputs or workspace.
	 */
	public boolean isKnown(@Nullable String internalName) {
		return getKnownClassInfo(internalName) != null;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Known class info, or {@code null} when it must be modeled as a phantom.
	 */
	@Nullable
	public ClassInfo getKnownClassInfo(@Nullable String internalName) {
		if (internalName == null)
			return null;

		// First check the provided input classes, then fall back to the workspace.
		JvmClassInfo provided = providedClasses.get(internalName);
		if (provided != null)
			return provided;
		ClassPathNode path = workspace.findClass(internalName);
		return path == null ? null : path.getValue();
	}

	/**
	 * @param internalName
	 * 		Class name.
	 *
	 * @return Kind inferred from known class info or collected phantom evidence.
	 */
	@Nonnull
	public PhantomTypeKind kindOf(@Nonnull String internalName) {
		// First check for any collected phantom evidence, then fall back to known class lookup.
		PhantomClassConstraint constraint = constraints.get(internalName);
		if (constraint != null) {
			if (constraint.isAnnotation())
				return PhantomTypeKind.ANNOTATION;
			if (constraint.isInterface())
				return PhantomTypeKind.INTERFACE;
			if (constraint.hasClassEvidence() || !constraint.getRequiredSupertypes().isEmpty())
				return PhantomTypeKind.CLASS;
		}

		// No phantom evidence, infer from known class info.
		ClassInfo info = getKnownClassInfo(internalName);
		if (info == null)
			return PhantomTypeKind.UNKNOWN;
		if (info.hasAnnotationModifier())
			return PhantomTypeKind.ANNOTATION;
		return info.hasInterfaceModifier() ? PhantomTypeKind.INTERFACE : PhantomTypeKind.CLASS;
	}

	/**
	 * @param child
	 * 		Potential subtype.
	 * @param target
	 * 		Potential supertype.
	 *
	 * @return {@code true} when {@code child} is a <i>strict</i> subtype of {@code target}.
	 */
	public boolean isStrictSubtypeOf(@Nullable String child, @Nullable String target) {
		return child != null
				&& target != null
				&& !child.equals(target) // strict subtyping means the types cannot be the same.
				&& isSubtypeOf(child, target, new HashSet<>());
	}

	private boolean isSubtypeOf(@Nullable String child,
	                            @Nullable String target,
	                            @Nonnull Set<String> visited) {
		// Base cases for null handling, equality, and cycle prevention.
		if (child == null || target == null)
			return false;
		if (child.equals(target))
			return true;
		if (!visited.add(child))
			return false;

		// Check if the child is a phantom with known constraints, and if so check its resolved supertype and interfaces.
		PhantomClassConstraint constraint = constraints.get(child);
		if (constraint != null) {
			if (isSubtypeOf(constraint.getResolvedSuperName(), target, visited))
				return true;
			for (String interfaceName : constraint.getResolvedInterfaces())
				if (isSubtypeOf(interfaceName, target, visited))
					return true;
			for (String requiredSupertype : constraint.getRequiredSupertypes())
				if (isSubtypeOf(requiredSupertype, target, visited))
					return true;
		}

		// If the child is not a phantom, check known class info for its supertype and interfaces.
		ClassInfo info = getKnownClassInfo(child);
		if (info == null)
			return false;
		if (isSubtypeOf(info.getSuperName(), target, visited))
			return true;
		for (String interfaceName : info.getInterfaces())
			if (isSubtypeOf(interfaceName, target, visited))
				return true;
		return false;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Existing or newly created phantom constraint, or {@code null} when the type is known.
	 */
	@Nullable
	public PhantomClassConstraint getOrCreateConstraint(@Nonnull String internalName) {
		if (internalName.startsWith("["))
			return null;
		if (isKnown(internalName))
			return null;
		return constraints.computeIfAbsent(internalName, PhantomClassConstraint::new);
	}

	/**
	 * Adds a missing class name as a phantom candidate.
	 *
	 * @param internalName
	 * 		Internal class name.
	 */
	public void collectInternalName(@Nullable String internalName) {
		if (internalName == null)
			return;
		getOrCreateConstraint(internalName);
	}

	/**
	 * Adds any missing referenced classes within the given type.
	 *
	 * @param type
	 * 		Type to inspect.
	 */
	public void collectType(@Nonnull Type type) {
		switch (type.getSort()) {
			case Type.ARRAY -> collectType(type.getElementType());
			case Type.OBJECT -> collectInternalName(type.getInternalName());
			case Type.METHOD -> {
				collectType(type.getReturnType());
				for (Type argumentType : type.getArgumentTypes())
					collectType(argumentType);
			}
			default -> {
				// Primitive type, nothing to do.
			}
		}
	}

	/**
	 * Adds any missing referenced types from a field descriptor.
	 *
	 * @param descriptor
	 * 		Field descriptor.
	 */
	public void collectDescriptor(@Nonnull String descriptor) {
		collectType(Type.getType(descriptor));
	}

	/**
	 * Adds any missing referenced types from a method descriptor.
	 *
	 * @param descriptor
	 * 		Method descriptor.
	 */
	public void collectMethodDescriptor(@Nonnull String descriptor) {
		collectType(Type.getMethodType(descriptor));
	}
}
