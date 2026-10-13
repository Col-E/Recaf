package software.coley.recaf.services.phantom.analysis;

import jakarta.annotation.Nonnull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tarjan's strongly connected components algorithm over a graph of type names.
 * <p>
 * See: <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's algorithm</a>
 * <p>
 * Video: <a href="https://www.youtube.com/watch?v=wUgWX0nc4NY">WilliamFiset's Tarjan's Strongly Connected Component (SCC) Algorithm</a>
 * @author Matt Coley
 */
final class StronglyConnectedComponents {
	private final Map<String, Integer> index = new HashMap<>();
	private final Map<String, Integer> lowLink = new HashMap<>();
	private final Deque<String> stack = new ArrayDeque<>();
	private final Set<String> onStack = new HashSet<>();
	private final Map<String, Integer> componentOf = new HashMap<>();
	private final Map<Integer, Integer> componentSize = new HashMap<>();
	private final Map<String, List<String>> graph;
	private int counter;
	private int components;

	private StronglyConnectedComponents(@Nonnull Map<String, List<String>> graph) {
		this.graph = graph;
	}

	/**
	 * @param graph
	 * 		Directed edges of each node. Edge targets that are not keys have no outgoing edges.
	 *
	 * @return Map of component identifiers for every node that lies on a cycle.
	 * Two nodes share an id exactly when they are on a common cycle.
	 */
	@Nonnull
	public static Map<String, Integer> cyclic(@Nonnull Map<String, List<String>> graph) {
		StronglyConnectedComponents tarjan = new StronglyConnectedComponents(graph);
		for (String node : graph.keySet())
			if (!tarjan.index.containsKey(node))
				tarjan.connect(node);

		Map<String, Integer> cyclic = new HashMap<>();
		for (Map.Entry<String, Integer> entry : tarjan.componentOf.entrySet())
			if (tarjan.componentSize.get(entry.getValue()) > 1)
				cyclic.put(entry.getKey(), entry.getValue());

		return cyclic;
	}

	/**
	 * Visits a node and everything reachable from it, closing each component whose root is found.
	 *
	 * @param node
	 * 		Unvisited node.
	 */
	private void connect(@Nonnull String node) {
		index.put(node, counter);
		lowLink.put(node, counter);
		counter++;
		stack.push(node);
		onStack.add(node);
		for (String next : graph.getOrDefault(node, List.of())) {
			if (!index.containsKey(next)) {
				connect(next);
				lowLink.put(node, Math.min(lowLink.get(node), lowLink.get(next)));
			} else if (onStack.contains(next)) {
				lowLink.put(node, Math.min(lowLink.get(node), index.get(next)));
			}
		}

		// The node is the root of a component: everything above it on the stack belongs to that component.
		if (lowLink.get(node).equals(index.get(node))) {
			int id = components++;
			int size = 0;
			String member;
			do {
				member = stack.pop();
				onStack.remove(member);
				componentOf.put(member, id);
				size++;
			} while (!member.equals(node));
			componentSize.put(id, size);
		}
	}
}
