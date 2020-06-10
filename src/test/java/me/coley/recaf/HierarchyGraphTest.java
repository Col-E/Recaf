package me.coley.recaf;

import me.coley.recaf.graph.ClassDfsSearch;
import me.coley.recaf.graph.SearchResult;
import me.coley.recaf.graph.inheritance.*;
import me.coley.recaf.workspace.JarResource;
import me.coley.recaf.workspace.Workspace;
import org.junit.jupiter.api.*;
import org.objectweb.asm.ClassReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for class hierarchy graph.
 *
 * @author Matt
 */
public class HierarchyGraphTest extends Base {
	private HierarchyGraph graph;

	@BeforeEach
	public void setup() throws IOException {
		Path file = getClasspathFile("inherit.jar");
		Workspace workspace = new Workspace(new JarResource(file));
		graph = workspace.getHierarchyGraph();
	}


	@Test
	public void testDescendants() {
		String actualParent = "test/Greetings";
		Set<String> expectedChildren = new HashSet<>(Arrays.asList(
				"test/Person", "test/Jedi", "test/Sith", "test/Yoda"));
		Set<String> descendants = graph.getAllDescendants(actualParent).collect(Collectors.toSet());
		expectedChildren.forEach(child -> assertTrue(descendants.contains(child)));
	}

	@Test
	public void testParents() {
		String actualChild = "test/Yoda";
		Set<String> expectedParents = new HashSet<>(Arrays.asList(
				"test/Jedi", "test/Person", "test/Greetings", "java/lang/Object"));
		Set<String> parents = graph.getAllParents(actualChild).collect(Collectors.toSet());
		expectedParents.forEach(parent -> assertTrue(parents.contains(parent)));
	}

	@Test
	public void testParentToChildSearch() {
		HierarchyVertex vertex = graph.getVertex("test/Person");
		HierarchyVertex target = graph.getVertex("test/Yoda");
		SearchResult<ClassReader> result = new ClassDfsSearch(ClassDfsSearch.Type.CHILDREN).find(vertex, target);
		if (result != null) {
			String[] expectedPath = new String[] {"test/Person", "test/Jedi", "test/Yoda"};
			String[] actualPath = result.getPath().stream()
					.map(v -> v.getData().getClassName()).toArray(String[]::new);
			assertArrayEquals(expectedPath, actualPath);
		} else {
			fail("no path");
		}
	}

	@Test
	public void testFindCommon() {
		assertEquals("test/Person", graph.getCommon("test/Jedi", "test/Sith"));
		assertEquals("test/Person", graph.getCommon("test/Jedi", "test/Person"));
		assertEquals("test/Person", graph.getCommon("test/Person", "test/Person"));
	}

	@Test
	public void testChildToParentSearch() {
		HierarchyVertex vertex = graph.getVertex("test/Yoda");
		HierarchyVertex target = graph.getVertex("test/Person");
		SearchResult<ClassReader> result = new ClassDfsSearch(ClassDfsSearch.Type.PARENTS).find(vertex, target);
		if (result != null) {
			String[] expectedPath = new String[] {"test/Yoda", "test/Jedi", "test/Person"};
			String[] actualPath = result.getPath().stream()
					.map(v -> v.getData().getClassName()).toArray(String[]::new);
			assertArrayEquals(expectedPath, actualPath);
		} else {
			fail("no path");
		}
	}

	@Test
	public void testHierarchyBuilder() {
		HierarchyVertex vertex = graph.getVertex("test/Yoda");
		Set<HierarchyVertex> hierarchy = graph.getHierarchy(vertex);
		// Almost all names should be discovered in the hierarchy for this test case.
		// Sith and Jedi for example, share the same parent "Person".
		Set<String> expected = new HashSet<>(Arrays.asList(
				"test/Deal", "test/Absolutes",
				"test/Person", "test/Greetings",
				"test/Jedi", "test/Sith",
				"test/Yoda", "java/lang/Object"));
		assertEquals(expected.size(), hierarchy.stream()
				.filter(v -> expected.contains(v.getData().getClassName()))
				.count());
		// Only two classes that should NOT be in there.
		// Never used
		assertFalse(hierarchy.stream().anyMatch(v -> v.getData().getClassName().equals("test/Speech")));
		// Referenced as field, never inherited
		assertFalse(hierarchy.stream().anyMatch(v -> v.getData().getClassName().equals("test/Ability")));
	}

	@Test
	public void testIsLibrary() {
		// The "say" method is defined only by classes in the input.
		// - It is not a "library" method.
		assertFalse(graph.isLibrary("test/Yoda", "say", "()V"));
		// The "toString" method belongs to "java/lang/Object" which is not in the input.
		// - It is a "library" method.
		assertTrue(graph.isLibrary("test/Yoda", "toString", "()Ljava/lang/String;"));
	}

	@Test
	public void testAreLinked() {
		// Yoda -> Jedi -> Person -> Greetings
		assertTrue(graph.areLinked("test/Yoda", "say", "()V", "test/Greetings", "say", "()V"));
		// No path between Yoda and Speech
		assertFalse(graph.areLinked("test/Yoda", "say", "()V", "test/Speech", "say", "()V"));
	}
}