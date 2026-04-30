package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.phantom.PhantomGenerationException;
import software.coley.recaf.services.phantom.PhantomGeneratorConfig;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.services.phantom.model.PhantomFieldRequirement;
import software.coley.recaf.services.phantom.model.PhantomMethodRequirement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the phantom constraints into a type hierarchy.
 *
 * @author Matt Coley
 */
public class PhantomHierarchyResolver {
	private final PhantomGenerationContext context;
	private final ClassLookup lookup;
	private final Map<String, PhantomClassConstraint> constraints;

	/**
	 * @param context
	 * 		Phantom analysis context.
	 */
	public PhantomHierarchyResolver(@Nonnull PhantomGenerationContext context) {
		this.context = context;

		lookup = context.getLookup();
		constraints = context.getConstraints();
	}

	/**
	 * Resolves nested-class metadata, supertypes, lenient hierarchy bridges, and inherited methods.
	 *
	 * @throws PhantomGenerationException
	 * 		When incompatible hierarchy requirements cannot be resolved.
	 */
	public void resolve() throws PhantomGenerationException {
		// Fill in inner/outer class relations for nested classes.
		linkNestedClasses();

		// Resolve supertype constraints for all phantom classes.
		// Drop redundant interfaces based on selected parent types.
		resolveSubtypeConstraints();

		// Select inheritance order for leniently chained phantom super-type hierarchies.
		arrangeLenientHierarchyChains();

		// Ensure all class constraints declare members from their dropped lenient supertypes.
		completeLenientHierarchyMembers();

		// Ensure all class constraints declare members from super-types and implemented interfaces.
		completeInheritedMethods();
	}

	/**
	 * Fill in inner/outer class relations for nested classes.
	 */
	private void linkNestedClasses() {
		Set<String> visited = new HashSet<>();
		for (PhantomClassConstraint constraint : Set.copyOf(constraints.values()))
			linkNestedClass(constraint, visited);
	}

	/**
	 * Fill in inner/outer class relations for nested classes.
	 *
	 * @param constraint
	 * 		Constraint to check for nesting.
	 * @param visited
	 * 		Visited set of class names.
	 */
	private void linkNestedClass(@Nonnull PhantomClassConstraint constraint, @Nonnull Set<String> visited) {
		if (!visited.add(constraint.getName()))
			return;

		// Check if we have 'Outer$Inner' style naming.
		int separator = constraint.getName().lastIndexOf('$');
		if (separator <= 0)
			return;

		// We do. Split the outer and inner names and link them together.
		String outerName = constraint.getName().substring(0, separator);
		String innerSimpleName = constraint.getName().substring(separator + 1);
		constraint.markInnerClassOf(outerName, innerSimpleName);
		PhantomClassConstraint outerConstraint = context.getOrCreateConstraint(outerName);
		if (outerConstraint != null) {
			outerConstraint.addDeclaredInner(constraint.getName(), innerSimpleName);
			linkNestedClass(outerConstraint, visited);
		}
	}

	/**
	 * Resolves the supertype constraints for each phantom class,
	 * picking a single parent class and filtering out redundant interfaces.
	 *
	 * @throws PhantomGenerationException
	 * 		When incompatible hierarchy requirements cannot be resolved.
	 */
	private void resolveSubtypeConstraints() throws PhantomGenerationException {
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Compute super/interface parents of the phantom class.
			Set<String> classCandidates = new HashSet<>();
			Set<String> interfaceCandidates = new HashSet<>();
			for (String candidate : constraint.getRequiredSupertypes()) {
				if (constraint.getName().equals(candidate))
					continue;
				switch (context.kindOf(candidate)) {
					case INTERFACE, ANNOTATION -> interfaceCandidates.add(candidate);
					case CLASS, UNKNOWN -> classCandidates.add(candidate);
				}
			}

			// Remove redundant candidates, leaving only the most specific parents.
			removeRedundantCandidates(classCandidates);
			removeRedundantCandidates(interfaceCandidates);

			// Pick a single parent class.
			// If there are multiple candidates, we have a hierarchy conflict, which we'll naively ignore for lenient mode.
			String chosenSuperName = classCandidates.isEmpty() ? "java/lang/Object" : classCandidates.iterator().next();
			if (classCandidates.size() > 1) {
				// If we're not lenient, then we cannot prove which candidate is correct, so we have to fail.
				if (!context.isLenientConflictingHierarchies()) {
					throw new PhantomGenerationException("Unsatisfiable phantom hierarchy for " +
							constraint.getName() + ": " + classCandidates);
				}

				// But if we are lenient just pick whatever is first.
				// The rest will be added as "dropped supertypes" and chained together later if possible.
				List<String> orderedCandidates = orderLenientClassCandidates(classCandidates);
				chosenSuperName = orderedCandidates.getFirst();
				constraint.setDroppedSupertypes(orderedCandidates.subList(1, orderedCandidates.size()));
			} else {
				// Clear any previously inferred dropped supertypes since we have a single clear parent now.
				constraint.clearDroppedSupertypes();
			}
			constraint.setResolvedSuperName(constraint.isInterface() ? "java/lang/Object" : chosenSuperName);

			// Based on the chosen parent class, filter out any interface candidates that are
			// actually supertypes of the selected parent, since those would be redundant to implement.
			constraint.clearResolvedInterfaces();
			if (constraint.isAnnotation())
				constraint.addResolvedInterface("java/lang/annotation/Annotation");
			for (String interfaceName : interfaceCandidates)
				if (!context.isStrictSubtypeOf(constraint.getResolvedSuperName(), interfaceName))
					constraint.addResolvedInterface(interfaceName);
		}
	}

	/**
	 * Orders class candidates for lenient hierarchy resolution, putting rewritable phantom classes first so they can be chained together.
	 *
	 * @param classCandidates
	 * 		Set of candidate class supertypes.
	 *
	 * @return Ordered list of class candidates, with rewritable phantom classes first.
	 */
	@Nonnull
	private List<String> orderLenientClassCandidates(@Nonnull Set<String> classCandidates) {
		List<String> ordered = new ArrayList<>(classCandidates.size());
		classCandidates.stream()
				.filter(this::isRewritablePhantomClass)
				.forEach(ordered::add);
		classCandidates.stream()
				.filter(candidate -> !ordered.contains(candidate))
				.forEach(ordered::add);
		return ordered;
	}

	/**
	 * Determines if a phantom class's type hierarchy can be treated as rewritable.
	 *
	 * @param internalName
	 * 		Phantom class name to check.
	 *
	 * @return {@code true} if the phantom class's hierarchy can be rewritten.
	 *
	 * @see PhantomGeneratorConfig#getLenientConflictingHierarchies()
	 */
	private boolean isRewritablePhantomClass(@Nonnull String internalName) {
		if (lookup.isKnown(internalName))
			return false;

		// A phantom class has a rewritable type hierarchy if it's not a known class from the workspace
		// and not constrained to be an interface or annotation, since those cannot be chained as super types.
		PhantomClassConstraint constraint = constraints.get(internalName);
		return constraint != null
				&& !constraint.isInterface()
				&& !constraint.isAnnotation();
	}

	/**
	 * Select inheritance order for leniently chained phantom super-type hierarchies.
	 */
	private void arrangeLenientHierarchyChains() {
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Interfaces don't have supertype chains.
			if (constraint.isInterface())
				continue;

			// Skip if there are no inferred supertypes to chain together.
			if (constraint.getDroppedSupertypes().isEmpty())
				continue;

			// When multiple class super-types are inferred, rewrite phantom parents into a chain when possible.
			//
			// The idea is that if we have: A extends [B or C] we can make it either:
			//  - A extends B extends C
			//  - A extends C extends B
			//
			// In both of these cases what actually was the original order doesn't matter.
			// Compilation contracts shouldn't be able to tell the difference between the two.
			Set<String> unresolved = new HashSet<>();
			String chainHead = constraint.getResolvedSuperName();
			for (String droppedSupertype : List.copyOf(constraint.getDroppedSupertypes())) {
				String linkedHead = ensureSubtypeChain(chainHead, droppedSupertype);
				if (linkedHead == null)
					unresolved.add(droppedSupertype);
				else
					chainHead = linkedHead;
			}
			constraint.setDroppedSupertypes(unresolved);
		}
	}

	/**
	 * @param current
	 * 		Current type in the chain. {@code null} if we are at the end of the chain.
	 * @param requiredSupertype
	 * 		Required supertype to link into the chain.
	 *
	 * @return The new head of the chain if linking was successful,
	 * or {@code null} if the types are incompatible and cannot be linked.
	 */
	@Nullable
	private String ensureSubtypeChain(@Nullable String current, @Nonnull String requiredSupertype) {
		// Base cases for null handling, equality, and direct subtyping.
		if (current == null || current.equals(requiredSupertype))
			return requiredSupertype;
		if (context.isStrictSubtypeOf(current, requiredSupertype))
			return requiredSupertype;
		if (context.isStrictSubtypeOf(requiredSupertype, current))
			return null;

		// If we don't have phantom info for the current type, we can't rewrite it.
		PhantomClassConstraint currentConstraint = constraints.get(current);
		if (currentConstraint == null)
			return null;

		// If the current type is a phantom but an interface it's not subject to class hierarchy chaining.
		if (currentConstraint.isInterface() || currentConstraint.isAnnotation())
			return null;

		// If the current type is a phantom but also somehow a known class from the workspace, we also can't rewrite it.
		// This shouldn't happen, but just in case...
		if (lookup.isKnown(current))
			return null;

		// Make a note of the previous supertype before we overwrite it, so we can check for compatibility and drop it if needed.
		String previousSuper = currentConstraint.getResolvedSuperName();
		currentConstraint.setResolvedSuperName(requiredSupertype);

		// If the previous supertype is not compatible with the new required supertype, then we have to drop it from the hierarchy.
		if (!"java/lang/Object".equals(previousSuper)
				&& !previousSuper.equals(requiredSupertype)
				&& !context.isStrictSubtypeOf(requiredSupertype, previousSuper)) {
			String chainedPrevious = ensureSubtypeChain(requiredSupertype, previousSuper);
			if (chainedPrevious == null)
				currentConstraint.addDroppedSupertype(previousSuper);
		}
		return requiredSupertype;
	}

	/**
	 * Ensures all class constraints declare members from their dropped lenient supertypes.
	 */
	private void completeLenientHierarchyMembers() {
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Interfaces only need their declared methods, so there's no need to copy members from dropped supertypes.
			if (constraint.isInterface())
				continue;

			// Skip constraints that did not lose any class supertypes during lenient resolution.
			if (constraint.getDroppedSupertypes().isEmpty())
				continue;

			// For each dropped supertype, copy inheritable instance members into the target constraint to
			// facilitate compilation contracts matching what we already got in the analyzed classes.
			for (String droppedSupertype : constraint.getDroppedSupertypes())
				collectLenientMembersFromType(droppedSupertype, constraint, new HashSet<>());
		}
	}

	/**
	 * Recursively copies inheritable instance members from a dropped lenient supertype into the target constraint.
	 *
	 * @param typeName
	 * 		Type to collect members from.
	 * @param targetConstraint
	 * 		Constraint receiving copied members.
	 * @param visited
	 * 		Classes already visited in this collection.
	 */
	private void collectLenientMembersFromType(@Nullable String typeName,
	                                           @Nonnull PhantomClassConstraint targetConstraint,
	                                           @Nonnull Set<String> visited) {
		// Skip if we've already visited this type, or if it's Object since that has no members to inherit.
		if (typeName == null || !visited.add(typeName) || "java/lang/Object".equals(typeName))
			return;

		// Skip if the target constraint is already a subtype of this type.
		if (context.isStrictSubtypeOf(targetConstraint.getResolvedSuperName(), typeName))
			return;

		// Skip if the target constraint is already a subtype of this type's interfaces.
		for (String interfaceName : targetConstraint.getResolvedInterfaces())
			if (context.isStrictSubtypeOf(interfaceName, typeName) || interfaceName.equals(typeName))
				return;

		// Visit parent types before this one.
		// That way inherited members are added first, and members declared here can replace them.
		//
		// Example:
		// - Parent defines: run()V
		// - Child defines:  run()V
		//
		// We visit Parent first, then Child, so the Child version is kept.
		PhantomClassConstraint phantom = constraints.get(typeName);
		if (phantom != null) {
			collectLenientMembersFromType(phantom.getResolvedSuperName(), targetConstraint, visited);
			for (String interfaceName : phantom.getResolvedInterfaces())
				collectLenientMembersFromType(interfaceName, targetConstraint, visited);

			// Only copy inheritable instance members.
			// Static members and constructors do not participate in the compile-time contracts we're trying to preserve here.
			for (PhantomFieldRequirement field : phantom.getFieldRequirements())
				if (!field.isStatic())
					targetConstraint.addField(field.getName(), field.getDescriptor(), false);
			for (PhantomMethodRequirement method : phantom.getMethodRequirements())
				if (!method.isStatic() && !method.isConstructor())
					targetConstraint.addMethod(method.getName(), method.getDescriptor(), false);

			return;
		}

		// If we don't have phantom info for this type, try to look it up as a known class and collect members from there.
		ClassInfo info = lookup.getKnownClassInfo(typeName);
		if (info == null)
			return;

		// Walking parents again.
		collectLenientMembersFromType(info.getSuperName(), targetConstraint, visited);
		for (String interfaceName : info.getInterfaces())
			collectLenientMembersFromType(interfaceName, targetConstraint, visited);

		// Same as the above, but for known classes.
		info.getFields().stream()
				.filter(field -> !field.hasStaticModifier() && !field.hasPrivateModifier())
				.forEach(field -> targetConstraint.addField(field.getName(), field.getDescriptor(), false));
		info.getMethods().stream()
				.filter(method -> !method.hasStaticModifier() && !method.hasPrivateModifier())
				.filter(method -> !"<init>".equals(method.getName()) && !"<clinit>".equals(method.getName()))
				.forEach(method -> targetConstraint.addMethod(method.getName(), method.getDescriptor(), false));
	}

	/**
	 * Removes redundant candidates from the set, leaving only the most specific supertypes.
	 *
	 * @param candidates
	 * 		Set of candidate supertypes to filter.
	 */
	private void removeRedundantCandidates(@Nonnull Set<String> candidates) {
		List<String> snapshot = List.copyOf(candidates);
		for (String candidate : snapshot) {
			for (String other : snapshot) {
				if (candidate.equals(other))
					continue;
				if (context.isStrictSubtypeOf(other, candidate)) {
					candidates.remove(candidate);
					break;
				}
			}
		}
	}

	/**
	 * Ensures all class constraints declare members from super-types and implemented interfaces.
	 */
	private void completeInheritedMethods() {
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Interfaces already model abstract declarations directly and do not need bridge stubs here.
			if (constraint.isInterface())
				continue;

			Map<String, PhantomMethodRequirement> requiredMethods = new HashMap<>();
			Set<String> visited = new HashSet<>();
			collectRequiredAbstractMethods(constraint.getResolvedSuperName(), requiredMethods, visited);
			for (String interfaceName : constraint.getResolvedInterfaces())
				collectRequiredAbstractMethods(interfaceName, requiredMethods, visited);

			for (PhantomMethodRequirement method : requiredMethods.values())
				constraint.addMethod(method.getName(), method.getDescriptor(), false);
		}
	}

	/**
	 * Recursively collects abstract instance methods that the target concrete phantom must implement.
	 *
	 * @param typeName
	 * 		Type to inspect for abstract-method obligations.
	 * @param requiredMethods
	 * 		Map of currently required methods, keyed by name and descriptor.
	 * @param visited
	 * 		Classes already visited in this collection.
	 */
	private void collectRequiredAbstractMethods(@Nullable String typeName,
	                                            @Nonnull Map<String, PhantomMethodRequirement> requiredMethods,
	                                            @Nonnull Set<String> visited) {
		if (typeName == null || !visited.add(typeName) || "java/lang/Object".equals(typeName))
			return;

		// Visit parent types before this one.
		// That way inherited members are added first, and members declared here can replace them.
		//
		// Example:
		// - Parent defines: run()V
		// - Child defines:  run()V
		//
		// We visit Parent first, then Child, so the Child version is kept.
		PhantomClassConstraint phantom = constraints.get(typeName);
		if (phantom != null) {
			collectRequiredAbstractMethods(phantom.getResolvedSuperName(), requiredMethods, visited);
			for (String interfaceName : phantom.getResolvedInterfaces())
				collectRequiredAbstractMethods(interfaceName, requiredMethods, visited);

			// Only copy inheritable instance members.
			// Static members and constructors do not participate in the compile-time contracts we're trying to preserve here.
			for (PhantomMethodRequirement method : phantom.getMethodRequirements()) {
				if (method.isConstructor() || method.isStatic())
					continue;
				if (phantom.isInterface())
					requiredMethods.putIfAbsent(method.key(), new PhantomMethodRequirement(method.getName(), method.getDescriptor(), false));
				else
					requiredMethods.remove(method.key());
			}
			return;
		}

		// If we don't have phantom info for this type, try to look it up as a known class and collect members from there.
		ClassInfo info = lookup.getKnownClassInfo(typeName);
		if (info == null)
			return;

		// Walking parents again.
		collectRequiredAbstractMethods(info.getSuperName(), requiredMethods, visited);
		for (String interfaceName : info.getInterfaces())
			collectRequiredAbstractMethods(interfaceName, requiredMethods, visited);

		// Same as the above, but for known classes.
		for (MethodMember method : info.getMethods()) {
			if (method.hasStaticModifier()
					|| method.hasPrivateModifier()
					|| "<init>".equals(method.getName())
					|| "<clinit>".equals(method.getName())) {
				continue;
			}
			String key = method.getName() + method.getDescriptor();
			if (method.hasAbstractModifier())
				requiredMethods.putIfAbsent(key,
						new PhantomMethodRequirement(method.getName(), method.getDescriptor(), false));
			else
				requiredMethods.remove(key);
		}
	}
}
