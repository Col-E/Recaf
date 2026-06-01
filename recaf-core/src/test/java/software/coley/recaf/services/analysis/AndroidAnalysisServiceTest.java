package software.coley.recaf.services.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.services.analysis.android.AndroidAnalysisService;
import software.coley.recaf.services.analysis.android.AndroidPermissionDetails;
import software.coley.recaf.services.analysis.android.AndroidPermissionEntry;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.manifestElement;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.mockManifest;
import static software.coley.recaf.test.TestClassUtils.fromFiles;

/**
 * Tests for {@link AndroidAnalysisService}.
 */
class AndroidAnalysisServiceTest extends TestBase {
	private static AndroidAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(AndroidAnalysisService.class);
	}

	@Test
	void skipsMissingManifest() {
		// Safely return when no manifest is found.
		WorkspaceResource primary = new WorkspaceResourceBuilder().build();
		Workspace workspace = new BasicWorkspace(primary);
		assertEquals(0, service.findRequestedPermissions(workspace, primary).size());
	}

	@Test
	void skipsManifestWithoutStringPool() {
		// If the input is malformed such that we have a manifest but there is no string pool, we should also safely return.
		BinaryXmlFileInfo manifest = mock(BinaryXmlFileInfo.class);
		when(manifest.getName()).thenReturn("AndroidManifest.xml");
		when(manifest.getRawContent()).thenReturn(new byte[0]);
		FileBundle files = fromFiles(manifest);
		WorkspaceResource primary = new WorkspaceResourceBuilder().withFileBundle(files).build();
		Workspace workspace = new BasicWorkspace(primary);
		assertEquals(0, service.findRequestedPermissions(workspace, primary).size());
	}

	@Test
	void findsPermissionsInEmbeddedManifest() {
		// Mock an AndroidManifest.xml that requests a permission.
		String permission = "android.permission.INTERNET";
		BinaryXmlFileInfo manifest = mockManifest(
				manifestElement("uses-permission", Map.of("name", permission))
		);

		// Even if its embedded, we should still find the permission request.
		FileBundle embeddedFiles = fromFiles(manifest);
		WorkspaceFileResource embedded = new WorkspaceFileResourceBuilder()
				.withFileInfo(new software.coley.recaf.info.StubFileInfo("embedded.apk"))
				.withFileBundle(embeddedFiles)
				.build();
		WorkspaceResource primary = new WorkspaceResourceBuilder()
				.withEmbeddedResources(Map.of("embedded.apk", embedded))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// Verify we find the permission and that the manifest path is correct.
		List<AndroidPermissionEntry> results = service.findRequestedPermissions(workspace, primary);
		assertEquals(1, results.size());
		assertEquals(permission, results.getFirst().permission());
		assertSame(embedded, results.getFirst().manifestPath().getValueOfType(WorkspaceResource.class));
	}

	@Test
	void resolvesPermissionLevels() {
		// Mock an AndroidManifest.xml that requests a few permissions.
		BinaryXmlFileInfo manifest = mockManifest(
				manifestElement("uses-permission", Map.of("name", "android.permission.INTERNET")),
				manifestElement("uses-permission", Map.of("name", "android.permission.ACCEPT_HANDOVER")),
				manifestElement("uses-permission", Map.of("name", "com.example.CUSTOM_PERMISSION"))
		);
		FileBundle files = fromFiles(manifest);
		WorkspaceResource primary = new WorkspaceResourceBuilder().withFileBundle(files).build();
		Workspace workspace = new BasicWorkspace(primary);

		// We should find all three permissions and resolve their levels.
		List<AndroidPermissionDetails> results = service.findRequestedPermissionDetails(workspace, primary);
		assertEquals(3, results.size());

		// INTERNET,normal
		AndroidPermissionDetails internet = results.get(0);
		assertEquals("android.permission.INTERNET", internet.entry().permission());
		assertEquals(1, internet.levels().size());
		assertEquals("normal", internet.levels().getFirst().baseLevel());
		assertEquals("normal", internet.levels().getFirst().rawLevel());

		// ACCEPT_HANDOVER,signature|privileged|development
		AndroidPermissionDetails acceptHandover = results.get(1);
		assertEquals("android.permission.ACCEPT_HANDOVER", acceptHandover.entry().permission());
		assertEquals(2, acceptHandover.levels().size());
		assertEquals("dangerous", acceptHandover.levels().get(0).baseLevel());
		assertEquals("dangerous", acceptHandover.levels().get(0).rawLevel());
		assertEquals("signature", acceptHandover.levels().get(1).baseLevel());
		assertEquals("signature|privileged|development", acceptHandover.levels().get(1).rawLevel());

		// Not something in our known list, so we should get "unknown" for the level.
		AndroidPermissionDetails custom = results.get(2);
		assertEquals("com.example.CUSTOM_PERMISSION", custom.entry().permission());
		assertEquals(1, custom.levels().size());
		assertEquals("unknown", custom.levels().getFirst().baseLevel());
		assertEquals("unknown", custom.levels().getFirst().rawLevel());
	}

	@Test
	void preservesManifestPathInPermissionDetails() {
		// Mock an AndroidManifest.xml that requests a permission.
		String permission = "android.permission.INTERNET";
		BinaryXmlFileInfo manifest = mockManifest(
				manifestElement("uses-permission", Map.of("name", permission))
		);

		// Put it in an embedded resource.
		FileBundle embeddedFiles = fromFiles(manifest);
		WorkspaceFileResource embedded = new WorkspaceFileResourceBuilder()
				.withFileInfo(new software.coley.recaf.info.StubFileInfo("embedded.apk"))
				.withFileBundle(embeddedFiles)
				.build();
		WorkspaceResource primary = new WorkspaceResourceBuilder()
				.withEmbeddedResources(Map.of("embedded.apk", embedded))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// We should find the permission and the manifest path
		// in the details should point to the embedded resource, not the primary.
		List<AndroidPermissionDetails> results = service.findRequestedPermissionDetails(workspace, primary);
		assertEquals(1, results.size());
		assertSame(embedded, results.getFirst().entry().manifestPath().getValueOfType(WorkspaceResource.class));
	}

}
