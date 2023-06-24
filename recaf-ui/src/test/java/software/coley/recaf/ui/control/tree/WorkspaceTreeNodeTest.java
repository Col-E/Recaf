package software.coley.recaf.ui.control.tree;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BasicFileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.properties.BasicPropertyContainer;
import software.coley.recaf.path.*;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.ui.control.tree.WorkspaceTreeNode.getOrInsertIntoTree;

/**
 * Tests for {@link WorkspaceTreeNode}.
 */
class WorkspaceTreeNodeTest {
	static Workspace workspace;
	static WorkspaceResource primaryResource;
	static JvmClassBundle primaryJvmBundle;
	static JvmClassInfo primaryClassInfo;
	// Normal paths
	static ClassPathNode p1;
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

		primaryClassInfo = TestClassUtils.fromRuntimeClass(StringConsumer.class);
		primaryJvmBundle = TestClassUtils.fromClasses(primaryClassInfo);
		primaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(primaryJvmBundle)
				.withFileBundle(fileBundle)
				.build();

		workspace = new BasicWorkspace(primaryResource);

		String packageName = Objects.requireNonNull(primaryClassInfo.getPackageName());
		String parentPackageName = packageName.substring(0, packageName.lastIndexOf('/'));

		p5 = PathNodes.workspacePath(workspace);
		p4 = p5.child(primaryResource);
		p3c = p4.child(primaryJvmBundle);
		p3f = p4.child(fileBundle);
		p2 = p3c.child(packageName);
		p2b = p3c.child(parentPackageName);
		p1 = p2.child(primaryClassInfo);

		// Content available in the default package/root directory.
		default2 = p3f.child(null);
		default1 = default2.child(new BasicFileInfo("root.txt", new byte[0], new BasicPropertyContainer()));

		// The path will visually look like (root)//zero.txt in the workspace tree.
		// This is not ideal, but there's not really any great alternatives either.
		z2  = p3f.child("//");
		z1 = z2.child(new BasicFileInfo("///zero.txt", new byte[0], new BasicPropertyContainer()));
	}

	@Test
	void defaultPackage() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(default1);

		// Remove the info
		assertNotNull(root.getNodeByPath(default1), "Could not get info");
		assertTrue(root.removeNodeByPath(default1));
		assertNull(root.getNodeByPath(default1), "Info not removed");

		// Remove the directory
		assertNotNull(root.getNodeByPath(default2), "Could not get directory of info");
		assertTrue(root.removeNodeByPath(default2));
		assertNull(root.getNodeByPath(default2), "Directory of info not removed");

		// Bundle should still exist
		assertNotNull(root.getNodeByPath(p3f), "Could not get file bundle");
	}

	@Test
	void zeroWidthDir() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(z1);

		// Remove the info
		assertNotNull(root.getNodeByPath(z1), "Could not get info");
		assertTrue(root.removeNodeByPath(z1));
		assertNull(root.getNodeByPath(z1), "Info not removed");

		// Remove the directory
		assertNotNull(root.getNodeByPath(z2), "Could not get directory of info");
		assertTrue(root.removeNodeByPath(z2));
		assertNull(root.getNodeByPath(z2), "Directory of info not removed");

		// Bundle should still exist
		assertNotNull(root.getNodeByPath(p3f), "Could not get file bundle");
	}

	@Test
	void removeNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1);

		// Remove the info
		assertNotNull(root.getNodeByPath(p1), "Could not get info");
		assertTrue(root.removeNodeByPath(p1));
		assertNull(root.getNodeByPath(p1), "Info not removed");

		// Remove the package
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertTrue(root.removeNodeByPath(p2));
		assertNull(root.getNodeByPath(p2), "Package of info not removed");
		assertNotNull(root.getNodeByPath(p2b), "Parent of that package should not have been removed");

		// Remove the bundle
		assertNotNull(root.getNodeByPath(p3c), "Could not get jvm class bundle");
		assertTrue(root.removeNodeByPath(p3c));
		assertNull(root.getNodeByPath(p3c), "Jvm class bundle not removed");
		assertNull(root.getNodeByPath(p2b), "Child of jvm class bundle still accessible after bundle removal");
	}

	@Test
	void getNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1);

		// Get each node, should exist.
		assertNotNull(root.getNodeByPath(p1), "Could not get info");
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertNotNull(root.getNodeByPath(p2b), "Could not get parent package of info");
		assertNotNull(root.getNodeByPath(p3c), "Could not get bundle");
		assertNotNull(root.getNodeByPath(p4), "Could not get resource");
	}

	@Test
	void getOrCreateNodeByPath() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);

		// Get or create the deepest path should create all other parent paths.
		WorkspaceTreeNode result = root.getOrCreateNodeByPath(p1);
		assertNotNull(result, "No result of get or create operation");
		assertEquals(result, root.getNodeByPath(p1), "Could not get info");
		assertNotNull(root.getNodeByPath(p2), "Could not get package of info");
		assertNotNull(root.getNodeByPath(p2b), "Could not get parent package of info");
		assertNotNull(root.getNodeByPath(p3c), "Could not get bundle");
		assertNotNull(root.getNodeByPath(p4), "Could not get resource");

		// Try inserting with just the info missing.
		assertTrue(root.removeNodeByPath(p1));
		result = root.getOrCreateNodeByPath(p1);
		assertEquals(result, root.getNodeByPath(p1), "Could not get info");

		// Try inserting with parent package missing.
		assertTrue(root.removeNodeByPath(p2b));
		result = root.getOrCreateNodeByPath(p1);
		assertEquals(result, root.getNodeByPath(p1), "Could not get info");

		// If we do repeated calls, the reference should always be the same since it acts as a getter.
		assertSame(result, root.getOrCreateNodeByPath(p1));
		assertSame(root.getOrCreateNodeByPath(p3c), root.getOrCreateNodeByPath(p3c));
	}

	@Test
	void matches() {
		WorkspaceTreeNode root = new WorkspaceTreeNode(p5);
		root.getOrCreateNodeByPath(p1);

		// Get child-most item following the "top" of the tree.
		// Should yield the class info node.
		WorkspaceTreeNode child = root;
		while (!child.getChildren().isEmpty())
			child = (WorkspaceTreeNode) child.getChildren().get(0);

		// Validate it is the node for the class info.
		assertTrue(child.matches(p1));
	}


	@Nested
	class Insertion {
		@Test
		void insertClassGeneratesIntermediatesToWorkspaceNode() {
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);

			// Insert the class, which should generate all paths between the class and the workspace node.
			WorkspaceTreeNode classNode = getOrInsertIntoTree(workspaceNode, p1);
			assertNotNull(classNode, "Class not yielded by original assert");

			// Assert all package entries exist for: software.coley.recaf.test.dummy
			WorkspaceTreeNode packageDummy = classNode.getParentNode();
			assertNotNull(packageDummy, "Missing node for package: 'software.coley.recaf.test.dummy'");
			WorkspaceTreeNode packageTest = packageDummy.getParentNode();
			assertNotNull(packageTest, "Missing node for package: 'software.coley.recaf.test'");
			WorkspaceTreeNode packageRecaf = packageTest.getParentNode();
			assertNotNull(packageRecaf, "Missing node for package: 'software.coley.recaf'");
			WorkspaceTreeNode packageColey = packageRecaf.getParentNode();
			assertNotNull(packageColey, "Missing node for package: 'software.coley'");
			WorkspaceTreeNode packageSoftware = packageColey.getParentNode();
			assertNotNull(packageSoftware, "Missing node for package: 'software'");

			// Bundle parent
			WorkspaceTreeNode bundleNode = packageSoftware.getParentNode();
			assertNotNull(bundleNode, "Missing bundle node");
			assertTrue(bundleNode.getValue() instanceof BundlePathNode);

			// Resource parent
			WorkspaceTreeNode resourceNode = bundleNode.getParentNode();
			assertNotNull(resourceNode, "Missing resource node");
			assertTrue(resourceNode.getValue() instanceof ResourcePathNode);

			// Workspace parent should be the same as the original item we had.
			WorkspaceTreeNode workspaceNode2 = resourceNode.getParentNode();
			assertSame(workspaceNode, workspaceNode2);
		}

		@Test
		void duplicateInsertYieldsExistingNode() {
			// Create workspace root
			WorkspaceTreeNode workspaceNode = new WorkspaceTreeNode(p5);

			// Insert operation
			WorkspaceTreeNode classNode1 = getOrInsertIntoTree(workspaceNode, p1);
			WorkspaceTreeNode classNode2 = getOrInsertIntoTree(workspaceNode, p1);
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