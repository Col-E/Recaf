package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link FixedParentHierarchySolver}, checked against a brute-force oracle that tries every possible tree.
 */
class FixedParentHierarchySolverTest {
	private static final String[] TYPES = {"root", "A", "B"};
	/** Every ordered pair of distinct types, by index into {@link #TYPES}. */
	private static final int[][] ORDERED_PAIRS = {
			{0, 1}, {0, 2}, {1, 0}, {1, 2}, {2, 0}, {2, 1}
	};

	@Test
	void exhaustiveThreeTypeSignedInstancesMatchTreeOracle() {
		// Every way to fix parents among three types:
		// - None fixed
		// - 'A' fixed under the root
		// - 'A' fixed under the free 'B'
		// - Both fixed in a chain.
		List<FixedParentStructure> structures = List.of(
				new FixedParentStructure(Set.of("root"), Map.of(), Set.of("A", "B")),
				new FixedParentStructure(Set.of("root", "A"), Map.of("A", "root"), Set.of("B")),
				new FixedParentStructure(Set.of("root", "A"), Map.of("A", "B"), Set.of("B")),
				new FixedParentStructure(Set.of("root", "A", "B"), Map.of("A", "B", "B", "root"), Set.of())
		);
		final int permutations = 3 * 3 * 3 * 3 * 3 * 3; // 3^6 = 729 labelings of the six ordered pairs.
		for (FixedParentStructure structure : structures) {
			for (int labels = 0; labels < permutations; labels++) {
				int remaining = labels;
				List<FixedParentHierarchySolver.Relation> positive = new ArrayList<>();
				List<FixedParentHierarchySolver.Relation> negative = new ArrayList<>();
				for (int[] pair : ORDERED_PAIRS) {
					int label = remaining % 3;
					remaining /= 3;
					if (label == 1)
						positive.add(relation(pair));
					else if (label == 2)
						negative.add(relation(pair));
				}

				// The solver should find a solution exactly when the oracle does, and any solution must be valid.
				boolean expected = oracleSatisfiable(structure, positive, negative);
				FixedParentHierarchySolver.Result actual = FixedParentHierarchySolver.solve("root", structure.fixedTypes,
						structure.fixedParents, structure.freeTypes, positive, negative);
				assertEquals(expected, actual != null, "Mismatch for structure " + structure + " and labels " + labels);
				if (actual != null)
					assertWitness(structure, actual, positive, negative);
			}
		}
	}

	@Test
	void fixedKnownChildCannotBeMadeItsOwnAncestor() {
		// 'knownChild' has the fixed parent 'missingParent'.
		// Requiring 'missingParent' to be below 'knownChild' would need a cycle, so there is no solution.
		List<FixedParentHierarchySolver.Relation> positive = List.of(new FixedParentHierarchySolver.Relation("missingParent", "knownChild"));
		FixedParentHierarchySolver.Result result = FixedParentHierarchySolver.solve(
				"root",
				Set.of("root", "knownChild"),
				Map.of("knownChild", "missingParent"),
				Set.of("missingParent"),
				positive,
				List.of());
		assertNull(result);
	}

	/**
	 * @param pair
	 * 		Two indices into {@link #TYPES} for the ordered pair of types.
	 *
	 * @return Relation for the ordered pair of types.
	 */
	@Nonnull
	private static FixedParentHierarchySolver.Relation relation(int[] pair) {
		return new FixedParentHierarchySolver.Relation(TYPES[pair[0]], TYPES[pair[1]]);
	}

	/**
	 * Tries every parent assignment for 'A' and 'B', since there are few enough to enumerate.
	 *
	 * @param structure
	 * 		Which types have fixed parents, and which are free.
	 * @param positive
	 * 		Relations that must hold in the tree.
	 * @param negative
	 * 		Relations that must not hold in the tree.
	 *
	 * @return {@code true} when some rooted tree matches the fixed parents and every relation.
	 */
	private static boolean oracleSatisfiable(@Nonnull FixedParentStructure structure,
	                                         @Nonnull List<FixedParentHierarchySolver.Relation> positive,
	                                         @Nonnull List<FixedParentHierarchySolver.Relation> negative) {
		for (int parentA = 0; parentA < TYPES.length; parentA++) {
			if (parentA == 1)
				continue;
			for (int parentB = 0; parentB < TYPES.length; parentB++) {
				if (parentB == 2)
					continue;
				int[] parents = {-1, parentA, parentB};
				if (!isRootedTree(parents) || !matchesFixedParents(structure, parents))
					continue;
				if (matches(positive, parents, true) && matches(negative, parents, false))
					return true;
			}
		}
		return false;
	}

	/**
	 * Asserts that the solver's result is a rooted tree that matches the fixed parents and every relation.
	 *
	 * @param structure
	 * 		Which types have fixed parents, and which are free.
	 * @param result
	 * 		Result from the solver.
	 * @param positive
	 * 		Relations that must hold in the tree.
	 * @param negative
	 * 		Relations that must not hold in the tree.
	 */
	private static void assertWitness(@Nonnull FixedParentStructure structure,
	                                  @Nonnull FixedParentHierarchySolver.Result result,
	                                  @Nonnull List<FixedParentHierarchySolver.Relation> positive,
	                                  @Nonnull List<FixedParentHierarchySolver.Relation> negative) {
		Map<String, Integer> index = new HashMap<>();
		for (int i = 0; i < TYPES.length; i++)
			index.put(TYPES[i], i);
		int[] parents = {-1, -1, -1};
		for (int i = 1; i < TYPES.length; i++) {
			String parentName = result.parentByType().get(TYPES[i]);
			assertNotNull(parentName);
			parents[i] = index.get(parentName);
		}
		assertTrue(isRootedTree(parents));
		assertTrue(matchesFixedParents(structure, parents));
		assertTrue(matches(positive, parents, true));
		assertTrue(matches(negative, parents, false));
	}

	/**
	 * @param structure
	 * 		Which types have fixed parents, and which are free.
	 * @param parents
	 * 		Parent array by index into {@link #TYPES}.
	 *
	 * @return {@code true} when the parent array matches the fixed parents in the structure.
	 */
	private static boolean matchesFixedParents(@Nonnull FixedParentStructure structure, int[] parents) {
		for (Map.Entry<String, String> entry : structure.fixedParents.entrySet())
			if (parents[indexOf(entry.getKey())] != indexOf(entry.getValue()))
				return false;
		return true;
	}

	/**
	 * @param relations
	 * 		List of relations to check in the tree.
	 * @param parents
	 * 		Parent array by index into {@link #TYPES}.
	 * @param expected
	 *        {@code true} when the relations are expected to hold,
	 *        {@code false} when they are expected not to hold.
	 *
	 * @return {@code true} when every relation holds in the tree,
	 * or when none does if {@code expected} is  {@code false}.
	 */
	private static boolean matches(@Nonnull List<FixedParentHierarchySolver.Relation> relations, int[] parents, boolean expected) {
		for (FixedParentHierarchySolver.Relation relation : relations) {
			int source = indexOf(relation.subtype());
			int target = indexOf(relation.supertype());
			boolean subtype = false;
			for (int current = source; current >= 0; current = parents[current])
				if (current == target) {
					subtype = true;
					break;
				}
			if (subtype != expected)
				return false;
		}
		return true;
	}

	/**
	 * @param parents
	 * 		Parent array by index into {@link #TYPES}.
	 *
	 * @return {@code true} when every type reaches the root by following parents, without a cycle.
	 */
	private static boolean isRootedTree(int[] parents) {
		for (int start = 1; start < parents.length; start++) {
			Set<Integer> seen = new HashSet<>();
			int current = start;
			while (current > 0 && seen.add(current))
				current = parents[current];
			if (current != 0)
				return false;
		}
		return true;
	}

	/**
	 * @param typeName
	 * 		Type name to find the index of.
	 *
	 * @return Index into {@link #TYPES} for the given type name.
	 *
	 * @throws IllegalArgumentException
	 * 		When the type name is not in {@link #TYPES}.
	 */
	private static int indexOf(@Nonnull String typeName) {
		for (int i = 0; i < TYPES.length; i++)
			if (TYPES[i].equals(typeName))
				return i;
		throw new IllegalArgumentException(typeName);
	}

	/**
	 * Which types have fixed parents, and which are free.
	 *
	 * @param fixedTypes
	 * 		Fixed types that must be in the tree.
	 * @param fixedParents
	 * 		Fixed parent assignments for some types, by type name.
	 * @param freeTypes
	 * 		Free types that may be placed anywhere in the tree.
	 */
	private record FixedParentStructure(@Nonnull Set<String> fixedTypes,
	                                    @Nonnull Map<String, String> fixedParents,
	                                    @Nonnull Set<String> freeTypes) {}
}
