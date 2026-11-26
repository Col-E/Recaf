package software.coley.recaf.ui.control.richtext.suggest.typeindex;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.StubClassInfo;
import software.coley.recaf.info.StubFileInfo;
import software.coley.recaf.services.workspace.WorkspaceManager;
import software.coley.recaf.ui.control.richtext.suggest.lookups.TypeVisibility;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.BasicJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.BasicVersionedJvmClassBundle;
import software.coley.recaf.workspace.model.bundle.JvmClassBundle;
import software.coley.recaf.workspace.model.bundle.VersionedJvmClassBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TypeIndexService}.
 */
class TypeIndexServiceTest {
	@Test
	void reusesCachedIndexAndAppliesAddedClassIncrementally() {
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		Workspace workspace = workspaceWith("example/Foo");

		// Multiple get calls should return the same index instance while we have the same workspace.
		TypeIndex first = service.getIndex(workspace);
		TypeIndex second = service.getIndex(workspace);
		assertSame(first, second);

		// Update a class in the workspace.
		workspace.getPrimaryResource().getJvmClassBundle().put(new StubClassInfo("example/Bar").asJvmClass());

		// We should still be operating in the same index, but the underlying entries should be updated to reflect the new class.
		TypeIndex updated = service.getIndex(workspace);
		assertSame(first, updated);
		assertNotNull(updated.findType("example.Bar"));
		assertEquals(2, updated.typesInPackage("example").size());
		assertTrue(updated.childPackages("").contains("example"));
	}

	@Test
	void updatesRenamedClassWithoutStaleEntries() {
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		Workspace workspace = workspaceWith("example/Foo");
		JvmClassBundle bundle = workspace.getPrimaryResource().getJvmClassBundle();

		TypeIndex index = service.getIndex(workspace);

		// Really shoddy simulation of a rename.
		JvmClassInfo renamed = new StubClassInfo("other/Bar").asJvmClass();
		bundle.remove("example/Foo");
		bundle.put(renamed);

		// Again, same index, but the old type should be gone.
		TypeIndex updated = service.getIndex(workspace);
		assertSame(index, updated);
		assertNull(updated.findType("example.Foo"));

		// The new type should be present.
		TypeCandidate renamedCandidate = updated.findType("other.Bar");
		assertNotNull(renamedCandidate);
		assertNotNull(renamedCandidate.path());
		assertSame(renamed, renamedCandidate.path().getValue());
		assertTrue(updated.typesInPackage("example").isEmpty());
		assertEquals(List.of("other"), updated.childPackages("").stream().sorted().toList());
	}

	@Test
	void promotesShadowedDuplicateWhenWinnerRemoved() {
		// Put 'Foo' in both the primary and a supporting resource.
		// The primary should win, but when it is removed the supporting one should be promoted.
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		JvmClassInfo primaryFoo = new StubClassInfo("example/Foo").asJvmClass();
		JvmClassInfo supportFoo = new StubClassInfo("example/Foo").asJvmClass();
		BasicJvmClassBundle primaryBundle = new BasicJvmClassBundle();
		BasicJvmClassBundle supportBundle = new BasicJvmClassBundle();
		primaryBundle.put(primaryFoo);
		supportBundle.put(supportFoo);
		Workspace workspace = new BasicWorkspace(new WorkspaceResourceBuilder()
				.withJvmClassBundle(primaryBundle)
				.build(), List.of(new WorkspaceResourceBuilder()
				.withJvmClassBundle(supportBundle)
				.build()), false);

		TypeIndex index = service.getIndex(workspace);

		// Show the primary 'Foo' wins initially.
		TypeCandidate initialWinner = index.findType("example.Foo");
		assertNotNull(initialWinner);
		assertNotNull(initialWinner.path());
		assertSame(primaryFoo, initialWinner.path().getValue());

		// Remove the primary 'Foo'.
		primaryBundle.remove("example/Foo");

		// The supporting 'Foo' should be promoted to winner, and there should be no stale entries for the removed primary 'Foo'.
		TypeIndex updated = service.getIndex(workspace);
		TypeCandidate promotedWinner = updated.findType("example.Foo");
		assertSame(index, updated);
		assertNotNull(promotedWinner);
		assertNotNull(promotedWinner.path());
		assertSame(supportFoo, promotedWinner.path().getValue());
	}

	@Test
	void addsAndRemovesLibraryIncrementallyIncludingEmbeddedAndVersionedClasses() {
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		Workspace workspace = workspaceWith("example/Host");
		TypeIndex index = service.getIndex(workspace);

		// Giant blob of supporting resource setup.
		BasicJvmClassBundle libraryBundle = new BasicJvmClassBundle();
		libraryBundle.put(new StubClassInfo("lib/Plain").asJvmClass());
		BasicVersionedJvmClassBundle versionedBundle = new BasicVersionedJvmClassBundle(17);
		versionedBundle.put(new StubClassInfo("lib/Versioned").asJvmClass());
		NavigableMap<Integer, VersionedJvmClassBundle> versionedBundles = new TreeMap<>();
		versionedBundles.put(17, versionedBundle);
		BasicJvmClassBundle embeddedBundle = new BasicJvmClassBundle();
		embeddedBundle.put(new StubClassInfo("nested/Embedded").asJvmClass());
		WorkspaceFileResource embedded = new WorkspaceFileResourceBuilder()
				.withFileInfo(new StubFileInfo("embedded.jar"))
				.withJvmClassBundle(embeddedBundle)
				.build();
		WorkspaceResource library = new WorkspaceFileResourceBuilder()
				.withFileInfo(new StubFileInfo("library.jar"))
				.withJvmClassBundle(libraryBundle)
				.withVersionedJvmClassBundles(versionedBundles)
				.withEmbeddedResources(Map.of("embedded.jar", embedded))
				.build();

		// Add the library and check all classes are present.
		workspace.addSupportingResource(library);
		TypeIndex added = service.getIndex(workspace);
		assertSame(index, added);
		assertNotNull(added.findType("lib.Plain"));
		assertNotNull(added.findType("lib.Versioned"));
		assertNotNull(added.findType("nested.Embedded"));

		// Remove the library and check all classes are gone.
		workspace.removeSupportingResource(library);
		TypeIndex removed = service.getIndex(workspace);
		assertSame(index, removed);
		assertNull(removed.findType("lib.Plain"));
		assertNull(removed.findType("lib.Versioned"));
		assertNull(removed.findType("nested.Embedded"));
	}

	@Test
	void typesWithInternalNamePrefixKeepsNestedAndPackagedNamesDistinct() {
		// Make a workspace with two classes that have unique internal names, but the same simple name and package.
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		Workspace workspace = workspaceWith("a/b$c", "a/b/c");
		TypeIndex index = service.getIndex(workspace);

		// Both the nested and packaged types should be present, and distinct.
		Set<String> internalNames = index.typesWithInternalNamePrefix("a/b").stream()
				.map(TypeCandidate::internalName)
				.collect(Collectors.toSet());
		assertEquals(Set.of("a/b$c", "a/b/c"), internalNames);

		// If we look for the same type twice, we should get the same candidate back (no duplicates).
		TypeCandidate first = index.findType("a.b.c");
		TypeCandidate second = index.findType("a.b.c");
		assertNotNull(first);
		assertSame(first, second);
		assertTrue(Set.of("a/b$c", "a/b/c").contains(first.internalName()));
	}

	@Test
	void typesWithSimpleNamePrefixTracksDistinctAndUpdatedCandidates() {
		// Make a workspace with two classes that have the same simple name, but different internal names.
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		Workspace workspace = workspaceWith("a/Shared", "b/Shared", "a/SharedType");
		TypeIndex index = service.getIndex(workspace);

		// All three types should be present, and distinct.
		Set<String> initial = index.typesWithSimpleNamePrefix("Share").stream()
				.map(TypeCandidate::internalName)
				.collect(Collectors.toSet());
		assertEquals(Set.of("a/Shared", "b/Shared", "a/SharedType"), initial);

		// Adding a new class with the same prefix but a distinct suffix will update the index.
		workspace.getPrimaryResource().getJvmClassBundle().put(new StubClassInfo("c/SharedNew").asJvmClass());
		assertTrue(service.getIndex(workspace).typesWithSimpleNamePrefix("Share").stream()
				.map(TypeCandidate::internalName).collect(Collectors.toSet())
				.contains("c/SharedNew"));
		assertEquals(Set.of("c/SharedNew"), service.getIndex(workspace).typesWithCamelHumpPrefix("ShN").stream()
				.map(TypeCandidate::internalName).collect(Collectors.toSet()));

		// Removing a class also removes its indexed camel-hump entry.
		workspace.getPrimaryResource().getJvmClassBundle().remove("c/SharedNew");
		assertTrue(service.getIndex(workspace).typesWithCamelHumpPrefix("ShN").isEmpty());

		// Removing one of the original classes will also update the simple-name index.
		workspace.getPrimaryResource().getJvmClassBundle().remove("a/Shared");
		Set<String> afterRemoval = service.getIndex(workspace).typesWithSimpleNamePrefix("Share").stream()
				.map(TypeCandidate::internalName)
				.collect(Collectors.toSet());
		assertEquals(Set.of("b/Shared", "a/SharedType"), afterRemoval);
	}

	@Test
	void typesWithCamelHumpPrefixMatchesFragmentsAndInitials() {
		WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
		TypeIndexService service = new TypeIndexService(workspaceManager);
		TypeIndex index = service.getIndex(workspaceWith(
				"java/lang/StringBuilder",
				"java/lang/StringBuffer",
				"java/util/concurrent/ConcurrentHashMap",
				"example/YouAreNotGoingToMatchThis"));

		// StrB is a camel-hump match for both StringBuilder and StringBuffer.
		assertEquals(Set.of("java/lang/StringBuilder", "java/lang/StringBuffer"),
				index.typesWithCamelHumpPrefix("StrB").stream()
						.map(TypeCandidate::internalName).collect(Collectors.toSet()));

		// SB is also a match for both.
		assertEquals(Set.of("java/lang/StringBuilder", "java/lang/StringBuffer"),
				index.typesWithCamelHumpPrefix("SB").stream()
						.map(TypeCandidate::internalName).collect(Collectors.toSet()));

		// CHM is a match for ConcurrentHashMap.
		assertEquals(Set.of("java/util/concurrent/ConcurrentHashMap"),
				index.typesWithCamelHumpPrefix("CHM").stream()
						.map(TypeCandidate::internalName).collect(Collectors.toSet()));
	}

	@Test
	void filtersPackagePrivateTypesOutsideCurrentPackage() {
		TypeCandidate candidate = new TypeCandidate(
				"HiddenType",
				"other.HiddenType",
				"other/HiddenType",
				"other",
				false,
				0,
				null
		);
		assertFalse(TypeVisibility.isAccessibleType(candidate, "example"));
		assertTrue(TypeVisibility.isAccessibleType(candidate, "other"));
	}

	@Test
	void keepsPublicTypesVisibleAcrossPackages() {
		TypeCandidate candidate = new TypeCandidate(
				"VisibleType",
				"other.VisibleType",
				"other/VisibleType",
				"other",
				false,
				Opcodes.ACC_PUBLIC,
				null
		);
		assertTrue(TypeVisibility.isAccessibleType(candidate, "example"));
	}

	@Nonnull
	private static Workspace workspaceWith(String... classNames) {
		return new BasicWorkspace(resourceWith(classNames), Collections.emptyList(), false);
	}

	@Nonnull
	private static WorkspaceResource resourceWith(String... classNames) {
		BasicJvmClassBundle bundle = new BasicJvmClassBundle();
		for (String className : classNames)
			bundle.put(new StubClassInfo(className).asJvmClass());
		return new WorkspaceResourceBuilder()
				.withJvmClassBundle(bundle)
				.build();
	}
}
