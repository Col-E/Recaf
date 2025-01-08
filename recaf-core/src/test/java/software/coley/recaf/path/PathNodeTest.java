package software.coley.recaf.path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.Accessed;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.annotation.Annotated;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.info.builder.TextFileInfoBuilder;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.StringConsumerUser;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.AndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicFileBundle;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.test.TestClassUtils.*;

/**
 * Tests for {@link PathNode}.
 */
class PathNodeTest {
	static Workspace workspace;
	static WorkspaceResource primaryResource;
	static WorkspaceResource secondaryResource;
	static JvmClassBundle primaryJvmBundle;
	static AndroidClassBundle primaryAndroidBundle;
	static JvmClassBundle secondaryJvmBundle;
	static FileInfo primaryFileInfo;
	static FileBundle primaryFileBundle;
	static JvmClassInfo primaryClassInfo;
	static JvmClassInfo secondaryClassInfo;
	static AndroidClassInfo primaryAndroidClassInfo;
	// paths
	static ClassPathNode p1;
	static DirectoryPathNode p2;
	static DirectoryPathNode p2b;
	static BundlePathNode p3;
	static BundlePathNode p3and;
	static BundlePathNode p3file;
	static ResourcePathNode p4;
	static WorkspacePathNode p5;
	static ClassPathNode s1;
	static DirectoryPathNode s2;
	static BundlePathNode s3;
	static ResourcePathNode s4;

	@BeforeAll
	static void setup() throws IOException {
		primaryClassInfo = fromRuntimeClass(StringConsumer.class);
		primaryJvmBundle = fromClasses(primaryClassInfo);
		primaryFileBundle = new BasicFileBundle();
		primaryFileInfo = new TextFileInfoBuilder().withName("foo").withRawContent("foo".getBytes()).build();
		primaryFileBundle.put(primaryFileInfo);
		primaryAndroidClassInfo = new AndroidClassInfoBuilder().withName("foo/HelloWorld").build();
		primaryAndroidBundle = new BasicAndroidClassBundle();
		primaryAndroidBundle.put(primaryAndroidClassInfo);
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		androidClassBundles.put("classex.dex", primaryAndroidBundle);
		primaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(primaryJvmBundle)
				.withFileBundle(primaryFileBundle)
				.withAndroidClassBundles(androidClassBundles)
				.build();

		secondaryClassInfo = fromRuntimeClass(StringConsumerUser.class);
		secondaryJvmBundle = fromClasses(secondaryClassInfo);
		secondaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(secondaryJvmBundle)
				.build();

		workspace = new BasicWorkspace(primaryResource, List.of(secondaryResource));

		String packageName = Objects.requireNonNull(primaryClassInfo.getPackageName());
		String parentPackageName = packageName.substring(0, packageName.lastIndexOf('/'));

		p5 = PathNodes.workspacePath(workspace);
		p4 = p5.child(primaryResource);
		p3 = p4.child(primaryJvmBundle);
		p3and = p4.child(primaryAndroidBundle);
		p3file = p4.child(primaryFileBundle);
		p2 = p3.child(packageName);
		p2b = p3.child(parentPackageName);
		p1 = p2.child(primaryClassInfo);

		s4 = p5.child(secondaryResource);
		s3 = s4.child(secondaryJvmBundle);
		s2 = s3.child(packageName);
		s1 = s2.child(secondaryClassInfo);
	}

	@Nested
	class Value {
		@Test
		void getValueOfTypeForParentTypes() {
			// We should be able to get the current value by any of its interfaces.
			ClassInfo v1 = p1.getValueOfType(ClassInfo.class);
			Accessed v2 = p1.getValueOfType(Accessed.class);
			Annotated v3 = p1.getValueOfType(Annotated.class);
			assertSame(v1, v2);
			assertSame(v2, v3);
		}

		@Test
		void getParentOfTypeForParentTypes() {
			ClassMemberPathNode memberPath = p1.child(primaryClassInfo.getMethods().get(0));

			// Member path is not a class-info, so this will be the parent.
			ClassPathNode v1 = memberPath.getPathOfType(ClassInfo.class);
			assertSame(v1, p1);

			// The member itself is accessed/annotated
			ClassMemberPathNode v2 = memberPath.getPathOfType(Accessed.class);
			ClassMemberPathNode v3 = memberPath.getPathOfType(Annotated.class);
			assertSame(memberPath, v2);
			assertSame(v2, v3);
		}
	}

	@Nested
	class Descendant {
		@Test
		void childDescendantOfParent() {
			// Descendant of parent
			assertTrue(p1.isDescendantOf(p2));
			assertTrue(p1.isDescendantOf(p2b));
			assertTrue(p1.isDescendantOf(p3));
			assertTrue(p1.isDescendantOf(p4));
			assertTrue(p1.isDescendantOf(p5));
			assertTrue(p2.isDescendantOf(p2b));
			assertTrue(p2.isDescendantOf(p3));
			assertTrue(p2.isDescendantOf(p4));
			assertTrue(p2.isDescendantOf(p5));
			assertTrue(p2b.isDescendantOf(p3));
			assertTrue(p2b.isDescendantOf(p4));
			assertTrue(p2b.isDescendantOf(p5));
			assertTrue(p3.isDescendantOf(p4));
			assertTrue(p3.isDescendantOf(p5));
			assertTrue(p4.isDescendantOf(p5));
		}

		@Test
		void descendantOfSelf() {
			// Descendant of self
			assertTrue(p1.isDescendantOf(p1));
			assertTrue(p2.isDescendantOf(p2));
			assertTrue(p2b.isDescendantOf(p2b));
			assertTrue(p3.isDescendantOf(p3));
			assertTrue(p4.isDescendantOf(p4));
			assertTrue(p5.isDescendantOf(p5));
		}

		@Test
		void parentNotDescendantOfChild() {
			// Parent is not descendant of child
			assertFalse(p5.isDescendantOf(p4));
			assertFalse(p5.isDescendantOf(p3));
			assertFalse(p5.isDescendantOf(p2));
			assertFalse(p5.isDescendantOf(p1));
			assertFalse(p4.isDescendantOf(p3));
			assertFalse(p4.isDescendantOf(p2));
			assertFalse(p4.isDescendantOf(p1));
			assertFalse(p3.isDescendantOf(p2));
			assertFalse(p3.isDescendantOf(p1));
			assertFalse(p2b.isDescendantOf(p2));
			assertFalse(p2b.isDescendantOf(p1));
			assertFalse(p2.isDescendantOf(p1));
		}

		@Test
		void sameDirectoryPathsFromDifferentParentAreNotDescendants() {
			assertFalse(p3.isDescendantOf(s3));
			assertFalse(p2.isDescendantOf(s2));
			assertFalse(p1.isDescendantOf(s1));
		}

		@Test
		void directoryNodesCanValidateParentChildRelationsFromPathValues() {
			DirectoryPathNode dirA = new DirectoryPathNode("a");
			DirectoryPathNode dirB = new DirectoryPathNode("b");
			DirectoryPathNode dirAA = new DirectoryPathNode("a/a");
			DirectoryPathNode dirAAA = new DirectoryPathNode("a/a/a");
			DirectoryPathNode dirAAB = new DirectoryPathNode("a/a/b");
			DirectoryPathNode dirAB = new DirectoryPathNode("a/b");
			DirectoryPathNode dirABA = new DirectoryPathNode("a/b/a");
			DirectoryPathNode dirABB = new DirectoryPathNode("a/b/b");

			// A
			assertFalse(dirA.isParentOf(dirB));
			assertTrue(dirA.isParentOf(dirAA));
			assertTrue(dirA.isParentOf(dirAAA));
			assertTrue(dirA.isParentOf(dirAAB));
			assertTrue(dirA.isParentOf(dirAB));
			assertTrue(dirA.isParentOf(dirABA));
			assertTrue(dirA.isParentOf(dirABB));
			// B
			assertFalse(dirB.isParentOf(dirA));
			assertFalse(dirB.isParentOf(dirAA));
			assertFalse(dirB.isParentOf(dirAAA));
			assertFalse(dirB.isParentOf(dirAAB));
			assertFalse(dirB.isParentOf(dirAB));
			assertFalse(dirB.isParentOf(dirABA));
			assertFalse(dirB.isParentOf(dirABB));
			// AA
			assertFalse(dirAA.isParentOf(dirB));
			assertFalse(dirAA.isParentOf(dirA));
			assertTrue(dirAA.isParentOf(dirAAA));
			assertTrue(dirAA.isParentOf(dirAAB));
			assertFalse(dirAA.isParentOf(dirAB));
			assertFalse(dirAA.isParentOf(dirABA));
			assertFalse(dirAA.isParentOf(dirABB));
			// AB
			assertFalse(dirAB.isParentOf(dirB));
			assertFalse(dirAB.isParentOf(dirAA));
			assertFalse(dirAB.isParentOf(dirAAA));
			assertFalse(dirAB.isParentOf(dirAAB));
			assertTrue(dirAB.isParentOf(dirABA));
			assertTrue(dirAB.isParentOf(dirABB));

			// Classes
			// A
			ClassPathNode classA = dirA.child(createEmptyClass(dirA.getValue() + "/TheClass"));
			assertTrue(classA.isDescendantOf(dirA));
			assertFalse(classA.isDescendantOf(dirAA));
			assertFalse(classA.isDescendantOf(dirAB));
			assertFalse(classA.isDescendantOf(dirAAA));
			assertFalse(classA.isDescendantOf(dirAAB));
			assertFalse(classA.isDescendantOf(dirABA));
			assertFalse(classA.isDescendantOf(dirABB));
			assertFalse(classA.isDescendantOf(dirB));

			// AA
			ClassPathNode classAA = dirAA.child(createEmptyClass(dirAA.getValue() + "/TheClass"));
			assertTrue(classAA.isDescendantOf(dirA));
			assertTrue(classAA.isDescendantOf(dirAA));
			assertFalse(classAA.isDescendantOf(dirAB));
			assertFalse(classAA.isDescendantOf(dirAAA));
			assertFalse(classAA.isDescendantOf(dirAAB));
			assertFalse(classAA.isDescendantOf(dirABA));
			assertFalse(classAA.isDescendantOf(dirABB));
			assertFalse(classAA.isDescendantOf(dirB));

			// AAA
			ClassPathNode classAAA = dirAAA.child(createEmptyClass(dirAAA.getValue() + "/TheClass"));
			assertTrue(classAAA.isDescendantOf(dirA));
			assertTrue(classAAA.isDescendantOf(dirAA));
			assertFalse(classAAA.isDescendantOf(dirAB));
			assertTrue(classAAA.isDescendantOf(dirAAA));
			assertFalse(classAAA.isDescendantOf(dirAAB));
			assertFalse(classAAA.isDescendantOf(dirABA));
			assertFalse(classAAA.isDescendantOf(dirABB));
			assertFalse(classAAA.isDescendantOf(dirB));

			// B
			ClassPathNode classB = dirB.child(createEmptyClass(dirB.getValue() + "/TheClass"));
			assertFalse(classB.isDescendantOf(dirA));
			assertFalse(classB.isDescendantOf(dirAA));
			assertFalse(classB.isDescendantOf(dirAB));
			assertFalse(classB.isDescendantOf(dirAAA));
			assertFalse(classB.isDescendantOf(dirAAB));
			assertFalse(classB.isDescendantOf(dirABA));
			assertFalse(classB.isDescendantOf(dirABB));
			assertTrue(classB.isDescendantOf(dirB));

			// Class to class
			// A
			assertTrue(classA.isDescendantOf(classA));
			assertFalse(classA.isDescendantOf(classAA));
			assertFalse(classA.isDescendantOf(classAAA));
			assertFalse(classA.isDescendantOf(classB));

			// AA
			assertFalse(classAA.isDescendantOf(classA));
			assertTrue(classAA.isDescendantOf(classAA));
			assertFalse(classAA.isDescendantOf(classAAA));
			assertFalse(classAA.isDescendantOf(classB));

			// AAA
			assertFalse(classAAA.isDescendantOf(classA));
			assertFalse(classAAA.isDescendantOf(classAA));
			assertTrue(classAAA.isDescendantOf(classAAA));
			assertFalse(classAAA.isDescendantOf(classB));

			// B
			assertFalse(classB.isDescendantOf(classA));
			assertFalse(classB.isDescendantOf(classAA));
			assertFalse(classB.isDescendantOf(classAAA));
			assertTrue(classB.isDescendantOf(classB));
		}
	}

	@Nested
	class Comparison {
		@Test // ignore warning about 'compareTo' on self
		@SuppressWarnings("all")
		void compareToSelfIsZero() {
			// Self comparison or equal items should always be 0
			assertEquals(0, p1.compareTo(p1));
			assertEquals(0, p2.compareTo(p2));
			assertEquals(0, p3.compareTo(p3));
			assertEquals(0, p4.compareTo(p4));
			assertEquals(0, p5.compareTo(p5));
		}

		@Test
		void compareToParentIsGreater() {
			// Children appear last (thus > 0)
			assertTrue(p1.compareTo(p2) > 0);
			assertTrue(p2.compareTo(p3) > 0);
			assertTrue(p3.compareTo(p4) > 0);
			assertTrue(p4.compareTo(p5) > 0);
		}

		@Test
		void compareToChildIsLess() {
			// Parents appear first (thus < 0)
			assertTrue(p5.compareTo(p4) < 0);
			assertTrue(p5.compareTo(p3) < 0);
			assertTrue(p5.compareTo(p2) < 0);
			assertTrue(p5.compareTo(p1) < 0);
			assertTrue(p4.compareTo(p3) < 0);
			assertTrue(p4.compareTo(p2) < 0);
			assertTrue(p4.compareTo(p1) < 0);
			assertTrue(p3.compareTo(p2) < 0);
			assertTrue(p3.compareTo(p1) < 0);
			assertTrue(p2.compareTo(p1) < 0);
		}

		@Test
		void compareToOtherBundleTypes() {
			// 1: JVM
			// 2: Versioned
			// 3: Android
			// 4: File
			assertEquals(-1, p3.compareTo(p3and));
			assertEquals(-1, p3.compareTo(p3file));
			assertEquals(-1, p3and.compareTo(p3file));

			// Inverse
			assertEquals(1, p3and.compareTo(p3));
			assertEquals(1, p3file.compareTo(p3));
			assertEquals(1, p3file.compareTo(p3and));
		}
	}

	@Nested
	class Misc {
		@Test
		void bundleInTarget() {
			// JVM bundle
			assertTrue(p3.isInJvmBundle());
			assertFalse(p3.isInVersionedJvmBundle());
			assertFalse(p3.isInAndroidBundle());
			assertFalse(p3.isInFileBundle());

			// Android bundle
			assertFalse(p3and.isInJvmBundle());
			assertFalse(p3and.isInVersionedJvmBundle());
			assertTrue(p3and.isInAndroidBundle());
			assertFalse(p3and.isInFileBundle());

			// File bundle
			assertFalse(p3file.isInJvmBundle());
			assertFalse(p3file.isInVersionedJvmBundle());
			assertFalse(p3file.isInAndroidBundle());
			assertTrue(p3file.isInFileBundle());
		}
	}
}