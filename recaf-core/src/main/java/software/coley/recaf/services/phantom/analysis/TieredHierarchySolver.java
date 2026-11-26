package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Places free types with {@link FixedParentHierarchySolver}, keeping as many of the requested superclass relations as
 * can hold together.
 * <p>
 * Relations rarely all hold, since most come from approximate flow analysis. Rather than give up on every relation
 * when one conflicts, the relations are handled in two layers:
 * <ul>
 *     <li><b>Components</b>: Relations are grouped into components that share no free type, and each component is
 *     solved alone. A conflict only costs the component it occurs in.</li>
 *     <li><b>Tiers</b>: Within a component, relations are offered in a fixed order <i>(hints, then evidence, then soft
 *     relations)</i>. A relation that conflicts with those already accepted is dropped, so a later relation never displaces
 *     an earlier one. If the component still cannot be solved, soft relations are left out entirely and the component
 *     is tried again, and then hints as well.</li>
 * </ul>
 * A component that fails at every tier leaves its free types unplaced. No other component is affected.
 *
 * @author jus7n
 */
final class TieredHierarchySolver {
	private static final String ROOT = "java/lang/Object";

	private TieredHierarchySolver() {}

	/**
	 * @param fixedParents
	 * 		Immutable direct parent of every fixed type. {@code java/lang/Object} is the root and has none.
	 * @param freeTypes
	 * 		Types whose direct parent may be chosen.
	 * @param positive
	 * 		Requested superclass relations, in the order to offer them within each tier.
	 * @param negative
	 * 		Forbidden strict subtype relations. These are never dropped.
	 *
	 * @return Placement of every free type that its component could place, and the relations each component kept.
	 */
	@Nonnull
	public static Solution solve(@Nonnull Map<String, String> fixedParents,
	                             @Nonnull Set<String> freeTypes,
	                             @Nonnull List<TieredRelation> positive,
	                             @Nonnull List<FixedParentHierarchySolver.Relation> negative) {
		// A relation joins its subtype's fragment to its supertype's fragment.
		// A fragment rooted at Object cannot move, so it joins nothing, and a fixed subtype on it forms its own node.
		UnionFind components = new UnionFind();
		for (TieredRelation entry : positive)
			components.union(ownerNode(entry.relation().subtype(), fixedParents, freeTypes), freeNode(entry.relation().supertype(), fixedParents, freeTypes));
		for (FixedParentHierarchySolver.Relation relation : negative)
			components.union(ownerNode(relation.subtype(), fixedParents, freeTypes), freeNode(relation.supertype(), fixedParents, freeTypes));

		// Group relations by the component they touch.
		// Each component is solved alone, so a conflict only costs the component it occurs in.
		Map<String, Group> groups = new TreeMap<>();
		for (TieredRelation entry : positive) {
			FixedParentHierarchySolver.Relation relation = entry.relation();
			Group group = groups.computeIfAbsent(components.find(ownerNode(relation.subtype(), fixedParents, freeTypes)), _ -> new Group());
			group.addFreeNode(relation.subtype(), fixedParents, freeTypes);
			group.addFreeNode(relation.supertype(), fixedParents, freeTypes);
			group.relationsOf(entry.tier()).add(relation);
		}
		for (FixedParentHierarchySolver.Relation relation : negative) {
			Group group = groups.computeIfAbsent(components.find(ownerNode(relation.subtype(), fixedParents, freeTypes)), _ -> new Group());
			group.addFreeNode(relation.subtype(), fixedParents, freeTypes);
			group.addFreeNode(relation.supertype(), fixedParents, freeTypes);
			group.negative.add(relation);
		}

		// Solve each component, keeping as many relations as can hold together.
		Map<String, String> parentByType = new HashMap<>(fixedParents);
		Map<String, Set<String>> acceptedByType = new HashMap<>();
		for (Group group : groups.values()) {
			// The solver only needs the fixed chains that the component's relations touch.
			Set<String> fixed = new TreeSet<>();
			fixed.add(ROOT);
			for (List<FixedParentHierarchySolver.Relation> relations : List.of(group.evidence, group.hints, group.soft, group.negative))
				for (FixedParentHierarchySolver.Relation relation : relations) {
					addChain(relation.subtype(), fixedParents, freeTypes, fixed);
					addChain(relation.supertype(), fixedParents, freeTypes, fixed);
				}
			Map<String, String> groupParents = new HashMap<>();
			for (String name : fixed)
				if (!ROOT.equals(name) && fixedParents.get(name) != null)
					groupParents.put(name, fixedParents.get(name));

			// Hints are offered before evidence, so a relation that javac asked for is kept over a conflicting one
			// inferred from the input. Each fallback attempt leaves out one more tier: first soft relations, then hints.
			List<List<FixedParentHierarchySolver.Relation>> attempts = List.of(
					concat(group.hints, group.evidence, group.soft),
					concat(group.hints, group.evidence),
					group.evidence);
			for (List<FixedParentHierarchySolver.Relation> attempt : attempts) {
				// The solver returns null when the negatives alone are unsatisfiable, which is a problem with the input.
				// The solver also returns null when the attempt is unsatisfiable, which is expected and handled by the next attempt.
				Outcome outcome = solveRelations(attempt, fixed, groupParents, group.freeTypes, group.negative);
				if (outcome == null)
					continue;

				// The solver returned a satisfiable placement of the component's free types, and the relations it could keep.
				// The component's free types are now placed, and the accepted relations are recorded.
				for (String free : group.freeTypes) {
					String parent = outcome.result().parentByType().get(free);
					if (parent != null)
						parentByType.put(free, parent);
				}
				for (FixedParentHierarchySolver.Relation relation : outcome.accepted())
					acceptedByType.computeIfAbsent(relation.subtype(), _ -> new TreeSet<>()).add(relation.supertype());
				break;
			}
		}
		return new Solution(parentByType, acceptedByType);
	}

	/**
	 * Keeps a satisfiable subset of {@code candidate}, taking relations in order and dropping each one that makes the
	 * prefix unsatisfiable. Each culprit is located by galloping forward from the previous culprit, so the search costs
	 * {@code O(culprits * log n)} solver calls and is never cut short.
	 * <blockquote>But what is 'galloping'?</blockquote>
	 * See: <a href="https://en.wikipedia.org/wiki/Exponential_search">Exponential search</a>
	 *
	 * @param candidate
	 * 		Relations to keep, in priority order.
	 * @param fixedTypes
	 * 		Types whose direct parent is immutable, including {@code java/lang/Object}.
	 * @param fixedParents
	 * 		Immutable direct parent of every fixed type except {@code java/lang/Object}.
	 * @param freeTypes
	 * 		Types whose direct parent may be chosen.
	 * @param negative
	 * 		Forbidden strict subtype relations.
	 *
	 * @return The accepted relations and their solution, or {@code null} when the negatives alone are unsatisfiable.
	 */
	@Nullable
	private static Outcome solveRelations(@Nonnull List<FixedParentHierarchySolver.Relation> candidate,
	                                      @Nonnull Set<String> fixedTypes,
	                                      @Nonnull Map<String, String> fixedParents,
	                                      @Nonnull Set<String> freeTypes,
	                                      @Nonnull List<FixedParentHierarchySolver.Relation> negative) {
		if (FixedParentHierarchySolver.solve(ROOT, fixedTypes, fixedParents, freeTypes, List.of(), negative) == null)
			return null;

		List<FixedParentHierarchySolver.Relation> accepted = new ArrayList<>();
		int n = candidate.size();
		int start = 0;
		while (start < n) {
			// Invariant: accepted + candidate[start, lo) is satisfiable, and accepted + candidate[start, hi) is not.
			int lo = start;
			int hi = -1;
			int step = 1;
			while (lo < n) {
				int probe = Math.min(n, lo + step);
				if (!satisfiable(accepted, candidate, start, probe, fixedTypes, fixedParents, freeTypes, negative)) {
					hi = probe;
					break;
				}
				lo = probe;
				step <<= 1;
			}
			if (hi < 0) {
				accepted.addAll(candidate.subList(start, n));
				break;
			}
			while (hi - lo > 1) {
				int mid = (lo + hi) >>> 1;
				if (satisfiable(accepted, candidate, start, mid, fixedTypes, fixedParents, freeTypes, negative))
					lo = mid;
				else
					hi = mid;
			}

			// candidate[lo] is the culprit. Everything before it in this stretch is kept.
			accepted.addAll(candidate.subList(start, lo));
			start = lo + 1;
		}
		FixedParentHierarchySolver.Result result = FixedParentHierarchySolver.solve(ROOT, fixedTypes, fixedParents, freeTypes, accepted, negative);
		return result == null ? null : new Outcome(result, accepted);
	}

	/**
	 * Checks whether the accepted relations plus {@code candidate[start, end)} can hold together.
	 *
	 * @param accepted
	 * 		Relations already accepted, in their original order.
	 * @param candidate
	 * 		Relations to check, in their original order.
	 * @param start
	 * 		Position of the first relation to check in {@code candidate}.
	 * @param end
	 * 		Position after the last relation to check in {@code candidate}.
	 * @param fixedTypes
	 * 		Types whose direct parent is immutable, including {@code rootName}.
	 * @param fixedParents
	 * 		Immutable direct parent for every fixed type except {@code rootName}.
	 * @param freeTypes
	 * 		Types whose direct parent may be chosen. No type may be both fixed and free.
	 * @param negative
	 * 		Forbidden strict subtype relations.
	 *
	 * @return {@code true} when the accepted relations plus {@code candidate[start, end)} can hold together.
	 */
	private static boolean satisfiable(@Nonnull List<FixedParentHierarchySolver.Relation> accepted,
	                                   @Nonnull List<FixedParentHierarchySolver.Relation> candidate,
	                                   int start, int end,
	                                   @Nonnull Set<String> fixedTypes,
	                                   @Nonnull Map<String, String> fixedParents,
	                                   @Nonnull Set<String> freeTypes,
	                                   @Nonnull List<FixedParentHierarchySolver.Relation> negative) {
		List<FixedParentHierarchySolver.Relation> trial = new ArrayList<>(accepted.size() + (end - start));
		trial.addAll(accepted);
		trial.addAll(candidate.subList(start, end));
		return FixedParentHierarchySolver.solve(ROOT, fixedTypes, fixedParents, freeTypes, trial, negative) != null;
	}

	/**
	 * @param name
	 * 		Type whose fragment is being queried.
	 * @param fixedParents
	 * 		Map of every fixed type to its immutable direct parent. {@code java/lang/Object} is the root and has none.
	 * @param freeTypes
	 * 		Set of free types, to check whether the fragment can move.
	 *
	 * @return The free type whose fragment contains {@code name}, or {@code null} when that fragment is rooted at
	 * {@code java/lang/Object}, which no free type can move.
	 */
	@Nullable
	private static String freeNode(@Nonnull String name, @Nonnull Map<String, String> fixedParents,
	                               @Nonnull Set<String> freeTypes) {
		String root = fragmentRoot(name, fixedParents);
		return freeTypes.contains(root) ? root : null;
	}

	/**
	 * @param name
	 * 		Type whose fragment is being queried.
	 * @param fixedParents
	 * 		Map of every fixed type to its immutable direct parent. {@code java/lang/Object} is the root and has none.
	 * @param freeTypes
	 * 		Set of free types, to check whether the fragment can move.
	 *
	 * @return The free type whose fragment contains {@code name}, or a node of its own when the fragment cannot move.
	 * The {@code @} prefix keeps such nodes apart from type names.
	 */
	@Nonnull
	private static String ownerNode(@Nonnull String name, @Nonnull Map<String, String> fixedParents,
	                                @Nonnull Set<String> freeTypes) {
		String free = freeNode(name, fixedParents, freeTypes);
		return free != null ? free : "@" + name;
	}

	/**
	 * Adds the fixed types from {@code name} up to its fragment root, and {@code java/lang/Object}.
	 *
	 * @param name
	 * 		Type whose fixed ancestry is being added.
	 * @param fixedParents
	 * 		Map of every fixed type to its immutable direct parent.
	 *        {@code java/lang/Object} is the root and has none.
	 * @param freeTypes
	 * 		Set of free types, which are not added to {@code fixed}.
	 * @param fixed
	 * 		Set to add to.
	 */
	private static void addChain(@Nonnull String name, @Nonnull Map<String, String> fixedParents,
	                             @Nonnull Set<String> freeTypes, @Nonnull Set<String> fixed) {
		String current = name;
		int steps = 0;
		while (!ROOT.equals(current) && !freeTypes.contains(current) && steps++ <= fixedParents.size()) {
			fixed.add(current);
			current = fixedParents.get(current);
			if (current == null)
				break;
		}
		fixed.add(ROOT);
	}

	/**
	 * @param name
	 * 		Type whose fragment is being walked.
	 * @param fixedParents
	 * 		Map of every fixed type to its immutable direct parent.
	 *        {@code java/lang/Object} is the root and has none.
	 *
	 * @return The top of the immutable parent chain above {@code name}. Walking is bounded, so a cycle cannot hang.
	 */
	@Nonnull
	private static String fragmentRoot(@Nonnull String name, @Nonnull Map<String, String> fixedParents) {
		String current = name;
		for (int steps = 0; fixedParents.containsKey(current) && steps <= fixedParents.size(); steps++)
			current = fixedParents.get(current);
		return current;
	}

	@SafeVarargs
	@Nonnull
	private static List<FixedParentHierarchySolver.Relation> concat(@Nonnull List<FixedParentHierarchySolver.Relation>... lists) {
		List<FixedParentHierarchySolver.Relation> result = new ArrayList<>();
		for (List<FixedParentHierarchySolver.Relation> list : lists)
			result.addAll(list);
		return result;
	}

	/**
	 * Source of a superclass relation, which decides when it is offered and when it is given up.
	 */
	enum Tier {
		/**
		 * Required by the structure of a type, such as an exception or enum base, or a relation to a known class that
		 * survived planning. Known classes have fixed ancestry, so these relations can be checked before solving.
		 */
		EVIDENCE,
		/**
		 * Supplied by a {@link software.coley.recaf.services.phantom.PhantomHint}.
		 */
		HINT,
		/**
		 * Inferred by flow analysis between two phantom types. These are the first to be dropped on conflict.
		 */
		SOFT
	}

	/**
	 * @param relation
	 * 		Required strict superclass relation.
	 * @param tier
	 * 		Source of the relation.
	 */
	record TieredRelation(@Nonnull FixedParentHierarchySolver.Relation relation, @Nonnull Tier tier) {}

	/**
	 * @param parentByType
	 * 		Parent of every fixed type, and of every free type that a component placed.
	 * @param acceptedByType
	 * 		Superclass relations accepted by their components, keyed by subtype.
	 */
	record Solution(@Nonnull Map<String, String> parentByType,
	                @Nonnull Map<String, Set<String>> acceptedByType) {}

	/**
	 * Relations of one component. Every free type in it is reachable from every relation through shared fragments.
	 */
	private static final class Group {
		private final Set<String> freeTypes = new TreeSet<>();
		private final List<FixedParentHierarchySolver.Relation> evidence = new ArrayList<>();
		private final List<FixedParentHierarchySolver.Relation> hints = new ArrayList<>();
		private final List<FixedParentHierarchySolver.Relation> soft = new ArrayList<>();
		private final List<FixedParentHierarchySolver.Relation> negative = new ArrayList<>();

		/**
		 * @return Positive relations of the given tier.
		 */
		@Nonnull
		private List<FixedParentHierarchySolver.Relation> relationsOf(@Nonnull Tier tier) {
			return switch (tier) {
				case EVIDENCE -> evidence;
				case HINT -> hints;
				case SOFT -> soft;
			};
		}

		/**
		 * Records the free type whose fragment contains {@code name}, if there is one.
		 */
		private void addFreeNode(@Nonnull String name, @Nonnull Map<String, String> fixedParents,
		                         @Nonnull Set<String> freeTypes) {
			String free = freeNode(name, fixedParents, freeTypes);
			if (free != null)
				this.freeTypes.add(free);
		}
	}

	/**
	 * @param result
	 * 		Solver result for the accepted relations.
	 * @param accepted
	 * 		Relations kept, in their original order.
	 */
	private record Outcome(@Nonnull FixedParentHierarchySolver.Result result,
	                       @Nonnull List<FixedParentHierarchySolver.Relation> accepted) {}

	/**
	 * Disjoint sets of node names, with path compression.
	 */
	private static final class UnionFind {
		private final Map<String, String> parent = new HashMap<>();

		/**
		 * Merges the sets of two nodes. Nothing happens when {@code b} is {@code null}.
		 */
		private void union(@Nonnull String a, @Nullable String b) {
			if (b == null)
				return;
			String rootA = find(a);
			String rootB = find(b);
			if (!rootA.equals(rootB))
				parent.put(rootA, rootB);
		}

		/**
		 * @return Representative of the node's set.
		 */
		@Nonnull
		private String find(@Nonnull String name) {
			String root = name;
			while (!parent.getOrDefault(root, root).equals(root))
				root = parent.get(root);
			String current = name;
			while (!current.equals(root)) {
				String next = parent.getOrDefault(current, current);
				parent.put(current, root);
				current = next;
			}
			return root;
		}
	}
}
