package me.coley.recaf;

import me.coley.recaf.bytecode.analysis.Hierarchy;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HierarchyTest {
	private static Hierarchy graph;

	@BeforeAll
	public static void setup() throws IOException {
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
		Set<String> descendants = graph.getAllDescendants(actualParent)
				.collect(Collectors.toSet());
		expectedChildren.forEach(child -> assertTrue(descendants.contains(child)));
	}

	@Test
	public void testParents() {
		String actualChild = "test/Yoda";
		Set<String> expectedParents = new HashSet<>(Arrays.asList(
				"test/Jedi", "test/Person", "test/Greetings", "java/lang/Object"));
		Set<String> parents = graph.getAllParents(actualChild)
				.collect(Collectors.toSet());
		expectedParents.forEach(parent -> assertTrue(parents.contains(parent)));
	}
}
