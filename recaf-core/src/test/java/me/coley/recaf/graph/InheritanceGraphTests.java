package me.coley.recaf.graph;

import me.coley.recaf.TestUtils;
import me.coley.recaf.workspace.Workspace;
import me.coley.recaf.workspace.resource.Resource;
import me.coley.recaf.workspace.resource.Resources;
import me.coley.recaf.workspace.resource.source.JarContentSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class InheritanceGraphTests extends TestUtils {
	@Test
	void testEquality() throws IOException {
		InheritanceGraph graph1 = createGraph("Sample.jar");
		InheritanceVertex vertex1 = graph1.getVertex("game/SnakeModel");
		InheritanceGraph graph2 = createGraph("Sample.zip");
		InheritanceVertex vertex2 = graph2.getVertex("game/SnakeModel");
		assertNotNull(vertex1);
		assertNotNull(vertex2);
		assertEquals(vertex1, vertex2);
	}

	@Test
	void testFamily() throws IOException {
		InheritanceGraph graph = createGraph("Sample.jar");
		Set<String> expectedFamily = Set.of(
				"game/AbstractModel",
				"game/SnakeModel",
				"game/WorldModel",
				"java/lang/Object"
		);
		for (String member : expectedFamily) {
			if (member.equals("java/lang/Object"))
				continue;
			Set<String> snakeModelFamily = graph.getVertex(member).getFamily().stream()
					.map(InheritanceVertex::getName)
					.collect(Collectors.toSet());
			assertEquals(expectedFamily, snakeModelFamily);
		}
	}

	@Test
	@Disabled
	void testLibraryMethod() throws IOException {
		// TODO: Create an jar that:
		//  - Has one class override a method defined in another
		//  - Can tell that the methods are in the same hierarchy
		//  - It should not be a library method

		// TODO: Create a jar that:
		//  - Has one class override a method defined in "java.base" interface
		//  - Can tell that the methods are in the same hierarchy
		//  - It should be marked as a library method, since the base definition does not belong to our resource
	}

	private InheritanceGraph createGraph(String fileName) throws IOException {
		Resource primary = new Resource(new JarContentSource(sourcesDir.resolve(fileName)));
		primary.read();
		Workspace workspace = new Workspace(new Resources(primary));
		return new InheritanceGraph(workspace);
	}
}
