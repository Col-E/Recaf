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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatComparable;
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
	static BundlePathNode p3and1;
	static BundlePathNode p3and2;
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
		BasicAndroidClassBundle secondaryAndroidBundle = new BasicAndroidClassBundle();
		primaryAndroidBundle.put(primaryAndroidClassInfo);
		Map<String, AndroidClassBundle> androidClassBundles = new HashMap<>();
		androidClassBundles.put("classes.dex", primaryAndroidBundle);
		androidClassBundles.put("others.dex", secondaryAndroidBundle);
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
		p3and1 = p4.child(primaryAndroidBundle);
		p3and2 = p4.child(secondaryAndroidBundle);
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
			assertThat(v2).isSameAs(v1);
			assertThat(v3).isSameAs(v2);
		}

		@Test
		void getParentOfTypeForParentTypes() {
			ClassMemberPathNode memberPath = p1.child(primaryClassInfo.getMethods().getFirst());

			// Member path is not a class-info, so this will be the parent.
			ClassPathNode v1 = memberPath.getPathOfType(ClassInfo.class);
			assertThat(p1).isSameAs(v1);

			// The member itself is accessed/annotated
			ClassMemberPathNode v2 = memberPath.getPathOfType(Accessed.class);
			ClassMemberPathNode v3 = memberPath.getPathOfType(Annotated.class);
			assertThat(v2).isSameAs(memberPath);
			assertThat(v3).isSameAs(v2);
		}
	}

	@Nested
	class Descendant {
		@Test
		void childDescendantOfParent() {
			// Descendant of parent
			assertThat(p1.isDescendantOf(p2)).isTrue();
			assertThat(p1.isDescendantOf(p2b)).isTrue();
			assertThat(p1.isDescendantOf(p3)).isTrue();
			assertThat(p1.isDescendantOf(p4)).isTrue();
			assertThat(p1.isDescendantOf(p5)).isTrue();
			assertThat(p2.isDescendantOf(p2b)).isTrue();
			assertThat(p2.isDescendantOf(p3)).isTrue();
			assertThat(p2.isDescendantOf(p4)).isTrue();
			assertThat(p2.isDescendantOf(p5)).isTrue();
			assertThat(p2b.isDescendantOf(p3)).isTrue();
			assertThat(p2b.isDescendantOf(p4)).isTrue();
			assertThat(p2b.isDescendantOf(p5)).isTrue();
			assertThat(p3.isDescendantOf(p4)).isTrue();
			assertThat(p3.isDescendantOf(p5)).isTrue();
			assertThat(p4.isDescendantOf(p5)).isTrue();
		}

		@Test
		void descendantOfSelf() {
			// Descendant of self
			assertThat(p1.isDescendantOf(p1)).isTrue();
			assertThat(p2.isDescendantOf(p2)).isTrue();
			assertThat(p2b.isDescendantOf(p2b)).isTrue();
			assertThat(p3.isDescendantOf(p3)).isTrue();
			assertThat(p4.isDescendantOf(p4)).isTrue();
			assertThat(p5.isDescendantOf(p5)).isTrue();
		}

		@Test
		void parentNotDescendantOfChild() {
			// Parent is not descendant of child
			assertThat(p5.isDescendantOf(p4)).isFalse();
			assertThat(p5.isDescendantOf(p3)).isFalse();
			assertThat(p5.isDescendantOf(p2)).isFalse();
			assertThat(p5.isDescendantOf(p1)).isFalse();
			assertThat(p4.isDescendantOf(p3)).isFalse();
			assertThat(p4.isDescendantOf(p2)).isFalse();
			assertThat(p4.isDescendantOf(p1)).isFalse();
			assertThat(p3.isDescendantOf(p2)).isFalse();
			assertThat(p3.isDescendantOf(p1)).isFalse();
			assertThat(p2b.isDescendantOf(p2)).isFalse();
			assertThat(p2b.isDescendantOf(p1)).isFalse();
			assertThat(p2.isDescendantOf(p1)).isFalse();
		}

		@Test
		void sameDirectoryPathsFromDifferentParentAreNotDescendants() {
			assertThat(p3.isDescendantOf(s3)).isFalse();
			assertThat(p2.isDescendantOf(s2)).isFalse();
			assertThat(p1.isDescendantOf(s1)).isFalse();
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
			assertThat(dirA.isParentOf(dirB)).isFalse();
			assertThat(dirA.isParentOf(dirAA)).isTrue();
			assertThat(dirA.isParentOf(dirAAA)).isTrue();
			assertThat(dirA.isParentOf(dirAAB)).isTrue();
			assertThat(dirA.isParentOf(dirAB)).isTrue();
			assertThat(dirA.isParentOf(dirABA)).isTrue();
			assertThat(dirA.isParentOf(dirABB)).isTrue();
			// B
			assertThat(dirB.isParentOf(dirA)).isFalse();
			assertThat(dirB.isParentOf(dirAA)).isFalse();
			assertThat(dirB.isParentOf(dirAAA)).isFalse();
			assertThat(dirB.isParentOf(dirAAB)).isFalse();
			assertThat(dirB.isParentOf(dirAB)).isFalse();
			assertThat(dirB.isParentOf(dirABA)).isFalse();
			assertThat(dirB.isParentOf(dirABB)).isFalse();
			// AA
			assertThat(dirAA.isParentOf(dirB)).isFalse();
			assertThat(dirAA.isParentOf(dirA)).isFalse();
			assertThat(dirAA.isParentOf(dirAAA)).isTrue();
			assertThat(dirAA.isParentOf(dirAAB)).isTrue();
			assertThat(dirAA.isParentOf(dirAB)).isFalse();
			assertThat(dirAA.isParentOf(dirABA)).isFalse();
			assertThat(dirAA.isParentOf(dirABB)).isFalse();
			// AB
			assertThat(dirAB.isParentOf(dirB)).isFalse();
			assertThat(dirAB.isParentOf(dirAA)).isFalse();
			assertThat(dirAB.isParentOf(dirAAA)).isFalse();
			assertThat(dirAB.isParentOf(dirAAB)).isFalse();
			assertThat(dirAB.isParentOf(dirABA)).isTrue();
			assertThat(dirAB.isParentOf(dirABB)).isTrue();

			// Classes
			// A
			ClassPathNode classA = dirA.child(createEmptyClass(dirA.getValue() + "/TheClass"));
			assertThat(classA.isDescendantOf(dirA)).isTrue();
			assertThat(classA.isDescendantOf(dirAA)).isFalse();
			assertThat(classA.isDescendantOf(dirAB)).isFalse();
			assertThat(classA.isDescendantOf(dirAAA)).isFalse();
			assertThat(classA.isDescendantOf(dirAAB)).isFalse();
			assertThat(classA.isDescendantOf(dirABA)).isFalse();
			assertThat(classA.isDescendantOf(dirABB)).isFalse();
			assertThat(classA.isDescendantOf(dirB)).isFalse();

			// AA
			ClassPathNode classAA = dirAA.child(createEmptyClass(dirAA.getValue() + "/TheClass"));
			assertThat(classAA.isDescendantOf(dirA)).isTrue();
			assertThat(classAA.isDescendantOf(dirAA)).isTrue();
			assertThat(classAA.isDescendantOf(dirAB)).isFalse();
			assertThat(classAA.isDescendantOf(dirAAA)).isFalse();
			assertThat(classAA.isDescendantOf(dirAAB)).isFalse();
			assertThat(classAA.isDescendantOf(dirABA)).isFalse();
			assertThat(classAA.isDescendantOf(dirABB)).isFalse();
			assertThat(classAA.isDescendantOf(dirB)).isFalse();

			// AAA
			ClassPathNode classAAA = dirAAA.child(createEmptyClass(dirAAA.getValue() + "/TheClass"));
			assertThat(classAAA.isDescendantOf(dirA)).isTrue();
			assertThat(classAAA.isDescendantOf(dirAA)).isTrue();
			assertThat(classAAA.isDescendantOf(dirAB)).isFalse();
			assertThat(classAAA.isDescendantOf(dirAAA)).isTrue();
			assertThat(classAAA.isDescendantOf(dirAAB)).isFalse();
			assertThat(classAAA.isDescendantOf(dirABA)).isFalse();
			assertThat(classAAA.isDescendantOf(dirABB)).isFalse();
			assertThat(classAAA.isDescendantOf(dirB)).isFalse();

			// B
			ClassPathNode classB = dirB.child(createEmptyClass(dirB.getValue() + "/TheClass"));
			assertThat(classB.isDescendantOf(dirA)).isFalse();
			assertThat(classB.isDescendantOf(dirAA)).isFalse();
			assertThat(classB.isDescendantOf(dirAB)).isFalse();
			assertThat(classB.isDescendantOf(dirAAA)).isFalse();
			assertThat(classB.isDescendantOf(dirAAB)).isFalse();
			assertThat(classB.isDescendantOf(dirABA)).isFalse();
			assertThat(classB.isDescendantOf(dirABB)).isFalse();
			assertThat(classB.isDescendantOf(dirB)).isTrue();

			// Class to class
			// A
			assertThat(classA.isDescendantOf(classA)).isTrue();
			assertThat(classA.isDescendantOf(classAA)).isFalse();
			assertThat(classA.isDescendantOf(classAAA)).isFalse();
			assertThat(classA.isDescendantOf(classB)).isFalse();

			// AA
			assertThat(classAA.isDescendantOf(classA)).isFalse();
			assertThat(classAA.isDescendantOf(classAA)).isTrue();
			assertThat(classAA.isDescendantOf(classAAA)).isFalse();
			assertThat(classAA.isDescendantOf(classB)).isFalse();

			// AAA
			assertThat(classAAA.isDescendantOf(classA)).isFalse();
			assertThat(classAAA.isDescendantOf(classAA)).isFalse();
			assertThat(classAAA.isDescendantOf(classAAA)).isTrue();
			assertThat(classAAA.isDescendantOf(classB)).isFalse();

			// B
			assertThat(classB.isDescendantOf(classA)).isFalse();
			assertThat(classB.isDescendantOf(classAA)).isFalse();
			assertThat(classB.isDescendantOf(classAAA)).isFalse();
			assertThat(classB.isDescendantOf(classB)).isTrue();
		}
	}

	@Nested
	class Comparison {
		@Test // ignore warning about 'compareTo' on self
		@SuppressWarnings("all")
		void compareToSelfIsZero() {
			// Self comparison or equal items should always be 0
			assertThatComparable(p1).isEqualTo(p1);
			assertThatComparable(p2).isEqualTo(p2);
			assertThatComparable(p3).isEqualTo(p3);
			assertThatComparable(p4).isEqualTo(p4);
			assertThatComparable(p5).isEqualTo(p5);
		}

		@Test
		void compareToParentIsGreater() {
			// Children appear last (thus > 0)
			assertThatComparable(p1).isGreaterThan(p2);
			assertThatComparable(p2).isGreaterThan(p3);
			assertThatComparable(p3).isGreaterThan(p4);
			assertThatComparable(p4).isGreaterThan(p5);
		}

		@Test
		void compareToChildIsLess() {
			// Parents appear first (thus < 0)
			assertThatComparable(p5).isLessThan(p4);
			assertThatComparable(p5).isLessThan(p3);
			assertThatComparable(p5).isLessThan(p2);
			assertThatComparable(p5).isLessThan(p1);
			assertThatComparable(p4).isLessThan(p3);
			assertThatComparable(p4).isLessThan(p2);
			assertThatComparable(p4).isLessThan(p1);
			assertThatComparable(p3).isLessThan(p2);
			assertThatComparable(p3).isLessThan(p1);
			assertThatComparable(p2).isLessThan(p1);
		}

		@Test
		void compareToOtherBundleTypes() {
			// 1: JVM
			// 2: Versioned
			// 3: Android
			// 4: File
			assertThatComparable(p3).isLessThan(p3and1);
			assertThatComparable(p3).isLessThan(p3file);
			assertThatComparable(p3and1).isLessThan(p3file);
			assertThatComparable(p3and1).isLessThan(p3and2);

			// Inverse
			assertThatComparable(p3and1).isGreaterThan(p3);
			assertThatComparable(p3file).isGreaterThan(p3);
			assertThatComparable(p3file).isGreaterThan(p3and1);
			assertThatComparable(p3and2).isGreaterThan(p3and1);
		}
	}

	@Nested
	class Misc {
		@Test
		void bundleInTarget() {
			// JVM bundle
			assertThat(p3.isInJvmBundle()).isTrue();
			assertThat(p3.isInVersionedJvmBundle()).isFalse();
			assertThat(p3.isInAndroidBundle()).isFalse();
			assertThat(p3.isInFileBundle()).isFalse();

			// Android bundle
			assertThat(p3and1.isInJvmBundle()).isFalse();
			assertThat(p3and1.isInVersionedJvmBundle()).isFalse();
			assertThat(p3and1.isInAndroidBundle()).isTrue();
			assertThat(p3and1.isInFileBundle()).isFalse();

			// File bundle
			assertThat(p3file.isInJvmBundle()).isFalse();
			assertThat(p3file.isInVersionedJvmBundle()).isFalse();
			assertThat(p3file.isInAndroidBundle()).isFalse();
			assertThat(p3file.isInFileBundle()).isTrue();
		}
	}
}