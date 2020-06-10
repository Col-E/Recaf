package me.coley.recaf;

import me.coley.recaf.graph.*;
import me.coley.recaf.graph.flow.FlowGraph;
import me.coley.recaf.graph.flow.FlowVertex;
import me.coley.recaf.workspace.JarResource;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for method flow graph.
 *
 * @author Matt
 */
public class FlowGraphTest extends Base {
	private FlowGraph graph;

	@BeforeEach
	public void setup() throws IOException {
		Path file = getClasspathFile("calls.jar");
		Workspace workspace = new Workspace(new JarResource(file));
		graph = workspace.getFlowGraph();
	}

	@Test
	public void testSimpleOutbound() {
		// Just a "System.out.println", so we need the "println" call
		FlowVertex one = graph.getVertex("test/Parent", "thing", "()V");
		assertEquals(1, one.getEdges().size());
		FlowVertex other = (FlowVertex) one.getEdges().iterator().next().getOther(one);
		assertEquals("java/io/PrintStream", other.getOwner());
		assertEquals("println", other.getName());
		assertEquals("(Ljava/lang/String;)V", other.getDesc());
	}

	@Test
	public void testRecursive() {
		// Recursive call to self should link to the same vertex
		FlowVertex count = graph.getVertex("test/Recursion", "countTo10", "(I)V");
		FlowVertex calledCount = getSingleEdgeOther(count);
		assertEquals(count, calledCount);
	}

	@Test
	public void testChain() {
		// one -> two --> three
		FlowVertex one = graph.getVertex("test/Chain", "one", "()V");
		FlowVertex two = graph.getVertex("test/Chain", "two", "()V");
		FlowVertex three = graph.getVertex("test/Chain", "three", "()V");
		// Use search to show the path following the chain
		SearchResult<ClassReader> result  = new ClassDfsSearch(ClassDfsSearch.Type.ALL).find(one, three);
		assertNotNull(result);
		List<Vertex<ClassReader>> path = result.getPath();
		assertEquals(3, path.size());
		assertEquals(one, path.get(0));
		assertEquals(two, path.get(1));
		assertEquals(three, path.get(2));
	}

	@Test
	public void testLoopback() {
		// one -> two --> three --> one
		FlowVertex one = graph.getVertex("test/Loopback", "one", "()V");
		FlowVertex two = graph.getVertex("test/Loopback", "two", "()V");
		FlowVertex three = graph.getVertex("test/Loopback", "three", "()V");
		// Show that the edges point to the next expected vertex
		FlowVertex oneEdge = getSingleEdgeOther(one);
		assertEquals(two, oneEdge);
		FlowVertex twoEdge = getSingleEdgeOther(two);
		assertEquals(three, twoEdge);
		FlowVertex threeEdge = getSingleEdgeOther(three);
		assertEquals(one, threeEdge);
	}

	@Test
	public void testChildCallsParent() {
		// Child extends Parent
		// Method calls "super.doThing"
		FlowVertex parentThing = graph.getVertex("test/Parent", "thing", "()V");
		FlowVertex callsParent = graph.getVertex("test/Child", "callParentThing", "()V");
		FlowVertex calledParent = getSingleEdgeOther(callsParent);
		assertEquals(parentThing, calledParent);
	}

	@Test
	public void testChildCallsInterface() {
		// Child implements Interface
		// Method calls "Interface.super.doThing"
		FlowVertex interfaceThing = graph.getVertex("test/Interface", "thing", "()V");
		FlowVertex callsInterface = graph.getVertex("test/Child", "callInterfaceThing", "()V");
		FlowVertex calledInterface = getSingleEdgeOther(callsInterface);
		assertEquals(interfaceThing, calledInterface);
	}

	/**
	 * @param vertex
	 * 		Vertex with one edge.
	 *
	 * @return The vertex on the other end of the edge.
	 */
	private static FlowVertex getSingleEdgeOther(FlowVertex vertex) {
		Set<Edge<ClassReader>> edges = vertex.getEdges();
		assertEquals(1, edges.size());
		return (FlowVertex) edges.iterator().next().getOther(vertex);
	}
}