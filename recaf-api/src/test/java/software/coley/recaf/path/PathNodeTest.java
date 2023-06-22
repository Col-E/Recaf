package software.coley.recaf.path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.AndroidClassInfo;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.AndroidClassInfoBuilder;
import software.coley.recaf.info.builder.TextFileInfoBuilder;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.StringConsumer;
import software.coley.recaf.test.dummy.StringConsumerUser;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.*;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

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
	static ResourcePathNode p4;
	static WorkspacePathNode p5;

	@BeforeAll
	static void setup() throws IOException {
		primaryClassInfo = TestClassUtils.fromRuntimeClass(StringConsumer.class);
		primaryJvmBundle = TestClassUtils.fromClasses(primaryClassInfo);
		primaryFileBundle = new BasicFileBundle();
		primaryFileInfo = new TextFileInfoBuilder().withName("foo").withRawContent("foo".getBytes()).build();
		primaryFileBundle.put(primaryFileInfo);
		primaryAndroidClassInfo = new AndroidClassInfoBuilder().withName("foo").build();
		primaryAndroidBundle = new BasicAndroidClassBundle();
		primaryAndroidBundle.put(primaryAndroidClassInfo);
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		androidClassBundles.put("classex.dex", primaryAndroidBundle);
		primaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(primaryJvmBundle)
				.withFileBundle(primaryFileBundle)
				.withAndroidClassBundles(androidClassBundles)
				.build();

		secondaryClassInfo = TestClassUtils.fromRuntimeClass(StringConsumerUser.class);
		secondaryJvmBundle = TestClassUtils.fromClasses(secondaryClassInfo);
		secondaryResource = new WorkspaceResourceBuilder()
				.withJvmClassBundle(secondaryJvmBundle)
				.build();

		workspace = new BasicWorkspace(primaryResource, List.of(secondaryResource));

		String packageName = Objects.requireNonNull(primaryClassInfo.getPackageName());
		String parentPackageName = packageName.substring(0, packageName.lastIndexOf('/'));

		p5 = new WorkspacePathNode(workspace);
		p4 = p5.child(primaryResource);
		p3 = p4.child(primaryJvmBundle);
		p2 = p3.child(packageName);
		p2b = p3.child(parentPackageName);
		p1 = p2.child(primaryClassInfo);
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
	}
}