package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.objectweb.asm.Type;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.workspace.model.Workspace;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared state for phantom generation analysis passes.
 *
 * @author Matt Coley
 */
public class PhantomGenerationContext {
	private final ClassLookup lookup;
	private final Map<String, PhantomClassConstraint> constraints = new HashMap<>();
	private final boolean lenientConflictingHierarchies;

	/**
	 * @param workspace
	 * 		Workspace to pull values from.
	 * @param inputClasses
	 * 		Classes being analyzed.
	 * @param lenientConflictingHierarchies
	 * 		Whether incompatible class supertype candidates should be chained leniently.
	 */
	public PhantomGenerationContext(@Nonnull Workspace workspace,
	                                @Nonnull Map<String, JvmClassInfo> inputClasses,
	                                boolean lenientConflictingHierarchies) {
		this.lenientConflictingHierarchies = lenientConflictingHierarchies;
		lookup = new ClassLookup(workspace, inputClasses, constraints);
	}

	/**
	 * @return Shared lookup helper.
	 */
	@Nonnull
	public ClassLookup getLookup() {
		return lookup;
	}

	/**
	 * @return Shared mutable phantom constraint map.
	 */
	@Nonnull
	public Map<String, PhantomClassConstraint> getConstraints() {
		return constraints;
	}

	/**
	 * @return {@code true} when hierarchy conflicts should be resolved leniently.
	 */
	public boolean isLenientConflictingHierarchies() {
		return lenientConflictingHierarchies;
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Existing or newly created phantom constraint, or {@code null} when the type is known.
	 */
	@Nullable
	public PhantomClassConstraint getOrCreateConstraint(@Nonnull String internalName) {
		return lookup.getOrCreateConstraint(internalName);
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return {@code true} when the type is already known from the inputs or workspace.
	 */
	public boolean isKnown(@Nullable String internalName) {
		return lookup.isKnown(internalName);
	}

	/**
	 * @param internalName
	 * 		Internal class name.
	 *
	 * @return Kind inferred from known class info or collected phantom evidence.
	 */
	@Nonnull
	public PhantomTypeKind kindOf(@Nonnull String internalName) {
		return lookup.kindOf(internalName);
	}

	/**
	 * @param child
	 * 		Potential subtype.
	 * @param target
	 * 		Potential supertype.
	 *
	 * @return {@code true} when {@code child} is a strict subtype of {@code target}.
	 */
	public boolean isStrictSubtypeOf(@Nullable String child, @Nullable String target) {
		return lookup.isStrictSubtypeOf(child, target);
	}

	/**
	 * Adds a missing internal name as a phantom candidate.
	 *
	 * @param internalName
	 * 		Internal class name.
	 */
	public void collectInternalName(@Nullable String internalName) {
		lookup.collectInternalName(internalName);
	}

	/**
	 * Adds any missing referenced types from a field descriptor.
	 *
	 * @param descriptor
	 * 		Type descriptor.
	 */
	public void collectDescriptor(@Nonnull String descriptor) {
		lookup.collectDescriptor(descriptor);
	}

	/**
	 * Adds any missing referenced types from a method descriptor.
	 *
	 * @param descriptor
	 * 		Method descriptor.
	 */
	public void collectMethodDescriptor(@Nonnull String descriptor) {
		lookup.collectMethodDescriptor(descriptor);
	}

	/**
	 * Adds any missing referenced types within the given type.
	 *
	 * @param type
	 * 		Type to inspect.
	 */
	public void collectType(@Nonnull Type type) {
		lookup.collectType(type);
	}
}
