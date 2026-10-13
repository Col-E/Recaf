package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import software.coley.recaf.services.phantom.model.PhantomClassConstraint;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Chooses the superclass of every class-kind phantom.
 * <p>
 * Resolution happens in three steps:
 * <ol>
 *     <li><b>Planning</b>: Collect and rank each phantom’s superclass requests by how reliable they are.
 *     <ul>
 *         <li><i>Hard</i> requests are structural <i>(exception bases, enum bases, sealed parents)</i>.
 *             If they conflict, point to a final class, or form a cycle, the phantom is forced to extend
 *             {@code java/lang/Object}.</li>
 *         <li><i>Soft</i> requests come from approximate analysis. Soft requests that already point to
 *             known classes are cleaned up so they form a single inheritance chain.</li>
 *     </ul>
 *     </li>
 *     <li><b>Fixing known ancestry</b>: Any known class that appears in a requirement already has a fixed
 *     superclass. A phantom that is used as a superclass of a known class is treated as a free type that
 *     the solver can place later.</li>
 *     <li><b>Solving</b>: When phantoms form a cycle of soft relationships, keep only the direction that
 *     points toward the type with more incoming claims. The remaining relationships are handed to
 *     {@link TieredHierarchySolver}, which simply drops any that still conflict instead of failing.</li>
 * </ol>
 * If the solver cannot place a phantom, it keeps only its hard parent.Superclasses that were requested but not
 * chosen are recorded as {@link PhantomClassConstraint#getDroppedSupertypes() dropped}, so that their members can
 * still be copied into the subtype. Nothing here aborts generation since we want a 'best effort' phantom generator.
 * <p>
 * Types that give no evidence of being a class or interface are treated as classes.
 *
 * @author jus7n
 */
final class PhantomSuperclassResolver {
	private static final String ROOT = "java/lang/Object";

	private final PhantomGenerationContext context;
	private final Map<String, PhantomClassConstraint> constraints;
	private final Map<String, Set<String>> knownAncestorCache = new HashMap<>();
	private final Map<String, Plan> plans = new TreeMap<>();
	private final ClassLookup lookup;

	/**
	 * @param context
	 * 		Phantom analysis context.
	 */
	PhantomSuperclassResolver(@Nonnull PhantomGenerationContext context) {
		this.context = context;

		lookup = context.getLookup();
		constraints = context.getConstraints();
	}

	/**
	 * Assigns resolved and dropped superclasses to every phantom constraint.
	 */
	public void resolve() {
		// Build a plan for every class-kind phantom, which is the set of superclasses to offer the solver.
		for (PhantomClassConstraint constraint : List.copyOf(constraints.values()))
			if (isClassKind(constraint.getName()))
				plans.put(constraint.getName(), plan(constraint));

		// Fix known ancestry, which is the immutable superclass of every known class that appears in a requirement.
		Set<String> fixedTypes = new TreeSet<>();
		Map<String, String> fixedParents = new HashMap<>();
		Set<String> freeTypes = new TreeSet<>();
		fixedTypes.add(ROOT);
		ArrayDeque<String> knownQueue = new ArrayDeque<>();
		for (Plan plan : plans.values()) {
			// For known parents, the solver cannot choose a parent, so the type is fixed.
			// For unknown parents, the solver can choose a parent, so the type is free.
			if (plan.fixedParent() != null) {
				fixedTypes.add(plan.name());
				fixedParents.put(plan.name(), plan.fixedParent());
				if (lookup.isKnown(plan.fixedParent()))
					knownQueue.add(plan.fixedParent());
			} else {
				freeTypes.add(plan.name());
			}

			// For every kept superclass, if it is known, the solver cannot choose a parent, so the type is fixed.
			for (String kept : plan.kept())
				if (lookup.isKnown(kept))
					knownQueue.add(kept);
		}
		fixKnownAncestry(knownQueue, fixedTypes, fixedParents, freeTypes);

		// No phantom may extend a final class.
		Set<String> finalTypes = new TreeSet<>();
		for (String type : fixedTypes)
			if (!ROOT.equals(type) && lookup.isKnownFinal(type))
				finalTypes.add(type);
		List<FixedParentHierarchySolver.Relation> negative = new ArrayList<>();
		for (Plan plan : plans.values())
			for (String finalType : finalTypes)
				if (!plan.name().equals(finalType))
					negative.add(new FixedParentHierarchySolver.Relation(plan.name(), finalType));

		// Solve the hierarchy, which may drop some soft relationships.
		// The solver is guaranteed to succeed since it can always drop all soft relationships and keep only the hard parent.
		TieredHierarchySolver.Solution solution = TieredHierarchySolver.solve(fixedParents, freeTypes, positiveRelations(), negative);
		Map<String, String> parentByType = solution.parentByType();
		Map<String, Set<String>> acceptedByType = solution.acceptedByType();

		// Assign the resolved parent and dropped supertypes to every phantom constraint.
		for (PhantomClassConstraint constraint : constraints.values()) {
			// Classes with no plan are either interface-kind or invalid.
			// They are forced to extend java/lang/Object and drop all supertypes.
			Plan plan = plans.get(constraint.getName());
			if (plan == null) {
				constraint.setResolvedSuperName(ROOT);
				constraint.clearDroppedSupertypes();
				continue;
			}

			// The solver may have dropped some soft relationships, so the resolved parent is the one chosen by the solver,
			// and the dropped supertypes are the ones that were requested but not chosen.
			String parent = parentByType.get(plan.name());
			Set<String> kept = acceptedByType.getOrDefault(plan.name(), Set.of());
			if (parent == null) {
				// The type's component could not be solved, so keep only the hard parent.
				parent = plan.hardParent() != null ? plan.hardParent() : ROOT;
				kept = Set.of();
			}
			Set<String> dropped = new TreeSet<>(plan.targets());
			dropped.removeAll(kept);
			dropped.remove(parent);
			constraint.setResolvedSuperName(parent);
			constraint.setDroppedSupertypes(dropped);
		}
	}

	/**
	 * Gives every known class reachable from a requirement its immutable superclass. A phantom superclass of a known
	 * class must be a class, unless it carries interface evidence, so it is planned as a free type.
	 *
	 * @param knownQueue
	 * 		Known classes to visit.
	 * @param fixedTypes
	 * 		Types with an immutable superclass, extended with each visited known class.
	 * @param fixedParents
	 * 		Immutable superclass of each fixed type, extended with each visited known class.
	 * @param freeTypes
	 * 		Types the solver places, extended with phantom superclasses of known classes.
	 */
	private void fixKnownAncestry(@Nonnull ArrayDeque<String> knownQueue, @Nonnull Set<String> fixedTypes,
	                              @Nonnull Map<String, String> fixedParents, @Nonnull Set<String> freeTypes) {
		while (!knownQueue.isEmpty()) {
			// Abort when the type is already fixed, or when it is java/lang/Object.
			String known = knownQueue.poll();
			if (ROOT.equals(known) || fixedTypes.contains(known))
				continue;
			fixedTypes.add(known);

			// If the known type does not have a known superclass,
			// it is either a phantom that the solver can place,
			// or the type is invalid and will be forced to extend java/lang/Object.
			String parent = lookup.getKnownSuperName(known);
			if (parent == null) {
				fixedParents.put(known, ROOT);
				continue;
			}

			// If the parent is known, it is fixed and the solver cannot choose a parent for the known type.
			if (lookup.isKnown(parent)) {
				fixedParents.put(known, parent);
				knownQueue.add(parent);
				continue;
			}

			// The parent isn't known, so it is a phantom that the solver can place.
			// It must be a class-kind type, or the known type is invalid.
			PhantomClassConstraint parentConstraint = context.getOrCreateConstraint(parent);
			if (parentConstraint == null) {
				fixedParents.put(known, ROOT);
				continue;
			}
			if (!plans.containsKey(parent)) {
				parentConstraint.markClass();
				if (!isClassKind(parent)) {
					fixedParents.put(known, ROOT);
					continue;
				}
				plans.put(parent, new Plan(parent, null, null, Set.of(), Set.of(), Set.of()));
				freeTypes.add(parent);
			}
			fixedParents.put(known, parent);
		}
	}

	/**
	 * @return Every kept superclass relation with its tier, in plan order.
	 */
	@Nonnull
	private List<TieredHierarchySolver.TieredRelation> positiveRelations() {
		// Count the number of incoming edges for each member of a cycle, so that the direction of the cycle can be kept.
		Map<String, Integer> cycles = cyclicComponents();
		Map<String, Integer> cycleInDegree = new HashMap<>();
		for (Plan plan : plans.values())
			for (String target : plan.kept())
				if (sameCycle(cycles, plan.name(), target))
					cycleInDegree.merge(target, 1, Integer::sum);

		// Build the list of positive relations, which are the kept superclasses with their tier.
		// The solver will drop any that conflict, but the tier is used to prefer keeping evidence and hints over soft relationships.
		List<TieredHierarchySolver.TieredRelation> positive = new ArrayList<>();
		for (Plan plan : plans.values()) {
			Set<String> hinted = constraints.get(plan.name()).getHintedSupertypes();
			for (String target : plan.kept()) {
				// Get the tier of the relationship, which is used to prefer keeping evidence and hints over soft relationships.
				TieredHierarchySolver.Tier tier = plan.softPhantoms().contains(target) ? TieredHierarchySolver.Tier.SOFT
						: hinted.contains(target) ? TieredHierarchySolver.Tier.HINT
						: TieredHierarchySolver.Tier.EVIDENCE;

				// Flow-derived edges inside a cycle cannot all hold. A superclass collects claims from its subclasses, so
				// the edge into the member with more incoming claims is kept, with name order breaking ties. That order
				// is total, so the kept soft edges of a cycle cannot form a cycle themselves. Evidence and hints inside a
				// cycle are kept for the solver to judge.
				if (tier == TieredHierarchySolver.Tier.SOFT
						&& sameCycle(cycles, plan.name(), target)
						&& !keepsCycleDirection(plan.name(), target, cycleInDegree))
					continue;

				positive.add(new TieredHierarchySolver.TieredRelation(new FixedParentHierarchySolver.Relation(plan.name(), target), tier));
			}
		}
		return positive;
	}

	/**
	 * @return Strongly connected component id for every phantom that lies on a cycle of superclass edges.
	 */
	@Nonnull
	private Map<String, Integer> cyclicComponents() {
		Map<String, List<String>> graph = new HashMap<>();
		for (Plan plan : plans.values()) {
			List<String> edges = new ArrayList<>();
			for (String target : plan.kept())
				if (plans.containsKey(target))
					edges.add(target);
			graph.put(plan.name(), edges);
		}
		return StronglyConnectedComponents.cyclic(graph);
	}

	/**
	 * Builds the hierarchy requirements of one class-kind type.
	 *
	 * @param constraint
	 * 		Class-kind constraint.
	 *
	 * @return Plan describing the fixed parent, the targets to offer the solver, and every class target.
	 */
	@Nonnull
	private Plan plan(@Nonnull PhantomClassConstraint constraint) {
		// Collect every class-kind supertype target, and rank them by how reliable they are.
		// Hard targets are structural, direct targets are required, and soft targets are hints.
		// The solver will drop any that conflict.
		String name = constraint.getName();
		Set<String> allTargets = classTargets(name, constraint.getRequiredSupertypes());
		Set<String> hard = mostSpecific(classTargets(name, constraint.getHardSupertypes()));
		Set<String> direct = mostSpecific(classTargets(name, constraint.getRequiredDirectSupertypes()));
		Set<String> soft = new TreeSet<>(allTargets);
		soft.removeAll(constraint.getHardSupertypes());
		Set<String> hinted = classTargets(name, constraint.getHintedSupertypes());
		soft.removeAll(hinted);

		// Hard requirements that cannot all hold, or cannot hold at all, invalidate the type's hierarchy.
		String hardTarget = hard.size() == 1 ? hard.iterator().next() : null;
		boolean valid = hard.size() <= 1 && direct.size() <= 1;
		if (valid && hardTarget != null && (lookup.isKnownFinal(hardTarget) || knownAncestors(hardTarget).contains(name)))
			valid = false;
		if (!valid)
			return new Plan(name, ROOT, null, Set.of(), allTargets, Set.of());

		// Collect the targets to offer the solver, which are the hard targets, soft phantoms, and hinted targets.
		Set<String> kept = new TreeSet<>(classTargets(name, constraint.getHardSupertypes()));
		Set<String> softPhantoms = new TreeSet<>();
		List<String> knownSoft = new ArrayList<>();
		for (String target : soft) {
			if (lookup.isKnownFinal(target))
				continue;
			if (lookup.isKnown(target))
				knownSoft.add(target);
			else
				softPhantoms.add(target);
		}
		kept.addAll(softPhantoms);
		kept.addAll(hinted);

		// Known targets that would close a cycle, or that cannot be ancestors of an immutable direct parent, are dropped.
		String fixedParent = direct.isEmpty() ? null : direct.iterator().next();
		Set<String> fixedAncestors = fixedParent != null && lookup.isKnown(fixedParent)
				? knownAncestors(fixedParent) : null;
		knownSoft.removeIf(target -> {
			boolean cycle = knownAncestors(target).contains(name);
			boolean misplaced = fixedAncestors != null && !target.equals(fixedParent) && !fixedAncestors.contains(target);
			return cycle || misplaced;
		});

		// Known targets must form one chain. The hard target goes first, then deeper targets before shallower ones.
		List<String> order = new ArrayList<>();
		if (hardTarget != null && lookup.isKnown(hardTarget))
			order.add(hardTarget);
		knownSoft.sort(Comparator.<String>comparingInt(target -> knownAncestors(target).size())
				.reversed()
				.thenComparing(Comparator.naturalOrder()));
		order.addAll(knownSoft);
		List<String> chain = new ArrayList<>();
		for (String target : order) {
			if (chain.stream().allMatch(member -> comparable(member, target))) {
				chain.add(target);
				kept.add(target);
			}
		}

		String knownHard = hardTarget != null && lookup.isKnown(hardTarget) ? hardTarget : null;
		return new Plan(name, fixedParent, knownHard, Set.copyOf(kept), Set.copyOf(allTargets), Set.copyOf(softPhantoms));
	}

	/**
	 * @param owner
	 * 		Type whose targets these are.
	 * @param candidates
	 * 		Supertype candidates.
	 *
	 * @return The class-kind candidates, excluding the owner itself.
	 */
	@Nonnull
	private Set<String> classTargets(@Nonnull String owner, @Nonnull Collection<String> candidates) {
		Set<String> result = new TreeSet<>();
		for (String candidate : candidates)
			if (!candidate.equals(owner) && isClassKind(candidate))
				result.add(candidate);
		return result;
	}

	/**
	 * @param targets
	 * 		Supertype targets.
	 *
	 * @return Targets that are not strict ancestors of another target in the set.
	 */
	@Nonnull
	private Set<String> mostSpecific(@Nonnull Set<String> targets) {
		Set<String> result = new TreeSet<>();
		for (String target : targets) {
			boolean redundant = false;
			for (String other : targets)
				// A target is redundant when it is a strict ancestor of another target in the set.
				if (!other.equals(target) && knownAncestors(other).contains(target)) {
					redundant = true;
					break;
				}
			if (!redundant)
				result.add(target);
		}
		return result;
	}

	/**
	 * @param a
	 * 		Candidate type.
	 * @param b
	 * 		Another candidate type.
	 *
	 * @return {@code true} when one type is a known ancestor of, or the same as, the other.
	 */
	private boolean comparable(@Nonnull String a, @Nonnull String b) {
		return a.equals(b) || knownAncestors(a).contains(b) || knownAncestors(b).contains(a);
	}

	/**
	 * @param name
	 * 		Type name.
	 *
	 * @return Strict known ancestors of a known type. Walking stops at the first phantom type, which is included.
	 * Empty for phantom types.
	 */
	@Nonnull
	private Set<String> knownAncestors(@Nonnull String name) {
		Set<String> cached = knownAncestorCache.get(name);
		if (cached != null)
			return cached;
		Set<String> ancestors = new HashSet<>();
		if (lookup.isKnown(name)) {
			String current = lookup.getKnownSuperName(name);
			while (current != null && ancestors.add(current) && lookup.isKnown(current))
				current = lookup.getKnownSuperName(current);
		}
		knownAncestorCache.put(name, ancestors);
		return ancestors;
	}

	/**
	 * @param name
	 * 		Type name.
	 *
	 * @return {@code true} when the type is, or is assumed to be, a class rather than an interface or annotation.
	 */
	private boolean isClassKind(@Nonnull String name) {
		return switch (context.kindOf(name)) {
			case CLASS, ENUM, UNKNOWN -> true;
			case INTERFACE, ANNOTATION -> false;
		};
	}

	/**
	 * @param subtype
	 * 		Source of the edge.
	 * @param supertype
	 * 		Target of the edge.
	 * @param inDegree
	 * 		Map of type to its number of incoming edges in the cycle.
	 *
	 * @return {@code true} when the edge {@code subtype -> supertype} points toward the member with more incoming
	 * claims, with name order breaking ties. Only such edges are kept inside a cycle.
	 */
	private static boolean keepsCycleDirection(@Nonnull String subtype, @Nonnull String supertype,
	                                           @Nonnull Map<String, Integer> inDegree) {
		int subtypeDegree = inDegree.getOrDefault(subtype, 0);
		int supertypeDegree = inDegree.getOrDefault(supertype, 0);
		return supertypeDegree > subtypeDegree
				|| (supertypeDegree == subtypeDegree && supertype.compareTo(subtype) > 0);
	}

	/**
	 * @param cycles
	 * 		Map of type to its strongly connected component id.
	 * @param a
	 * 		Candidate type.
	 * @param b
	 * 		Another candidate type.
	 *
	 * @return {@code true} when both types lie on a common cycle.
	 */
	private static boolean sameCycle(@Nonnull Map<String, Integer> cycles, @Nonnull String a, @Nonnull String b) {
		Integer component = cycles.get(a);
		return component != null && component.equals(cycles.get(b));
	}

	/**
	 * @param name
	 * 		Class-kind type being planned.
	 * @param fixedParent
	 * 		Immutable superclass. Either the required direct parent, or {@code java/lang/Object} when the type is invalid.
	 *        {@code null} when the solver chooses the parent.
	 * @param hardParent
	 * 		Known hard superclass used when the solver cannot place the type, or {@code null}.
	 * @param kept
	 * 		Superclass targets offered to the solver.
	 * @param targets
	 * 		Every class-kind supertype target.
	 * @param softPhantoms
	 * 		Soft superclass targets that are phantom types.
	 * 		These are the first culprits dropped on conflict.
	 */
	private record Plan(@Nonnull String name,
	                    @Nullable String fixedParent,
	                    @Nullable String hardParent,
	                    @Nonnull Set<String> kept,
	                    @Nonnull Set<String> targets,
	                    @Nonnull Set<String> softPhantoms) {}
}
