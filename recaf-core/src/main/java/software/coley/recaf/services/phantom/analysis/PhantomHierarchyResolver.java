package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.member.MethodMember;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;
import software.coley.recaf.services.phantom.model.PhantomFieldRequirement;
import software.coley.recaf.services.phantom.model.PhantomMethodRequirement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Resolves the phantom constraints into a type hierarchy.
 * <p>
 * Superclasses are chosen by {@link PhantomSuperclassResolver}, and interfaces here.
 *
 * @author Matt Coley
 */
public class PhantomHierarchyResolver {
	private static final String ANNOTATION = "java/lang/annotation/Annotation";
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
	 * Resolves nested-class metadata, supertypes, dropped-supertype members, and inherited methods.
	 */
	public void resolve() {
		// Fill in inner/outer class relations for nested classes.
		linkNestedClasses();

		// Choose superclasses, then interfaces, for all phantom types.
		new PhantomSuperclassResolver(context).resolve();
		resolveInterfaces();

		// Ensure all class constraints declare members from their dropped superclasses.
		completeDroppedSupertypeMembers();

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
		// However, if the name is something like packageName/$Foo then it is not a nested class, just a goober name.
		String name = constraint.getName();
		int separator = name.lastIndexOf('$');
		int packageSeparator = name.lastIndexOf('/');
		if (separator <= packageSeparator + 1 || separator == name.length() - 1)
			return;

		// We do. Split the outer and inner names and link them together.
		String outerName = name.substring(0, separator);
		String innerSimpleName = name.substring(separator + 1);
		constraint.markInnerClassOf(outerName, innerSimpleName);
		PhantomClassConstraint outerConstraint = context.getOrCreateConstraint(outerName);
		if (outerConstraint != null) {
			outerConstraint.addDeclaredInner(constraint.getName(), innerSimpleName);
			linkNestedClass(outerConstraint, visited);
		}
	}

	/**
	 * Resolves interfaces for every constraint, then breaks any interface cycles introduced by flow analysis.
	 */
	private void resolveInterfaces() {
		// Populate a map of all interfaces that each phantom class constraint could implement.
		Map<String, Set<String>> interfaces = new TreeMap<>();
		for (PhantomClassConstraint constraint : constraints.values()) {
			String name = constraint.getName();

			// @annotation is implicitly implemented by all annotations
			Set<String> set = new TreeSet<>();
			if (constraint.isAnnotation())
				set.add(ANNOTATION);

			// Add all required supertypes that are interfaces or annotations.
			for (String candidate : constraint.getRequiredSupertypes()) {
				if (candidate.equals(name))
					continue;

				PhantomTypeKind kind = context.kindOf(candidate);
				if (kind == PhantomTypeKind.INTERFACE || kind == PhantomTypeKind.ANNOTATION)
					set.add(candidate);
			}
			interfaces.put(name, set);
		}

		// An interface that already reaches its implementer would close a cycle, so it is removed.
		for (Map.Entry<String, Set<String>> entry : interfaces.entrySet()) {
			Set<String> set = entry.getValue();
			for (String candidate : List.copyOf(set))
				if (reachesInterface(candidate, entry.getKey(), new HashSet<>(), interfaces))
					set.remove(candidate);
		}

		// Populate the resolved interfaces for each constraint.
		for (PhantomClassConstraint constraint : constraints.values()) {
			constraint.clearResolvedInterfaces();
			for (String interfaceName : interfaces.get(constraint.getName()))
				constraint.addResolvedInterface(interfaceName);
		}
	}

	/**
	 * @param from
	 * 		Type to start from.
	 * @param to
	 * 		Type to look for.
	 * @param visited
	 * 		Types already visited.
	 * @param interfaces
	 * 		Interfaces chosen so far for each phantom.
	 *
	 * @return {@code true} when {@code from} is {@code to} or reaches it through super-interfaces.
	 */
	private boolean reachesInterface(@Nonnull String from, @Nonnull String to, @Nonnull Set<String> visited,
	                                 @Nonnull Map<String, Set<String>> interfaces) {
		if (from.equals(to))
			return true;
		if (!visited.add(from))
			return false;
		Set<String> next = interfaces.containsKey(from) ? interfaces.get(from) : lookup.getKnownInterfaces(from);
		for (String candidate : next)
			if (reachesInterface(candidate, to, visited, interfaces))
				return true;
		return false;
	}

	/**
	 * Ensures all class constraints declare members from their dropped superclasses.
	 */
	private void completeDroppedSupertypeMembers() {
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Interfaces only need their declared methods, so there's no need to copy members from dropped supertypes.
			if (constraint.isInterface())
				continue;

			// Skip constraints that did not drop any superclass.
			if (constraint.getDroppedSupertypes().isEmpty())
				continue;

			// For each dropped supertype, copy inheritable instance members into the target constraint to
			// facilitate compilation contracts matching what we already got in the analyzed classes.
			for (String droppedSupertype : constraint.getDroppedSupertypes())
				collectDroppedSupertypeMembers(droppedSupertype, constraint, new HashSet<>());
		}
	}

	/**
	 * Recursively copies inheritable instance members from a dropped superclass into the target constraint.
	 *
	 * @param typeName
	 * 		Type to collect members from.
	 * @param targetConstraint
	 * 		Constraint receiving copied members.
	 * @param visited
	 * 		Classes already visited in this collection.
	 */
	private void collectDroppedSupertypeMembers(@Nullable String typeName,
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
			collectDroppedSupertypeMembers(phantom.getResolvedSuperName(), targetConstraint, visited);
			for (String interfaceName : phantom.getResolvedInterfaces())
				collectDroppedSupertypeMembers(interfaceName, targetConstraint, visited);

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
		collectDroppedSupertypeMembers(info.getSuperName(), targetConstraint, visited);
		for (String interfaceName : info.getInterfaces())
			collectDroppedSupertypeMembers(interfaceName, targetConstraint, visited);

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
