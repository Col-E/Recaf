package software.coley.recaf.services.inheritance;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.Inheritance;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link InheritanceGraph}
 */
class InheritanceGraphTest extends TestBase {
	static Workspace workspace;
	static InheritanceGraph inheritanceGraph;

	@BeforeAll
	static void setup() throws IOException {
		// Create workspace with the inheritance classes
		BasicJvmClassBundle classes = TestClassUtils.fromClasses(Inheritance.class.getClasses());
		classes.initialPut(TestClassUtils.fromRuntimeClass(StringConsumer.class));
		assertEquals(7, classes.size(), "Expecting 7 classes");
		workspace = TestClassUtils.fromBundle(classes);
		workspaceManager.setCurrent(workspace);

		// Get graph
		inheritanceGraph = recaf.get(InheritanceGraphService.class).getCurrentWorkspaceInheritanceGraph();
	}

	@Test
	void getVertex() {
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');

		// Check vertex
		InheritanceVertex vertex = inheritanceGraph.getVertex(appleName);
		assertNotNull(vertex, "Could not get Apple vertex from workspace");
		assertEquals(appleName, vertex.getName(), "Vertex should have same name as lookup");

		// Check children
		Set<InheritanceVertex> children = vertex.getChildren();
		assertEquals(1, children.size(), "Expecting 1 child for Apple (apple with worm)");
		assertTrue(children.stream()
				.map(InheritanceVertex::getName)
				.anyMatch(name -> name.equals(appleName + "WithWorm")));

		// Check parents
		Set<InheritanceVertex> parents = vertex.getParents();
		assertEquals(3, parents.size(), "Expecting 3 parents for Apple (interfaces, super is object and is ignored)");
		assertTrue(parents.stream()
						.map(InheritanceVertex::getName)
						.anyMatch(name -> name.equals(appleName.replace("Apple", "Red"))),
				"Apple missing parent: Red");
		assertTrue(parents.stream()
						.map(InheritanceVertex::getName)
						.anyMatch(name -> name.equals(appleName.replace("Apple", "Edible"))),
				"Apple missing parent: Edible");

		// Check awareness of library methods
		vertex = inheritanceGraph.getVertex(StringConsumer.class.getName().replace('.', '/'));
		assertTrue(vertex.hasMethod("accept", "(Ljava/lang/Object;)V"));
		assertTrue(vertex.hasMethod("accept", "(Ljava/lang/String;)V")); // Redirects to Object method
		assertTrue(vertex.isLibraryMethod("accept", "(Ljava/lang/Object;)V")); // From Consumer<T>
		assertFalse(vertex.isLibraryMethod("accept", "(Ljava/lang/String;)V")); // Local, doesn't matter if renamed.
	}

	@Test
	void getVertexFamily() {
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');
		Set<String> names = Set.of(appleName,
				appleName + "WithWorm", // child of apple
				appleName.replace("Apple", "Red"), // parent of apple
				appleName.replace("Apple", "Edible"), // parent of apple
				appleName.replace("Apple", "Grape") // shared parent edible
		);
		Set<InheritanceVertex> family = inheritanceGraph.getVertexFamily(appleName, false);
		assertEquals(5, family.size());
		assertEquals(names, family.stream().map(InheritanceVertex::getName).collect(Collectors.toSet()));
	}

	@Test
	void getCommon() {
		String edibleName = Inheritance.Edible.class.getName().replace('.', '/');
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');
		String grapeName = Inheritance.Grape.class.getName().replace('.', '/');

		// Compare obvious case --> edible
		String commonType = inheritanceGraph.getCommon(appleName, grapeName);
		assertEquals(edibleName, commonType, "Common type of Apple/Grape should be Edible");

		// Compare with bogus --> object
		commonType = inheritanceGraph.getCommon(appleName, UUID.randomUUID().toString());
		assertEquals(Types.OBJECT_TYPE.getInternalName(), commonType,
				"Common type of two unrelated classes should be Object");
	}

	@Test
	void isAssignableFrom() {
		String edibleName = Inheritance.Edible.class.getName().replace('.', '/');
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');
		String grapeName = Inheritance.Grape.class.getName().replace('.', '/');

		// Edible.class.isAssignableFrom(Apple.class) --> true
		assertTrue(inheritanceGraph.isAssignableFrom(edibleName, appleName), "Edible should be assignable from Apple");
		assertTrue(inheritanceGraph.isAssignableFrom(edibleName, grapeName), "Edible should be assignable from Grape");

		// Apple.class.isAssignableFrom(Edible.class) --> false
		assertFalse(inheritanceGraph.isAssignableFrom(appleName, edibleName), "Apple should not be assignable from Edible");
		assertFalse(inheritanceGraph.isAssignableFrom(grapeName, edibleName), "Grape should not be assignable from Edible");
	}

	@Test
	void getFamilyOfThrowable() {
		String notFoodExceptionName = Inheritance.NotFoodException.class.getName().replace('.', '/');
		ClassPathNode classPath = workspace.findJvmClass(notFoodExceptionName);
		assertNotNull(classPath, "Could not find class 'NotFoodException'");

		// Assert that looking at child types of throwable finds NotFoodException.
		// Our class extends Exception, which extends Throwable. So there should be a vertex between Throwable and our type.
		JvmClassInfo notFoodException = classPath.getValue().asJvmClass();
		List<ClassInfo> exceptionClasses = inheritanceGraph.getVertex("java/lang/Exception").getAllChildren().stream()
				.map(InheritanceVertex::getValue)
				.toList();
		assertTrue(exceptionClasses.contains(notFoodException), "Subtypes of 'Exception' did not yield 'NotFoodException'");
		List<ClassInfo> throwableClasses = inheritanceGraph.getVertex("java/lang/Throwable").getAllChildren().stream()
				.map(InheritanceVertex::getValue)
				.toList();
		assertTrue(throwableClasses.contains(notFoodException), "Subtypes of 'Throwable' did not yield 'NotFoodException'");
	}
}