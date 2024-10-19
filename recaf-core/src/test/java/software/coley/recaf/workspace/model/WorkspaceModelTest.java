package software.coley.recaf.workspace.model;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.StubFileInfo;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.test.dummy.AccessibleFields;
import software.coley.recaf.test.dummy.AccessibleMethods;
import software.coley.recaf.test.dummy.AccessibleMethodsChild;
import software.coley.recaf.test.dummy.ClassWithAnnotation;
import software.coley.recaf.test.dummy.ClassWithConstructor;
import software.coley.recaf.test.dummy.ClassWithExceptions;
import software.coley.recaf.workspace.model.bundle.BasicAndroidClassBundle;
import software.coley.recaf.workspace.model.bundle.Bundle;
import software.coley.recaf.workspace.model.resource.AndroidApiResource;
import software.coley.recaf.workspace.model.resource.RuntimeWorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static software.coley.recaf.test.TestClassUtils.fromClasses;

/**
 * Tests for the {@link Workspace} model.
 */
class WorkspaceModelTest {
	@Nested
	class ResourceModel {
		@Test
		void getPrimaryResource() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary);
			assertSame(primary, workspace.getPrimaryResource());
		}

		@Test
		void getSupportingResources() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			WorkspaceResource support1 = new WorkspaceResourceBuilder().build();
			WorkspaceResource support2 = new WorkspaceResourceBuilder().build();
			WorkspaceResource support3 = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary, List.of(support1, support2, support3));
			List<WorkspaceResource> supportingResources = workspace.getSupportingResources();
			assertEquals(3, supportingResources.size());
			assertTrue(supportingResources.containsAll(List.of(support1, support2, support3)));
		}

		@Test
		void getInternalSupportingResources_JVM() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary);

			// Will only contain the runtime resource
			List<WorkspaceResource> internal = workspace.getInternalSupportingResources();
			assertEquals(1, internal.size());
			assertTrue(internal.contains(RuntimeWorkspaceResource.getInstance()));
		}

		@Test
		void getInternalSupportingResources_Android() {
			WorkspaceResource primary = new WorkspaceResourceBuilder()
					.withAndroidClassBundles(Map.of("classes.dex", new BasicAndroidClassBundle()))
					.build();
			Workspace workspace = new BasicWorkspace(primary);
			List<WorkspaceResource> internal = workspace.getInternalSupportingResources();

			// Will contain the runtime resource AND the Android API resource since android content was detected
			assertEquals(2, internal.size());
			assertTrue(internal.contains(RuntimeWorkspaceResource.getInstance()));
			assertTrue(internal.contains(AndroidApiResource.getInstance()));
		}

		@Test
		void addAndRemoveSupportingResource() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary);

			// No supporting resources by default
			assertTrue(workspace.getSupportingResources().isEmpty());

			// Add a listener
			WorkspaceModificationListener listener = mock(WorkspaceModificationListener.class);
			workspace.addWorkspaceModificationListener(listener);

			// Add a supporting resource
			WorkspaceResource supporting = new WorkspaceResourceBuilder().build();
			workspace.addSupportingResource(supporting);

			// The listener should be called, and we should see it in the list
			verify(listener).onAddLibrary(same(workspace), same(supporting));
			assertEquals(1, workspace.getSupportingResources().size());

			// Same idea in reverse
			assertTrue(workspace.removeSupportingResource(supporting));
			verify(listener).onRemoveLibrary(same(workspace), same(supporting));
			assertTrue(workspace.getSupportingResources().isEmpty());
		}

		@Test
		void getAllResources() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			WorkspaceResource supporting1 = new WorkspaceResourceBuilder().build();
			WorkspaceResource supporting2 = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary, List.of(supporting1, supporting2));

			// Should give us the primary/supporting resources in order.
			List<WorkspaceResource> streamResult = workspace.allResourcesStream(false).toList();
			assertEquals(List.of(primary, supporting1, supporting2), streamResult);

			// Should give us the expected primary/supporting resources, then the internal resources as well in order.
			streamResult = workspace.allResourcesStream(true).toList();
			assertEquals(List.of(primary, supporting1, supporting2, RuntimeWorkspaceResource.getInstance()), streamResult);
		}
	}

	@Nested
	class Finding {
		@Test
		void findClass() throws IOException {
			WorkspaceResource primary = new WorkspaceResourceBuilder()
					.withJvmClassBundle(fromClasses(
							AccessibleFields.class,
							AccessibleMethods.class,
							AccessibleMethodsChild.class
					)).build();
			WorkspaceResource supporting = new WorkspaceResourceBuilder()
					.withJvmClassBundle(fromClasses(
							ClassWithAnnotation.class,
							ClassWithConstructor.class,
							ClassWithExceptions.class
					)).build();
			Workspace workspace = new BasicWorkspace(primary, List.of(supporting));

			// Find a class in the primary resource
			ClassPathNode result = findClass(workspace, AccessibleFields.class);
			assertNotNull(result);
			assertSame(primary, result.getValueOfType(WorkspaceResource.class));
			assertSame(workspace, result.getValueOfType(Workspace.class));

			// Find a class in the secondary resource
			result = findClass(workspace, ClassWithExceptions.class);
			assertNotNull(result);
			assertSame(supporting, result.getValueOfType(WorkspaceResource.class));
			assertSame(workspace, result.getValueOfType(Workspace.class));
		}

		@Test
		void findClass_fromRuntime() {
			WorkspaceResource primary = new WorkspaceResourceBuilder().build();
			Workspace workspace = new BasicWorkspace(primary);

			// Find a class that exists in the runtime, but not explicitly in the provided resources of the workspace
			ClassPathNode result = findClass(workspace, String.class);
			assertNotNull(result);
			assertSame(RuntimeWorkspaceResource.getInstance(), result.getValueOfType(WorkspaceResource.class));
			assertSame(workspace, result.getValueOfType(Workspace.class));
		}

		@Test
		void findClass_embeddedInPrimary() throws IOException {
			WorkspaceFileResource embeddedResource = new WorkspaceFileResourceBuilder()
					.withFileInfo(new StubFileInfo("embedded.jar"))
					.withJvmClassBundle(fromClasses(
							AccessibleFields.class
					)).build();
			WorkspaceResource primary = new WorkspaceResourceBuilder()
					.withEmbeddedResources(Map.of("embedded.jar", embeddedResource))
					.build();
			Workspace workspace = new BasicWorkspace(primary);

			// Find a class in the primary resource's embedded resource
			ClassPathNode result = findClass(workspace, AccessibleFields.class);
			assertNotNull(result);

			// The path is "normalized" in a way that leads to the "primary" resource being listed in the path.
			// However, the bundle is still the correct directly related one.
			var bundle = result.getValueOfType(Bundle.class);
			assertNotNull(bundle);
			assertSame(primary, result.getValueOfType(WorkspaceResource.class));
			assertSame(embeddedResource.getJvmClassBundle(), bundle);

			// We can undo the "normalization" by doing a bundle lookup.
			assertSame(embeddedResource, primary.resolveBundleContainer(bundle));
			assertSame(primary, embeddedResource.getContainingResource());
		}

		@Test
		void findClass_embeddedDeeperInPrimary() throws IOException {
			// Build the following model:
			//  > 1.jar
			//   > ...
			//    > 5.jar
			//      - AccessibleFields.class
			WorkspaceFileResourceBuilder embeddedBuilder =
					new WorkspaceFileResourceBuilder().withFileInfo(new StubFileInfo("1.jar")).withEmbeddedResources(Map.of("2.jar",
							new WorkspaceFileResourceBuilder().withFileInfo(new StubFileInfo("2.jar")).withEmbeddedResources(Map.of("3.jar",
									new WorkspaceFileResourceBuilder().withFileInfo(new StubFileInfo("3.jar")).withEmbeddedResources(Map.of("4.jar",
											new WorkspaceFileResourceBuilder().withFileInfo(new StubFileInfo("4.jar")).withEmbeddedResources(Map.of("5.jar",
													new WorkspaceFileResourceBuilder().withFileInfo(new StubFileInfo("5.jar")).withJvmClassBundle(fromClasses(
															AccessibleFields.class
													)).build())
											).build())
									).build())
							).build())
					);


			// Build the workspace
			WorkspaceResource primary = new WorkspaceResourceBuilder()
					.withEmbeddedResources(Map.of("1.jar", embeddedBuilder.build()))
					.build();
			Workspace workspace = new BasicWorkspace(primary);

			// Find a class in the primary resource's embedded resource
			ClassPathNode result = findClass(workspace, AccessibleFields.class);
			assertNotNull(result);

			// From the prior test, we know about normalization and how to find the true embedded resource the class resides in.
			var bundle = result.getValueOfType(Bundle.class);
			assertNotNull(bundle);
			WorkspaceResource valueOfType = result.getValueOfType(WorkspaceResource.class);
			assertSame(primary, valueOfType);
			var resolvedBundleContainer = ((WorkspaceFileResource) primary.resolveBundleContainer(bundle));
			assertNotNull(resolvedBundleContainer);
			assertEquals("5.jar", resolvedBundleContainer.getFileInfo().getName());

			// The predicate-based find operation should yield the same path
			SortedSet<ClassPathNode> allClassPaths = workspace.findClasses(c -> true);
			assertEquals(1, allClassPaths.size());
			assertEquals(result, allClassPaths.first());
		}

		/**
		 * @param workspace
		 * 		Workspace to search in.
		 * @param type
		 * 		Class to look for.
		 *
		 * @return Path to resource in the workspace.
		 */
		@Nullable
		private static ClassPathNode findClass(@Nonnull Workspace workspace, @Nonnull Class<?> type) {
			return workspace.findClass(type.getName().replace('.', '/'));
		}
	}
}