package me.coley.recaf;

import me.coley.recaf.bytecode.analysis.*;
import me.coley.recaf.graph.SearchResult;
import org.junit.jupiter.api.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for class hierarchy graph.
 *
 * @author Matt
 */
public class HierarchyTest {
	private Hierarchy graph;

	@BeforeEach
	public void setup() throws IOException {
		ClassLoader classLoader = HierarchyTest.class.getClassLoader();
		File file = new File(classLoader.getResource("inherit.jar").getFile());
		Input input = new Input(file);
		graph = new Hierarchy(input);
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
		ClassVertex root = graph.getRoot("test/Person");
		ClassVertex target = graph.getRoot("test/Yoda");
		SearchResult<ClassNode> result = new ClassDfsSearch(ClassDfsSearch.Type.CHILDREN).find(root, target);
		if (result != null) {
			String[] expectedPath = new String[] {"test/Person", "test/Jedi", "test/Yoda"};
			String[] actualPath = result.getPath().stream()
					.map(v -> v.getData().name).toArray(String[]::new);
			assertArrayEquals(expectedPath, actualPath);
		} else {
			fail("no path");
		}
	}

	@Test
	public void testChildToParentSearch() {
		ClassVertex root = graph.getRoot("test/Yoda");
		ClassVertex target = graph.getRoot("test/Person");
		SearchResult<ClassNode> result = new ClassDfsSearch(ClassDfsSearch.Type.PARENTS).find(root, target);
		if (result != null) {
			String[] expectedPath = new String[] {"test/Yoda", "test/Jedi", "test/Person"};
			String[] actualPath = result.getPath().stream()
					.map(v -> v.getData().name).toArray(String[]::new);
			assertArrayEquals(expectedPath, actualPath);
		} else {
			fail("no path");
		}
	}

	@Test
	public void testHierarchyBuilder() {
		ClassVertex root = graph.getRoot("test/Yoda");
		ClassHierarchyBuilder builder = new ClassHierarchyBuilder();
		Set<ClassVertex> hierarchy = builder.build(root);
		// Almost all names should be discovered in the hierarchy for this test case.
		// Sith and Jedi for example, share the same parent "Person".
		Set<String> expected = new HashSet<>(Arrays.asList(
				"test/Deal", "test/Absolutes",
				"test/Person", "test/Greetings",
				"test/Jedi", "test/Sith",
				"test/Yoda", "java/lang/Object"));
		assertEquals(expected.size(), hierarchy.stream()
				.filter(v -> expected.contains(v.getData().name))
				.count());
		// Only class that should NOT be in there.
		// Its used as a reference but is never inherited / extended by anything in Yoda's hierarhcy.
		assertFalse(hierarchy.stream().anyMatch(v -> v.getData().name.equals("test/Speech")));
		assertFalse(hierarchy.stream().anyMatch(v -> v.getData().name.equals("test/Ability")));
	}
}
