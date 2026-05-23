package software.coley.recaf.services.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.services.analysis.android.AndroidAnalysisService;
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

}
