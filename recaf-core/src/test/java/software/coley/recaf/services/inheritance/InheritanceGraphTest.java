package software.coley.recaf.services.inheritance;

import jakarta.annotation.Nonnull;
import me.darknet.dex.tree.definitions.ClassDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.Inheritance;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.util.Types;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

/**
 * Tests for {@link InheritanceGraph}
 */
class InheritanceGraphTest extends TestBase {
	static Workspace workspace;
	static InheritanceGraph inheritanceGraph;

	@BeforeAll
	static void setup() throws IOException {
		InheritanceGraphService inheritanceGraphService = recaf.get(InheritanceGraphService.class);
		assertNotNull(inheritanceGraphService.toString()); // Bogus call to initialize the service

		// Create workspace with the inheritance classes
		BasicJvmClassBundle classes = TestClassUtils.fromClasses(Inheritance.class.getClasses());
		classes.initialPut(TestClassUtils.fromRuntimeClass(StringConsumer.class));
		assertEquals(7, classes.size(), "Expecting 7 classes");
		workspace = TestClassUtils.fromBundle(classes);
		workspaceManager.setCurrent(workspace);

		// Get graph
		inheritanceGraph = inheritanceGraphService.getCurrentWorkspaceInheritanceGraph();
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
		assertNotNull(vertex);
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
	void isLibraryMethod() {
		String stringConsumerName = StringConsumer.class.getName().replace('.', '/');

		// The bridge method defined in StringConsumer can technically be renamed since it is defined locally
		// and is not an override of the Consumer<T> method which takes Object.
		assertFalse(inheritanceGraph.isLibraryMethod(stringConsumerName, "accept", "(Ljava/lang/String;)V"),
				"StringConsumer.accept(String) should not be a library method");

		// Inherited method from java.util.function.Consumer so it should be marked as a library method.
		assertTrue(inheritanceGraph.isLibraryMethod(stringConsumerName, "accept", "(Ljava/lang/Object;)V"),
				"StringConsumer.accept(Object) should be a library method");
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

		// Core Java types
		assertTrue(inheritanceGraph.isAssignableFrom("java/lang/Throwable", "java/lang/Exception"), "Throwable should be assignable from Exception");
		assertFalse(inheritanceGraph.isAssignableFrom("java/lang/Exception", "java/lang/Throwable"), "Exception should not be assignable from Throwable");
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

	@Test
	void vertexUpdatedWithClassModifications() {
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');

		// Get 'Apple' class and vertex.
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();
		JvmClassInfo classInfo = bundle.get(appleName);
		InheritanceVertex vertex = inheritanceGraph.getVertex(appleName);

		// Assert the vertex points to the same reference as the class info.
		assertNotNull(vertex);
		assertSame(classInfo, vertex.getValue(), "Expected vertex to point to class");

		// Modify the class a bit and put it back into the workspace.
		// We're only changing a single byte that doesn't matter so that it won't affect the other tests.
		byte[] bytecodeCopy = Arrays.copyOf(classInfo.getBytecode(), classInfo.getBytecode().length);
		bytecodeCopy[4] = Opcodes.V17;
		JvmClassInfo classInfoUpdated = classInfo.toJvmClassBuilder().adaptFrom(bytecodeCopy).build();
		bundle.put(classInfoUpdated);
		assertNotSame(classInfo, classInfoUpdated);

		// Assert the vertex now points to the updated class info instance.
		InheritanceVertex vertexUpdated = inheritanceGraph.getVertex(appleName);
		assertNotNull(vertexUpdated);
		assertSame(classInfoUpdated, vertexUpdated.getValue(), "Expected updated vertex to point to updated class");
	}

	@Test
	void addingLibraryClearsCachedChildLookups() throws IOException {
		// Create a graph from 'Inheritance' and its nested classes.
		Workspace localWorkspace = TestClassUtils.fromBundle(TestClassUtils.fromClasses(Inheritance.class.getClasses()));
		InheritanceGraph localGraph = new InheritanceGraph(localWorkspace);

		// Get the 'Apple' vertex and assert we can see the initial child 'AppleWithWorm' but not the new child we are about to add.
		String appleName = Inheritance.Apple.class.getName().replace('.', '/');
		InheritanceVertex appleVertex = localGraph.getVertex(appleName);
		assertNotNull(appleVertex, "Could not get Apple vertex from local workspace");
		Set<String> initialChildren = appleVertex.getChildren().stream()
				.map(InheritanceVertex::getName)
				.collect(Collectors.toSet());
		assertEquals(Set.of(appleName + "WithWorm"), initialChildren, "Unexpected initial children for Apple");

		// Add a new class to the workspace that extends Apple.
		String extraChildName = appleName + "ExtraChild";
		WorkspaceResource library = new WorkspaceResourceBuilder()
				.withJvmClassBundle(TestClassUtils.fromClasses(createChildClass(extraChildName, appleName)))
				.build();
		localWorkspace.addSupportingResource(library);

		// Assert that the new child is now visible in the graph.
		Set<String> refreshedChildren = appleVertex.getChildren().stream()
				.map(InheritanceVertex::getName)
				.collect(Collectors.toSet());
		assertEquals(Set.of(appleName + "WithWorm", extraChildName), refreshedChildren,
				"Expected full refresh to invalidate cached child lookups");

		// Assert that the family also includes the new child and that the new child vertex is resolvable.
		Set<String> family = localGraph.getVertexFamily(appleName, false).stream()
				.map(InheritanceVertex::getName)
				.collect(Collectors.toSet());
		assertTrue(family.contains(extraChildName), "Expected refreshed family to include library child");
		assertNotNull(localGraph.getVertex(extraChildName), "Expected library child vertex to be resolvable after refresh");
	}

	/** Had a regression at some point where Android workspaces were not properly populating JDK class hierarchies. */
	@Test
	void androidClassInheritsThroughRuntimeHierarchy() {
		// Mock Android workspace.
		BasicAndroidClassBundle bundle = new BasicAndroidClassBundle();
		String childName = "foo/AndroidListImpl";
		bundle.initialPut(createAndroidChildClass(childName, "java/util/AbstractList"));
		Workspace localWorkspace = TestClassUtils.fromBundle(bundle);
		InheritanceGraph localGraph = new InheritanceGraph(localWorkspace);

		// Assert that a common JDK class has lookups populated for:
		//  - Other JDK classes (List -> AbstractList)
		//  - Android classes (List -> AndroidListImpl)
		Set<String> listChildren = localGraph.getVertex("java/util/List").getAllChildren().stream()
				.map(InheritanceVertex::getName)
				.collect(Collectors.toSet());
		assertTrue(listChildren.contains("java/util/AbstractList"),
				"Expected java/util/List children to include AbstractList from runtime resource");
		assertTrue(listChildren.contains(childName),
				"Expected java/util/List children to include AndroidListImpl from primary resource");
	}

	@Test
	@Timeout(5)
	void profileLargeHierarchyConstruction() {
		// Generate a workspace with a large number of classes in a single inheritance chain.
		int classCount = 200_000;
		String prefix = "bench/Generated";
		BasicJvmClassBundle classes = new BasicJvmClassBundle();
		for (int i = 0; i < classCount; i++) {
			String name = prefix + i;
			String superName = i == 0 ? "java/lang/Object" : prefix + (i - 1);
			classes.initialPut(createChildClass(name, superName));
		}
		Workspace localWorkspace = TestClassUtils.fromBundle(classes);
		long start = System.nanoTime();

		// Constructing the graph should be reasonably fast even with a large number of classes.
		// If this test fails, it is likely that the graph construction is doing something inefficient.
		InheritanceGraph localGraph = new InheritanceGraph(localWorkspace);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
		assertNotNull(localGraph.getVertex(prefix + (classCount - 1)), "Expected last generated class to be present");
		System.out.println("Constructed inheritance graph for " + classCount + " classes in " + elapsedMs + "ms");
	}

	@Nonnull
	private static JvmClassInfo createChildClass(String name, String superName) {
		return TestClassUtils.createClass(name, node -> node.superName = superName);
	}

	@Nonnull
	private static AndroidClassInfo createAndroidChildClass(String name, String superName) {
		ClassDefinition definition = new ClassDefinition(
				me.darknet.dex.tree.type.Types.instanceTypeFromInternalName(name),
				me.darknet.dex.tree.type.Types.instanceTypeFromInternalName(superName),
				ACC_PUBLIC);
		return new AndroidClassInfoBuilder(definition).build();
	}
}
