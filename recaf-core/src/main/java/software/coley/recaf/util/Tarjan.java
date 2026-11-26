package software.coley.recaf.util;

import jakarta.annotation.Nonnull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tarjan's strongly connected components algorithm over a directed graph.
 * <p>
 * See: <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's algorithm</a>
 * <p>
 * Video: <a href="https://www.youtube.com/watch?v=wUgWX0nc4NY">WilliamFiset's Tarjan's Strongly Connected Component (SCC) Algorithm</a>
 *
 * @author Matt Coley
 */
public final class Tarjan {
	private Tarjan() {}

	/**
	 * Finds all strongly connected components in a directed graph whose node type has a natural order.
	 *
	 * @param graph
	 * 		Directed edges of each node. Edge targets that are not keys have no outgoing edges.
	 *
	 * @return Immutable list of components, with each component's members sorted by natural order and the components
	 * sorted by their first member.
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> List<List<T>> components(@Nonnull Map<T, ? extends Collection<T>> graph) {
		return components(graph, Comparator.naturalOrder());
	}

	/**
	 * Finds all strongly connected components in a directed graph.
	 *
	 * @param graph
	 * 		Directed edges of each node. Edge targets that are not keys have no outgoing edges.
	 * @param comparator
	 * 		Order used for traversal and to sort component members and components.
	 * 		Must provide a consistent total order over the graph's nodes.
	 *
	 * @return Immutable list of components, with each component's members sorted by the comparator and the components
	 * sorted by their first member.
	 */
	@Nonnull
	public static <T> List<List<T>> components(@Nonnull Map<T, ? extends Collection<T>> graph,
	                                           @Nonnull Comparator<? super T> comparator) {
		return new SCC<>(graph, comparator).compute();
	}

	/**
	 * Finds the nodes in non-trivial strongly connected components in a graph whose node type has a natural order.
	 *
	 * @param graph
	 * 		Directed edges of each node. Edge targets that are not keys have no outgoing edges.
	 *
	 * @return Map of component identifiers for every node in a component with more than one node.
	 * Two nodes share an id exactly when they belong to the same component.
	 */
	@Nonnull
	public static <T extends Comparable<? super T>> Map<T, Integer> cyclic(@Nonnull Map<T, ? extends Collection<T>> graph) {
		return cyclic(graph, Comparator.naturalOrder());
	}

	/**
	 * Finds the nodes in non-trivial strongly connected components.
	 *
	 * @param graph
	 * 		Directed edges of each node. Edge targets that are not keys have no outgoing edges.
	 * @param comparator
	 * 		Order used to sort the components and assign component identifiers.
	 * 		Must provide a consistent total order over the graph's nodes.
	 *
	 * @return Map of component identifiers for every node in a component with more than one node.
	 * Two nodes share an id exactly when they belong to the same component.
	 */
	@Nonnull
	public static <T> Map<T, Integer> cyclic(@Nonnull Map<T, ? extends Collection<T>> graph,
	                                         @Nonnull Comparator<? super T> comparator) {
		Map<T, Integer> cyclic = new HashMap<>();
		List<List<T>> components = components(graph, comparator);
		for (int id = 0; id < components.size(); id++) {
			List<T> component = components.get(id);
			if (component.size() > 1)
				for (T member : component)
					cyclic.put(member, id);
		}
		return cyclic;
	}

	/**
	 * Connected component finder for a directed graph.
	 */
	private static final class SCC<T> {
		private final Map<T, Integer> index = new HashMap<>();
		private final Map<T, Integer> lowLink = new HashMap<>();
		private final Deque<T> stack = new ArrayDeque<>();
		private final Set<T> onStack = new HashSet<>();
		private final List<List<T>> components = new ArrayList<>();
		private final Map<T, ? extends Collection<T>> graph;
		private final Comparator<? super T> comparator;
		private int counter;

		/**
		 * @param graph
		 * 		Directed edges of each node.
		 * 		Edge targets that are not keys have no outgoing edges.
		 * @param comparator
		 * 		Order used for traversal and to sort component members and components.
		 * 		Must provide a consistent total order over the graph's nodes.
		 */
		private SCC(@Nonnull Map<T, ? extends Collection<T>> graph, @Nonnull Comparator<? super T> comparator) {
			this.graph = graph;
			this.comparator = comparator;
		}

		/**
		 * Computes the strongly connected components of the graph.
		 *
		 * @return Immutable list of components, with each component's members
		 * sorted by the comparator and the components  sorted by their first member.
		 */
		@Nonnull
		private List<List<T>> compute() {
			List<T> nodes = new ArrayList<>(graph.keySet());
			nodes.sort(comparator);
			for (T node : nodes)
				if (!index.containsKey(node))
					connect(node);

			components.sort((left, right) -> comparator.compare(left.getFirst(), right.getFirst()));
			return List.copyOf(components);
		}

		/**
		 * Visits a node and everything reachable from it, closing each component whose root is found.
		 *
		 * @param node
		 * 		Unvisited node.
		 */
		private void connect(@Nonnull T node) {
			index.put(node, counter);
			lowLink.put(node, counter);
			counter++;
			stack.push(node);
			onStack.add(node);

			// Sort neighbors for deterministic output, even though the order doesn't affect the resulting components.
			Collection<T> outgoing = graph.get(node);
			List<T> neighbors = outgoing == null ? List.of() : new ArrayList<>(outgoing);
			if (outgoing != null)
				neighbors.sort(comparator);

			// Explore neighbors, tracking low-link values to identify strongly connected components.
			for (T next : neighbors) {
				if (!index.containsKey(next)) {
					connect(next);
					lowLink.put(node, Math.min(lowLink.get(node), lowLink.get(next)));
				} else if (onStack.contains(next)) {
					lowLink.put(node, Math.min(lowLink.get(node), index.get(next)));
				}
			}

			// If this node is a root of a strongly connected component, pop the stack to build the component.
			if (lowLink.get(node).equals(index.get(node))) {
				List<T> members = new ArrayList<>();
				T member;
				do {
					member = stack.pop();
					onStack.remove(member);
					members.add(member);
				} while (!member.equals(node));
				members.sort(comparator);
				components.add(List.copyOf(members));
			}
		}
	}
}
