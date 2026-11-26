package software.coley.recaf.util;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link Tarjan}.
 */
class TarjanTest {
	@Test
	void componentsAndCyclicSupportNaturallyOrderedNodes() {
		// Graph shape:
		//
		// 1 <----> 2
		// 3 <----> 4
		Map<Integer, Collection<Integer>> graph = new HashMap<>();
		graph.put(4, List.of(3));
		graph.put(3, List.of(4));
		graph.put(2, List.of(1));
		graph.put(1, List.of(2));

		// Components are sorted by their first member, and members are sorted by natural order.
		assertEquals(List.of(
				List.of(1, 2),
				List.of(3, 4)), Tarjan.components(graph));
		assertEquals(Map.of(
				1, 0,
				2, 0,
				3, 1,
				4, 1), Tarjan.cyclic(graph));
	}

	@Test
	void componentsAndCyclicSupportNodesWithAnExplicitOrder() {
		// Graph shape:
		//
		// a <--------> b
		// c <--self--> c
		// d
		Node a = new Node("a");
		Node b = new Node("b");
		Node c = new Node("c");
		Node d = new Node("d");
		Map<Node, Collection<Node>> graph = new HashMap<>();
		graph.put(d, List.of());
		graph.put(c, List.of(c));
		graph.put(b, List.of(a));
		graph.put(a, List.of(b));
		Comparator<Node> order = Comparator.comparing(Node::name);

		// Components are sorted by their first member, and members are sorted by the provided order.
		assertEquals(List.of(
				List.of(a, b),
				List.of(c),
				List.of(d)), Tarjan.components(graph, order));
		assertEquals(Map.of(
				a, 0,
				b, 0), Tarjan.cyclic(graph, order));
	}

	private record Node(@Nonnull String name) {}
}
