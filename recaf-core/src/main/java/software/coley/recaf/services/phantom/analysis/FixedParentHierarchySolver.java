package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Completes a single-inheritance type hierarchy.
 * <p>
 * Some types <i>(known classes)</i> have fixed parents while others <i>(phantoms)</i> do not.
 * The solver assigns a parent to every free type so that:
 * <ul>
 *   <li>The result is a single tree.</li>
 *   <li>Every required subtype relation holds.</li>
 *   <li>Every forbidden subtype relation is avoided.</li>
 * </ul>
 * <p>
 * The solver algorithm is exact and non-backtracking. It operates on <i>"fragments"</i>,
 * the trees induced by the already-fixed parent edges. Each fragment is rooted at a free
 * type or at the overall root. Internal ancestry is immutable.
 * <ol>
 *   <li><b>Normalization</b>.
 *       Relations that lie wholly inside one fragment are checked and discarded.
 *       Every remaining relation is rewritten so that its subtype is a fragment root
 *       <i>(because any path leaving a fragment must leave through the root)</i>.
 *       The supertype is left unchanged.</li>
 *   <li><b>Elimination</b>.
 *       A fragment may sit at the top of the remaining types when it is <i>free</i>:
 *       <ul>
 *         <li>Its root is not required to be a subtype of any other remaining fragment,</li>
 *         <li>Removing it partitions the rest into components connected by required relations.
 *             For each component the required targets inside the fragment lie on a single path
 *             from the root, and the deepest of them is the unique attachment point, and no
 *             member of a component is forbidden from residing under that attachment point
 *             <i>(or any ancestor of it)</i>.</li>
 *       </ul>
 *       The free fragment is placed, then each component is solved independently and
 *       attached beneath its attachment point. The first fragment placed must be
 *       the one that contains the overall root.</li>
 * </ol>
 *
 * @author jus7n
 */
final class FixedParentHierarchySolver {
	private FixedParentHierarchySolver() {}

	/**
	 * @param rootName
	 * 		Root of the completed tree. It must be a fixed type without a parent.
	 * @param fixedTypes
	 * 		Types whose direct parent is immutable, including {@code rootName}.
	 * @param fixedParents
	 * 		Immutable direct parent for every fixed type except {@code rootName}.
	 * @param freeTypes
	 * 		Types whose direct parent may be chosen. No type may be both fixed and free.
	 * @param positive
	 * 		Required strict subtype relations.
	 * @param negative
	 * 		Forbidden strict subtype relations.
	 *
	 * @return A completion satisfying every relation, or {@code null} when none exists or the input is malformed.
	 */
	@Nullable
	public static Result solve(@Nonnull String rootName,
	                           @Nonnull Set<String> fixedTypes,
	                           @Nonnull Map<String, String> fixedParents,
	                           @Nonnull Set<String> freeTypes,
	                           @Nonnull Collection<Relation> positive,
	                           @Nonnull Collection<Relation> negative) {
		// Index every named type. Sorted names make the solution independent of input iteration order.
		TreeSet<String> typeNames = new TreeSet<>();
		typeNames.add(rootName);
		typeNames.addAll(fixedTypes);
		typeNames.addAll(freeTypes);
		typeNames.addAll(fixedParents.values());
		for (Relation relation : positive) {
			typeNames.add(relation.subtype());
			typeNames.add(relation.supertype());
		}
		for (Relation relation : negative) {
			typeNames.add(relation.subtype());
			typeNames.add(relation.supertype());
		}

		List<String> names = new ArrayList<>(typeNames);
		Map<String, Integer> indexByName = new HashMap<>(names.size() * 2);
		for (int i = 0; i < names.size(); i++)
			indexByName.put(names.get(i), i);

		Integer root = indexByName.get(rootName);
		if (root == null || !fixedTypes.contains(rootName) || fixedParents.containsKey(rootName))
			return null;

		// Every type must be exactly one of fixed or free, and every fixed type except the root needs a parent.
		int size = names.size();
		int[] parent = new int[size];
		Arrays.fill(parent, -1);
		for (String typeName : names) {
			boolean fixed = fixedTypes.contains(typeName);
			boolean free = freeTypes.contains(typeName);
			if (fixed == free)
				return null;
			if (typeName.equals(rootName))
				continue;
			if (fixed) {
				String parentName = fixedParents.get(typeName);
				if (parentName == null)
					return null;
				Integer parentIndex = indexByName.get(parentName);
				if (parentIndex == null)
					return null;
				parent[indexByName.get(typeName)] = parentIndex;
			} else if (fixedParents.containsKey(typeName)) {
				return null;
			}
		}

		// Build the fixed forest, and check that it is valid.
		// A cycle or a missing parent means the input is malformed.
		Model model = new Model(names, indexByName, parent);
		if (!model.isValidFixedForest())
			return null;

		// Decide relations within a fragment, and lift the rest to start at fragment roots.
		ArrayList<Pair> normalizedPositive = new ArrayList<>(positive.size());
		ArrayList<Pair> normalizedNegative = new ArrayList<>(negative.size());
		if (!normalize(positive, true, model, normalizedPositive)
				|| !normalize(negative, false, model, normalizedNegative))
			return null;

		// Eliminate free fragments. The first task must place the root's fragment on top.
		// Every later task is one component, which is solved alone and hung below its attachment point.
		BitSet allFragments = new BitSet(size);
		for (int fragmentRoot : model.fragmentRoots)
			allFragments.set(fragmentRoot);
		Deque<Task> pending = new ArrayDeque<>();
		pending.push(new Task(allFragments, normalizedPositive, normalizedNegative, model.fragmentOf[root], -1));
		Scratch scratch = new Scratch(size);

		// Work through the tasks, placing one free fragment at a time.
		// Each task is a component of the remaining fragments.
		while (!pending.isEmpty()) {
			// Get first free fragment, and push the components that hang below it as new tasks.
			Task task = pending.pop();
			Choice choice = findFreeFragment(task, model, scratch);
			if (choice == null)
				return null;

			// Place the free fragment on top.
			// If it is required to hang below a type, check that it is not already placed.
			int selectedRoot = choice.fragmentRoot;
			if (task.attachTo >= 0) {
				if (selectedRoot == root || parent[selectedRoot] >= 0)
					return null;
				parent[selectedRoot] = task.attachTo;
			}

			// Queue the components that hang below the placed fragment.
			// Each is solved independently.
			for (ChildTask child : choice.children)
				pending.push(new Task(child.fragments, child.positive, child.negative, -1, child.attachTo));
		}

		// Check that the completed tree is valid and satisfies every relation.
		// A cycle or a type outside the root's fragment means the task is impossible.
		int[][] completionIntervals = Model.intervals(size, parent);
		if (completionIntervals == null)
			return null;

		// Check that every type is in the root's fragment, and that every relation holds as expected.
		int[] completionEntry = completionIntervals[0];
		int[] completionExit = completionIntervals[1];
		for (int i = 0; i < size; i++)
			if (i != root && !(completionEntry[root] <= completionEntry[i] && completionExit[i] <= completionExit[root]))
				return null;
		if (!satisfies(positive, indexByName, completionEntry, completionExit, true)
				|| !satisfies(negative, indexByName, completionEntry, completionExit, false))
			return null;

		// Build the result map from type name to parent name, skipping the root.
		Map<String, String> parentByType = new HashMap<>(size * 2);
		for (int i = 0; i < size; i++) {
			if (i == root)
				continue;
			if (parent[i] < 0)
				return null;
			parentByType.put(names.get(i), names.get(parent[i]));
		}
		return new Result(Map.copyOf(parentByType));
	}

	/**
	 * Checks relations within one fragment, whose ancestry is fixed, and lifts the subtype of every other relation to
	 * its fragment root. A path from a type to anything outside its fragment must leave through the fragment root, so
	 * the lifted relation holds exactly when the original does.
	 *
	 * @param relations
	 * 		Relations to normalize.
	 * @param positive
	 *        {@code true} for required relations, {@code false} for forbidden ones.
	 * @param model
	 * 		Fixed forest.
	 * @param normalized
	 * 		Output list for the lifted relations.
	 *
	 * @return {@code false} when a relation within a fragment is violated, or names an unknown type.
	 */
	private static boolean normalize(@Nonnull Collection<Relation> relations,
	                                 boolean positive,
	                                 @Nonnull Model model,
	                                 @Nonnull List<Pair> normalized) {
		for (Relation relation : relations) {
			// Check that both types are known.
			Integer source = model.indexByName.get(relation.subtype());
			Integer target = model.indexByName.get(relation.supertype());
			if (source == null || target == null)
				return false;

			// For required relations, a type cannot be a subtype of itself.
			// For forbidden relations, it is always satisfied.
			if (source.equals(target)) {
				if (positive)
					return false;
				continue;
			}

			// Relations within a fragment are already fixed. Check that they hold as expected.
			// Ancestry is immutable, so a relation that fails cannot be satisfied later.
			if (model.fragmentOf[source] == model.fragmentOf[target]) {
				boolean holds = model.isAncestor(target, source);
				if (holds != positive)
					return false;
				continue;
			}

			// Lift the relation to the fragment root of the subtype.
			// A path from a type to anything outside its fragment
			int liftedSource = model.fragmentOf[source];
			normalized.add(new Pair(liftedSource, target));
		}
		return true;
	}

	/**
	 * @param task
	 * 		Remaining fragments and their relations.
	 * @param model
	 * 		Fixed forest.
	 * @param scratch
	 * 		Reusable buffers.
	 *
	 * @return The first free fragment in the task, with the components to solve below it, or {@code null} when no
	 * fragment is free, so the task has no solution. A task with a forced root only considers that fragment.
	 */
	@Nullable
	private static Choice findFreeFragment(@Nonnull Task task, @Nonnull Model model, @Nonnull Scratch scratch) {
		// A fragment required to be below another remaining fragment cannot be on top.
		boolean[] hasPositiveOutEdge = scratch.hasPositiveOutEdge;
		try {
			// Mark every fragment that is required to be below another remaining fragment.
			for (Pair relation : task.positive) {
				int sourceFragment = model.fragmentOf[relation.source];
				int targetFragment = model.fragmentOf[relation.target];
				if (sourceFragment != targetFragment)
					hasPositiveOutEdge[sourceFragment] = true;
			}

			// Test every remaining fragment that is not required to be below another. The first free one is returned.
			for (int fragmentRoot = task.fragments.nextSetBit(0); fragmentRoot >= 0;
			     fragmentRoot = task.fragments.nextSetBit(fragmentRoot + 1)) {

				// Skip fragments that are required to be below another remaining fragment
				if (hasPositiveOutEdge[fragmentRoot])
					continue;
				// or that are not the forced root.
				if (task.forcedRoot >= 0 && fragmentRoot != task.forcedRoot)
					continue;

				// If the fragment is free, return it with the components that hang below it.
				Choice choice = testFreeFragment(task, model, fragmentRoot, scratch);
				if (choice != null)
					return choice;
			}
			return null;
		} finally {
			// Clear the scratch buffer for the next candidate test.
			for (Pair relation : task.positive)
				hasPositiveOutEdge[model.fragmentOf[relation.source]] = false;
		}
	}

	/**
	 * Tests one candidate, then restores the scratch buffers it used.
	 *
	 * @param task
	 * 		Remaining fragments and their relations.
	 * @param model
	 * 		Fixed forest.
	 * @param candidate
	 * 		Root of the fragment to test.
	 * @param scratch
	 * 		Reusable buffers.
	 *
	 * @return The candidate with its components, or {@code null} when it is not free.
	 */
	@Nullable
	private static Choice testFreeFragment(@Nonnull Task task, @Nonnull Model model, int candidate,
	                                       @Nonnull Scratch scratch) {
		ArrayList<Integer>[] adjacency = scratch.adjacency;
		int[] componentOf = scratch.componentOf;
		try {
			return testFreeFragmentWith(task, model, candidate, adjacency, componentOf);
		} finally {
			// Clear the scratch buffers for the next candidate test.
			for (Pair relation : task.positive) {
				adjacency[model.fragmentOf[relation.source]] = null;
				adjacency[model.fragmentOf[relation.target]] = null;
			}
			for (int fragmentRoot = task.fragments.nextSetBit(0); fragmentRoot >= 0;
			     fragmentRoot = task.fragments.nextSetBit(fragmentRoot + 1))
				componentOf[fragmentRoot] = -1;
		}
	}

	/**
	 * Tests whether a fragment can be placed on top of the remaining fragments.
	 *
	 * @param task
	 * 		Remaining fragments and their relations.
	 * @param model
	 * 		Fixed forest.
	 * @param candidate
	 * 		Root of the fragment to test.
	 * @param adjacency
	 * 		Cleared adjacency buffer, indexed by fragment root.
	 * @param componentOf
	 * 		Buffer of {@code -1} entries, indexed by fragment root.
	 *
	 * @return The candidate with its components, or {@code null} when it is not free.
	 */
	@Nullable
	private static Choice testFreeFragmentWith(@Nonnull Task task, @Nonnull Model model, int candidate,
	                                           @Nonnull ArrayList<Integer>[] adjacency, @Nonnull int[] componentOf) {
		// Build the adjacency graph of the remaining fragments, ignoring the candidate. Each edge is a required relation.
		for (Pair relation : task.positive) {
			int sourceFragment = model.fragmentOf[relation.source];
			int targetFragment = model.fragmentOf[relation.target];
			if (sourceFragment == candidate || targetFragment == candidate)
				continue;
			if (adjacency[sourceFragment] == null)
				adjacency[sourceFragment] = new ArrayList<>();
			if (adjacency[targetFragment] == null)
				adjacency[targetFragment] = new ArrayList<>();
			adjacency[sourceFragment].add(targetFragment);
			adjacency[targetFragment].add(sourceFragment);
		}

		// Without the candidate, the other fragments split into connected components. Each must hang below one point.
		ArrayList<BitSet> components = new ArrayList<>();
		ArrayDeque<Integer> queue = new ArrayDeque<>();
		for (int fragmentRoot = task.fragments.nextSetBit(0); fragmentRoot >= 0;
		     fragmentRoot = task.fragments.nextSetBit(fragmentRoot + 1)) {
			if (fragmentRoot == candidate || componentOf[fragmentRoot] >= 0)
				continue;

			// BFS to find all fragments in the same component as the current fragment root.
			int componentIndex = components.size();
			BitSet component = new BitSet(model.names.size());
			components.add(component);
			componentOf[fragmentRoot] = componentIndex;
			component.set(fragmentRoot);
			queue.add(fragmentRoot);
			while (!queue.isEmpty()) {
				int current = queue.removeFirst();
				ArrayList<Integer> neighbors = adjacency[current];
				if (neighbors == null)
					continue;
				for (int neighbor : neighbors) {
					if (!task.fragments.get(neighbor) || componentOf[neighbor] >= 0)
						continue;
					componentOf[neighbor] = componentIndex;
					component.set(neighbor);
					queue.addLast(neighbor);
				}
			}
		}

		// A component's targets in the candidate must lie on one path from its root. The deepest is the attachment point.
		// A target that is neither above nor below the current deepest one means the candidate is not free.
		int[] deepestTarget = new int[components.size()];
		Arrays.fill(deepestTarget, -1);
		for (Pair relation : task.positive) {
			// Skip relations that do not go from a component to the candidate.
			int sourceFragment = model.fragmentOf[relation.source];
			int targetFragment = model.fragmentOf[relation.target];
			if (targetFragment != candidate || sourceFragment == candidate)
				continue;

			// -1 component means the source fragment is not in the task,
			// so it is already placed and cannot be below the candidate.
			int component = componentOf[sourceFragment];
			if (component < 0)
				return null;

			// The deepest target is the attachment point for the component.
			// It must be an ancestor of every other target.
			int previous = deepestTarget[component];
			if (previous < 0) {
				deepestTarget[component] = relation.target;
			} else if (model.isAncestor(previous, relation.target)) {
				deepestTarget[component] = relation.target;
			} else if (!model.isAncestor(relation.target, previous)) {
				return null;
			}
		}

		// Attaching a component makes it a subtype of the attachment point and everything above it. A component member
		// forbidden from being below any of those means the candidate is not free.
		for (Pair relation : task.negative) {
			// Skip relations that do not go from a component to the candidate.
			int sourceFragment = model.fragmentOf[relation.source];
			int targetFragment = model.fragmentOf[relation.target];
			if (sourceFragment == candidate || targetFragment != candidate)
				continue;

			// Same -1 condition as above.
			int component = componentOf[sourceFragment];
			if (component < 0)
				return null;

			// The deepest target is the attachment point for the component.
			// It must not be an ancestor of the forbidden target.
			int target = deepestTarget[component] < 0 ? candidate : deepestTarget[component];
			if (model.isAncestor(relation.target, target))
				return null;
		}

		// The candidate is free. Relations between different components are always satisfied once each hangs separately,
		// so a relation is passed on only when both endpoints are in the same component. One pass per relation list.
		int componentCount = components.size();
		ArrayList<ChildTask> children = new ArrayList<>(componentCount);
		ArrayList<ArrayList<Pair>> positiveByComponent = new ArrayList<>(componentCount);
		ArrayList<ArrayList<Pair>> negativeByComponent = new ArrayList<>(componentCount);
		for (int i = 0; i < componentCount; i++) {
			positiveByComponent.add(new ArrayList<>());
			negativeByComponent.add(new ArrayList<>());
		}

		// Positive relations are required to hold, so they are passed on only when both endpoints are in the same component.
		for (Pair relation : task.positive) {
			int component = componentOf[model.fragmentOf[relation.source]];
			if (component >= 0 && component == componentOf[model.fragmentOf[relation.target]])
				positiveByComponent.get(component).add(relation);
		}

		// Negative relations are forbidden, so they are passed on only when both endpoints are in the same component.
		for (Pair relation : task.negative) {
			int component = componentOf[model.fragmentOf[relation.source]];
			if (component >= 0 && component == componentOf[model.fragmentOf[relation.target]])
				negativeByComponent.get(component).add(relation);
		}

		// Build the child tasks for each component, with the attachment point in the candidate.
		for (int i = 0; i < componentCount; i++) {
			int attachTo = deepestTarget[i] < 0 ? candidate : deepestTarget[i];
			children.add(new ChildTask(components.get(i), positiveByComponent.get(i), negativeByComponent.get(i), attachTo));
		}

		return new Choice(candidate, children);
	}

	/**
	 * @param relations
	 * 		Relations to check.
	 * @param indexByName
	 * 		Index of every type.
	 * @param entry
	 * 		Depth-first entry time of every type in the completion.
	 * @param exit
	 * 		Depth-first exit time of every type in the completion.
	 * @param expected
	 *        {@code true} when every relation must hold, {@code false} when none may.
	 *
	 * @return {@code true} when every relation holds or fails as expected.
	 */
	private static boolean satisfies(@Nonnull Collection<Relation> relations,
	                                 @Nonnull Map<String, Integer> indexByName,
	                                 @Nonnull int[] entry,
	                                 @Nonnull int[] exit,
	                                 boolean expected) {
		for (Relation relation : relations) {
			int source = indexByName.get(relation.subtype());
			int target = indexByName.get(relation.supertype());
			boolean holds = source != target && entry[target] <= entry[source] && exit[source] <= exit[target];
			if (holds != expected)
				return false;
		}
		return true;
	}

	/**
	 * A relation by type index.
	 *
	 * @param source
	 * 		Subtype index. After normalization this is a fragment root.
	 * @param target
	 * 		Supertype index.
	 */
	private record Pair(int source, int target) {}

	/**
	 * Per-solve buffers reused across candidate tests. Entries are restored to their initial state after each use,
	 * so no candidate test allocates arrays sized to the whole type universe.
	 */
	private static final class Scratch {
		private final boolean[] hasPositiveOutEdge;
		private final int[] componentOf;
		private final ArrayList<Integer>[] adjacency;

		@SuppressWarnings("unchecked")
		private Scratch(int size) {
			hasPositiveOutEdge = new boolean[size];
			componentOf = new int[size];
			Arrays.fill(componentOf, -1);
			adjacency = (ArrayList<Integer>[]) new ArrayList<?>[size];
		}
	}

	/**
	 * A set of fragments to place, with the relations among them.
	 *
	 * @param fragments
	 * 		Roots of the fragments.
	 * @param positive
	 * 		Required relations with both endpoints in the fragments.
	 * @param negative
	 * 		Forbidden relations with both endpoints in the fragments.
	 * @param forcedRoot
	 * 		Root of the only fragment allowed on top, or {@code -1} for any.
	 * @param attachTo
	 * 		Type below which the top fragment hangs, or {@code -1} for the overall root's task.
	 */
	private record Task(@Nonnull BitSet fragments, @Nonnull List<Pair> positive, @Nonnull List<Pair> negative,
	                    int forcedRoot, int attachTo) {}

	/**
	 * A component left after placing a free fragment.
	 *
	 * @param fragments
	 * 		Roots of the component's fragments.
	 * @param positive
	 * 		Required relations within the component.
	 * @param negative
	 * 		Forbidden relations within the component.
	 * @param attachTo
	 * 		Attachment point in the placed fragment.
	 */
	private record ChildTask(@Nonnull BitSet fragments, @Nonnull List<Pair> positive, @Nonnull List<Pair> negative,
	                         int attachTo) {}

	/**
	 * @param fragmentRoot
	 * 		Root of the free fragment placed on top.
	 * @param children
	 * 		Components to solve and hang below it.
	 */
	private record Choice(int fragmentRoot, @Nonnull List<ChildTask> children) {}

	/**
	 * The fixed forest: every type's fragment, and depth-first intervals that answer fixed ancestry in constant time.
	 */
	private static final class Model {
		private final List<String> names;
		private final Map<String, Integer> indexByName;
		private final int[] fragmentOf;
		private final int[] entry;
		private final int[] exit;
		private final List<Integer> fragmentRoots;

		/**
		 * @param names
		 * 		Type names by index.
		 * @param indexByName
		 * 		Index of every type.
		 * @param parent
		 * 		Fixed parent index of every type, or {@code -1} for a fragment root.
		 */
		private Model(@Nonnull List<String> names, @Nonnull Map<String, Integer> indexByName, @Nonnull int[] parent) {
			this.names = names;
			this.indexByName = indexByName;
			int size = names.size();
			fragmentOf = new int[size];
			Arrays.fill(fragmentOf, -1);
			int[] state = new int[size];
			int[] path = new int[size];
			int[] roots = new int[size];

			// Walk up from each type to find its fragment root. A walk that meets its own path is a cycle.
			for (int start = 0; start < size; start++) {
				if (state[start] == 2)
					continue;
				int length = 0;
				int current = start;
				while (current >= 0 && state[current] == 0) {
					state[current] = 1;
					path[length++] = current;
					current = parent[current];
				}
				if (current >= 0 && state[current] == 1) {
					entry = null;
					exit = null;
					fragmentRoots = List.of();
					return;
				}
				int root = current < 0 ? path[length - 1] : roots[current];
				for (int i = length - 1; i >= 0; i--) {
					int vertex = path[i];
					roots[vertex] = root;
					state[vertex] = 2;
				}
			}
			System.arraycopy(roots, 0, fragmentOf, 0, size);

			int[][] intervals = intervals(size, parent);
			if (intervals == null) {
				entry = null;
				exit = null;
				fragmentRoots = List.of();
				return;
			}
			entry = intervals[0];
			exit = intervals[1];
			ArrayList<Integer> rootsList = new ArrayList<>();
			for (int i = 0; i < size; i++)
				if (fragmentOf[i] == i)
					rootsList.add(i);
			fragmentRoots = List.copyOf(rootsList);
		}

		/**
		 * @return {@code true} when the fixed parents form a forest, without cycles.
		 */
		private boolean isValidFixedForest() {
			return entry != null && exit != null;
		}

		/**
		 * @return {@code true} when {@code ancestor} is {@code descendant} or above it through fixed parents.
		 */
		private boolean isAncestor(int ancestor, int descendant) {
			return entry[ancestor] <= entry[descendant] && exit[descendant] <= exit[ancestor];
		}

		/**
		 * Iterative depth-first traversal of a parent forest. A type is an ancestor of another exactly when its
		 * interval contains the other's.
		 *
		 * @param size
		 * 		Number of types.
		 * @param parent
		 * 		Parent index of every type, or {@code -1} for a root.
		 *
		 * @return Entry and exit times of every type, or {@code null} when the parents do not form a forest.
		 */
		@Nullable
		@SuppressWarnings("unchecked")
		private static int[][] intervals(int size, @Nonnull int[] parent) {
			// Build the adjacency list of the parent forest.
			// A cycle or a missing parent means the input is malformed.
			List<Integer>[] children = (ArrayList<Integer>[]) new ArrayList<?>[size];
			int rootCount = 0;
			for (int vertex = 0; vertex < size; vertex++) {
				int parentVertex = parent[vertex];
				if (parentVertex < 0) {
					rootCount++;
					continue;
				}
				if (parentVertex >= size || parentVertex == vertex)
					return null;
				if (children[parentVertex] == null)
					children[parentVertex] = new ArrayList<>();
				children[parentVertex].add(vertex);
			}

			int[] entry = new int[size];
			int[] exit = new int[size];
			int[] stack = new int[size];
			int[] nextChild = new int[size];
			Arrays.fill(entry, -1);

			// Iterative depth-first traversal of the forest, recording entry and exit times.
			// A type is an ancestor of another exactly when its interval contains the other's.
			int time = 0;
			int visited = 0;
			for (int root = 0; root < size; root++) {
				if (parent[root] >= 0)
					continue;

				int depth = 0;
				stack[0] = root;
				nextChild[0] = 0;
				entry[root] = time++;
				visited++;
				while (depth >= 0) {
					// If there are children left to visit, go to the next one.
					// Otherwise, exit the current type and backtrack.
					int current = stack[depth];
					List<Integer> currentChildren = children[current];
					if (currentChildren != null && nextChild[depth] < currentChildren.size()) {
						int child = currentChildren.get(nextChild[depth]++);
						if (entry[child] >= 0)
							return null;
						entry[child] = time++;
						visited++;
						stack[++depth] = child;
						nextChild[depth] = 0;
					} else {
						exit[current] = time++;
						depth--;
					}
				}
			}
			if (visited != size || rootCount == 0)
				return null;
			return new int[][]{entry, exit};
		}
	}

	/**
	 * A strict subtype requirement or prohibition.
	 *
	 * @param subtype
	 * 		Potential descendant.
	 * @param supertype
	 * 		Potential ancestor.
	 */
	record Relation(@Nonnull String subtype, @Nonnull String supertype) {}

	/**
	 * @param parentByType
	 * 		Direct parent of every type except the root, both fixed and chosen.
	 */
	record Result(@Nonnull Map<String, String> parentByType) {}
}
