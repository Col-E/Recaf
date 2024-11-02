package software.coley.recaf.ui.control.tree;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.collections.Unchecked;
import software.coley.recaf.info.BasicFileInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubFileInfo;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.path.BundlePathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.DirectoryPathNode;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.path.ResourcePathNode;
import software.coley.recaf.path.WorkspacePathNode;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.VariedModifierFields;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.test.TestClassUtils.*;
import static software.coley.recaf.ui.control.tree.WorkspaceTreeNode.getOrInsertIntoTree;

/**
 * Tests for {@link WorkspaceTreeNode}.
 */
class WorkspaceTreeNodeTest {
	static Workspace workspace;
	static WorkspaceResource primaryResource;
	static WorkspaceResource resourceWithEmbedded;
	static WorkspaceFileResource embeddedResource;
	static String embeddedResourcePath = "embedded.jar";
	static JvmClassBundle primaryJvmBundle;
	static JvmClassInfo classA;
	static JvmClassInfo classB;
	static JvmClassInfo classC;
	static JvmClassInfo classD;
	// Normal paths
	static ClassPathNode p1a;
	static ClassPathNode p1b;
	static ClassPathNode p1c;
	static ClassPathNode p1d;
	static DirectoryPathNode p2;
	static DirectoryPathNode p2b;
	static BundlePathNode p3c;
	static BundlePathNode p3f;
	static ResourcePathNode p4;
	static WorkspacePathNode p5;
	// Default package paths
	static FilePathNode default1;
	static DirectoryPathNode default2;
	// Obfuscated zero width paths
	static FilePathNode z1;
	static DirectoryPathNode z2;

	@BeforeAll
	static void setup() throws IOException {
		BasicFileBundle fileBundle = new BasicFileBundle();

		primaryJvmBundle = fromClasses(
				classA = fromRuntimeClass(AccessibleFields.class),
				classB = fromRuntimeClass(HelloWorld.class),
				classC = fromRuntimeClass(StringConsumer.class),
				classD = fromRuntimeClass(VariedModifierFields.class));
		primaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(primaryJvmBundle)
				.withFileBundle(fileBundle)
				.build();

		workspace = new BasicWorkspace(primaryResource);

		String packageName = Objects.requireNonNull(classA.getPackageName());
		String parentPackageName = packageName.substring(0, packageName.lastIndexOf('/'));

		p5 = PathNodes.workspacePath(workspace);
		p4 = p5.child(primaryResource);
		p3c = p4.child(primaryJvmBundle);
		p3f = p4.child(fileBundle);
		p2 = p3c.child(packageName);
		p2b = p3c.child(parentPackageName);
		p1a = p2.child(classA);
		p1b = p2.child(classB);
		p1c = p2.child(classC);
		p1d = p2.child(classD);

		// Content available in the default package/root directory.
		default2 = p3f.child(null);
		default1 = default2.child(new BasicFileInfo("root.txt", new byte[0], new BasicPropertyContainer()));

		// The path will visually look like (root)//zero.txt in the workspace tree.
		// This is not ideal, but there's not really any great alternatives either.
		z2 = p3f.child("//");
		z1 = z2.child(new BasicFileInfo("///zero.txt", new byte[0], new BasicPropertyContainer()));

		// Embedded resource containing just 'root.txt'
		embeddedResource = new WorkspaceFileResourceBuilder(new BasicJvmClassBundle(), fromFiles(default1.getValue()))
				.withFileInfo(new StubFileInfo("embedded.jar")).build();
		resourceWithEmbedded = new WorkspaceResourceBuilder(new BasicJvmClassBundle(), new BasicFileBundle())
				.withEmbeddedResources(Map.of(embeddedResourcePath, embeddedResource))
				.build();
	}

	@Test
	void testPathCreationOfFileInEmbeddedResource() {
		Workspace workspace = new BasicWorkspace(resourceWithEmbedded);
		WorkspacePathNode workspacePath = PathNodes.workspacePath(workspace);
		FilePathNode embeddedFilePath = workspacePath.child(resourceWithEmbedded)
				.embeddedChildContainer()
				.child(embeddedResource)
				.child(embeddedResource.getFileBundle())
				.child(null)
				.child(default1.getValue());

		WorkspaceTreeNode root = new WorkspaceTreeNode(workspacePath);
		root.getOrCreateNodeByPath(embeddedFilePath);

		// workspace
		WorkspaceTreeNode child = root.getFirstChild();
		assertNotNull(child, "Workspace did not have child");

		// workspace > resourceWithEmbedded
		child = child.getFirstChild();
		assertNotNull(child, "Primary resource did not have child");

		// workspace > resourceWithEmbedded > embedded-container
		child = child.getFirstChild();
		assertNotNull(child, "Embedded container did not have child");

		// workspace > resourceWithEmbedded > embedded-container > embeddedResource
		child = child.getFirstChild();
		assertNotNull(child, "Embedded resource did not have child");

		// workspace > resourceWithEmbedded > embedded-container > embeddedResource > bundle
		child = child.getFirstChild();
		assertNotNull(child, "Embedded bundle did not have child");

		// workspace > resourceWithEmbedded > embedded-container > embeddedResource > bundle > directory
		child = child.getFirstChild();
		assertNotNull(child, "Embedded directory did not have child");

		// workspace > resourceWithEmbedded > embedded-container > embeddedResource > bundle > directory > file
		Object createdPathFile = child.getValue().getValue();
		FileInfo file = default1.getValue();
		assertEquals(file, createdPathFile, "File at end of path not the same");
	}

	@Test
	void testPackageDoesNotPreventRemovalOfPackageWithSamePrefix() {
		DirectoryPathNode firstDir = new DirectoryPathNode("co/fizz");
		DirectoryPathNode secondDir = new DirectoryPathNode("com/foo");
		ClassPathNode firstClass = firstDir.child(createEmptyClass(firstDir.getValue() + "/Buzz"));
		ClassPathNode secondClass = secondDir.child(createEmptyClass(secondDir.getValue() + "/Bar"));

		// Create the tree:
		//  root:
		//   co/
		//    fizz/
		//     Buzz
		//   com/
		//    foo/
		//     Bar
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(firstClass);
		root.getOrCreateNodeByPath(secondClass);

		// Removal of 'com/foo/Bar' should not be blocked by existence of 'co/fizz/Buzz'
		// This ensures our package parent checks do not regress.
		assertTrue(root.removeNodeByPath(secondClass));
	}

	@Test
	void testGetAndRemoveWithDifferentNodesOfEqualValue() {
		ClassPathNode[] array = new ClassPathNode[]{p1a, p1b, p1c, p1d};

		// Create copies of the path nodes which we will use for the rest of the test.
		var p5 = PathNodes.workspacePath(workspace);
		var p4 = p5.child(primaryResource);
		var p3c = p4.child(primaryJvmBundle);
		var p2 = p3c.child(classA.getPackageName());
		var p1a = p2.child(classA);
		var p1b = p2.child(classB);
		var p1c = p2.child(classC);
		var p1d = p2.child(classD);

		// Test for each class path in the array.
		for (ClassPathNode classPath : array) {
			// Create a tree model which has each class (using the cloned path nodes). Looks like:
			//  workspace
			//   bundle
			//    dir
			//     classA
			//     classB
			//     classC
			//     classD
			WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
			root.getOrCreateNodeByPath(p1a);
			root.getOrCreateNodeByPath(p1b);
			root.getOrCreateNodeByPath(p1c);
			root.getOrCreateNodeByPath(p1d);

			// Try doing operations with the ORIGINAL path node reference.
			assertNotNull(root.getNodeByPath(classPath), "Could not get info");
			assertTrue(root.removeNodeByPath(classPath));
			assertNull(root.getNodeByPath(classPath), "Info not removed");
		}
	}

	@Test
	void defaultPackage() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(default1);

		// Remove the info
		assertNotNull(root.getNodeByPath(default1), "Could not get info");
		assertTrue(root.removeNodeByPath(default1));
		assertNull(root.getNodeByPath(default1), "Info not removed");

		// The directory should be removed since the tree was linear
		assertNull(root.getNodeByPath(default2), "Could not get directory of info");
		assertFalse(root.removeNodeByPath(default2));
		assertNull(root.getNodeByPath(default2), "Directory of info not removed");

		// There is no more content in the bundle, so it should be gone too
		assertNull(root.getNodeByPath(p3f), "Could not get file bundle");
	}

	@Test
	void zeroWidthDir() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(z1);

		// Remove the info
		assertNotNull(root.getNodeByPath(z1), "Could not get info");
		assertTrue(root.removeNodeByPath(z1));
		assertNull(root.getNodeByPath(z1), "Info not removed");

		// The directory should be removed since the tree was linear
		assertNull(root.getNodeByPath(z2), "Could not get directory of info");
		assertFalse(root.removeNodeByPath(z2));
		assertNull(root.getNodeByPath(z2), "Directory of info not removed");

		// There is no more content in the bundle, so it should be gone too
		assertNull(root.getNodeByPath(p3f), "Could not get file bundle");
	}

	@Test
	void removeOneOfTwoChildrenDoesNotPruneWholeTree() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1a);
		root.getOrCreateNodeByPath(p1b);

		// Remove the info
		assertNotNull(root.getNodeByPath(p1a), "Could not get info");
		assertTrue(root.removeNodeByPath(p1a));
		assertNull(root.getNodeByPath(p1a), "Info not removed");

		// The package should be not be removed since the tree still has one class remaining
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertTrue(root.removeNodeByPath(p1b), "Info not removed");

		// Now the package should be removed
		assertNull(root.getNodeByPath(p2), "Package of info not removed");
		assertNull(root.getNodeByPath(p2b), "Parent of that package should not have been removed");

		// There is no more content in the bundle, so it should be gone too
		assertNull(root.getNodeByPath(p3c), "Could not get jvm class bundle");
		assertFalse(root.removeNodeByPath(p3c));
		assertNull(root.getNodeByPath(p3c), "Jvm class bundle not removed");
		assertNull(root.getNodeByPath(p2b), "Child of jvm class bundle still accessible after bundle removal");
	}

	@Test
	void removeNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1a);

		// Remove the info
		assertNotNull(root.getNodeByPath(p1a), "Could not get info");
		assertTrue(root.removeNodeByPath(p1a));
		assertNull(root.getNodeByPath(p1a), "Info not removed");

		// The package should be removed since the tree was linear
		assertNull(root.getNodeByPath(p2), "Could not get package of info");
		assertFalse(root.removeNodeByPath(p2));
		assertNull(root.getNodeByPath(p2), "Package of info not removed");
		assertNull(root.getNodeByPath(p2b), "Parent of that package should not have been removed");

		// There is no more content in the bundle, so it should be gone too
		assertNull(root.getNodeByPath(p3c), "Could not get jvm class bundle");
		assertFalse(root.removeNodeByPath(p3c));
		assertNull(root.getNodeByPath(p3c), "Jvm class bundle not removed");
		assertNull(root.getNodeByPath(p2b), "Child of jvm class bundle still accessible after bundle removal");
	}

	@Test
	void getNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1a);

		// Get each node, should exist.
		assertNotNull(root.getNodeByPath(p1a), "Could not get info");
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertNotNull(root.getNodeByPath(p2b), "Could not get parent package of info");
		assertNotNull(root.getNodeByPath(p3c), "Could not get bundle");
		assertNotNull(root.getNodeByPath(p4), "Could not get resource");
	}

	@Test
	void getOrCreateNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);

		// Get or create the deepest path should create all other parent paths.
		WorkspaceTreeNode result = root.getOrCreateNodeByPath(p1a);
		assertNotNull(result, "No result of get or create operation");
		assertEquals(result, root.getNodeByPath(p1a), "Could not get info");
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertNotNull(root.getNodeByPath(p2b), "Could not get parent package of info");
		assertNotNull(root.getNodeByPath(p3c), "Could not get bundle");
		assertNotNull(root.getNodeByPath(p4), "Could not get resource");

		// Try inserting with just the info missing.
		assertTrue(root.removeNodeByPath(p1a));
		result = root.getOrCreateNodeByPath(p1a);
		assertEquals(result, root.getNodeByPath(p1a), "Could not get info");

		// Try inserting with parent package missing.
		assertTrue(root.removeNodeByPath(p2b));
		result = root.getOrCreateNodeByPath(p1a);
		assertEquals(result, root.getNodeByPath(p1a), "Could not get info");

		// If we do repeated calls, the reference should always be the same since it acts as a getter.
		assertSame(result, root.getOrCreateNodeByPath(p1a));
		assertSame(root.getOrCreateNodeByPath(p3c), root.getOrCreateNodeByPath(p3c));
	}

	@Test
	void matches() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1a);

		// Get child-most item following the "top" of the tree.
		// Should yield the class info node.
		WorkspaceTreeNode child = root;
		while (!child.getChildren().isEmpty())
			child = (WorkspaceTreeNode) child.getChildren().get(0);

		// Validate it is the node for the class info.
		assertTrue(child.matches(p1a));
	}

	@Nested
	class Filtered {
		@Test
		void insertWhileFilteredStillUpdatesChildren() {
			// Create workspace root, but apply a filter so that nothing is shown
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);
			workspaceNode.predicateProperty().set(p -> false);

			// Insert the class, which should generate all paths between the class and the workspace node.
			WorkspaceTreeNode classNode = getOrInsertIntoTree(workspaceNode, p1a);
			assertNotNull(classNode, "Class not yielded by original assert");

			// Validate the filtered view is still empty, but the unfiltered model has children
			WorkspaceTreeNode node = workspaceNode;
			for (int d = 0; d < 5; d++) {
				assertTrue(node.getChildren().isEmpty(), "Filtered children list is not empty: depth=" + d);
				assertFalse(node.getSourceChildren().isEmpty(), "Unfiltered children list was empty: depth=" + d);
				node = Unchecked.cast(workspaceNode.getSourceChildren().getFirst());
			}
		}

		@Test
		void removeWhileFilteredStillUpdatesChildren() {
			// Create workspace root and insert the class, which should generate all paths between the class and the workspace node.
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);
			WorkspaceTreeNode classNode = getOrInsertIntoTree(workspaceNode, p1a);
			assertNotNull(classNode, "Class not yielded by original assert");
			assertNotNull(workspaceNode.getNodeByPath(p1a), "Could not get class after setup");

			// Apply a filter so that nothing is shown.
			// We should still be able to access items via path-lookup though.
			workspaceNode.predicateProperty().set(p -> false);
			assertNotNull(workspaceNode.getNodeByPath(p1a), "Could not get class after filtering");

			// Validate the filtered view is still empty, but the unfiltered model has children
			assertTrue(workspaceNode.removeNodeByPath(p1a), "Class could not be removed");
			assertNull(workspaceNode.getNodeByPath(p1a), "Class accessible after removed");
		}

		@Test
		void removeWhileFilteredDoesNotEliminateOtherClasses() {
			// Create workspace root and insert the class, which should generate all paths between the class and the workspace node.
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);
			WorkspaceTreeNode classNodeA = getOrInsertIntoTree(workspaceNode, p1a);
			WorkspaceTreeNode classNodeB = getOrInsertIntoTree(workspaceNode, p1b);
			WorkspaceTreeNode classNodeC = getOrInsertIntoTree(workspaceNode, p1c);
			assertNotNull(classNodeA, "Class not yielded by original assert");
			assertNotNull(classNodeB, "Class not yielded by original assert");
			assertNotNull(classNodeC, "Class not yielded by original assert");
			assertNotNull(workspaceNode.getNodeByPath(p1a), "Could not get class after setup");
			assertNotNull(workspaceNode.getNodeByPath(p1b), "Could not get class after setup");
			assertNotNull(workspaceNode.getNodeByPath(p1c), "Could not get class after setup");

			// Apply a filter so that nothing is shown.
			workspaceNode.predicateProperty().set(p -> false);

			// Remove 'Class A' and verify 'Class B' and 'Class C' are still accessible
			assertTrue(workspaceNode.removeNodeByPath(p1a), "Could not remove class after filtering");
			assertNull(workspaceNode.getNodeByPath(p1a), "Class A accessible after removed");
			assertNotNull(workspaceNode.getNodeByPath(p1b), "Class B not accessible after adjacent leaf removed");
			assertNotNull(workspaceNode.getNodeByPath(p1c), "Class C not accessible after adjacent leaf removed");
		}
	}

	@Nested
	class Insertion {
		@Test
		void insertClassGeneratesIntermediatesToWorkspaceNode() {
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);

			// Insert the class, which should generate all paths between the class and the workspace node.
			WorkspaceTreeNode classNode = getOrInsertIntoTree(workspaceNode, p1a);
			assertNotNull(classNode, "Class not yielded by original assert");

			// Assert all package entries exist for: software.coley.recaf.test.dummy
			WorkspaceTreeNode packageDummy = classNode.getSourceParentNode();
			assertNotNull(packageDummy, "Missing node for package: 'software.coley.recaf.test.dummy'");
			WorkspaceTreeNode packageTest = packageDummy.getSourceParentNode();
			assertNotNull(packageTest, "Missing node for package: 'software.coley.recaf.test'");
			WorkspaceTreeNode packageRecaf = packageTest.getSourceParentNode();
			assertNotNull(packageRecaf, "Missing node for package: 'software.coley.recaf'");
			WorkspaceTreeNode packageColey = packageRecaf.getSourceParentNode();
			assertNotNull(packageColey, "Missing node for package: 'software.coley'");
			WorkspaceTreeNode packageSoftware = packageColey.getSourceParentNode();
			assertNotNull(packageSoftware, "Missing node for package: 'software'");

			// Bundle parent
			WorkspaceTreeNode bundleNode = packageSoftware.getSourceParentNode();
			assertNotNull(bundleNode, "Missing bundle node");
			assertTrue(bundleNode.getValue() instanceof BundlePathNode);

			// Resource parent
			WorkspaceTreeNode resourceNode = bundleNode.getSourceParentNode();
			assertNotNull(resourceNode, "Missing resource node");
			assertTrue(resourceNode.getValue() instanceof ResourcePathNode);

			// Workspace parent should be the same as the original item we had.
			WorkspaceTreeNode workspaceNode2 = resourceNode.getSourceParentNode();
			assertSame(workspaceNode, workspaceNode2);
		}

		@Test
		void duplicateInsertYieldsExistingNode() {
			// Create workspace root
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);

			// Insert operation
			WorkspaceTreeNode classNode1 = getOrInsertIntoTree(workspaceNode, p1a);
			WorkspaceTreeNode classNode2 = getOrInsertIntoTree(workspaceNode, p1a);
			assertSame(classNode1, classNode2);
		}

		@Test
		void duplicateInsertYieldsExistingNodeOnRoot() {
			// Create workspace root
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);

			// Insert operation, insert root value should yield self.
			WorkspaceTreeNode workspaceNode2 = getOrInsertIntoTree(workspaceNode, p5);
			assertSame(workspaceNode2, workspaceNode);
		}
	}
}