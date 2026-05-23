package software.coley.recaf.services.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.BinaryXmlFileInfo;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.services.analysis.structure.EntryPointGroup;
import software.coley.recaf.services.analysis.structure.EntryPointSource;
import software.coley.recaf.services.analysis.structure.FlowAnalysisService;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResourceBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.manifestElement;
import static software.coley.recaf.services.analysis.AnalysisTestUtils.mockManifest;
import static software.coley.recaf.test.TestClassUtils.*;
import static software.coley.recaf.test.TestDexUtils.newAndroidClass;

/**
 * Tests for {@link FlowAnalysisService}.
 */
class FlowAnalysisServiceTest extends TestBase {
	private static FlowAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(FlowAnalysisService.class);
	}

	@Test
	void returnsEmptyWhenNoEntryPointsExist() {
		// Safely return when no entry points are found.
		WorkspaceResource primary = new WorkspaceResourceBuilder().build();
		Workspace workspace = new BasicWorkspace(primary);
		assertEquals(0, service.findEntryPointGroups(workspace, primary).size());
	}

	@Test
	void findsJvmMainMethodGroup() throws IOException {
		// Create a workspace with a single class that has a main method.
		Workspace workspace = fromBundle(fromClasses(HelloWorld.class));

		// We should find the main method as an entry point.
		List<EntryPointGroup> results = service.findEntryPointGroups(workspace, workspace.getPrimaryResource());
		assertEquals(1, results.size());
		EntryPointGroup group = results.getFirst();
		ClassMemberPathNode path = group.memberEntryPoints().getFirst();
		assertEquals(EntryPointSource.JVM_MAIN_METHOD, group.source());
		assertEquals(HelloWorld.class.getName().replace('.', '/'), group.classPath().getValue().getName());
		assertEquals(1, group.memberEntryPoints().size());
		assertEquals("main", path.getValue().getName());
	}

	@Test
	void findsAndroidActivityGroup() {
		// Mock an AndroidManifest.xml with a single activity entry.
		BinaryXmlFileInfo manifest = mockManifest(manifestElement("activity", Map.of("name", "foo.MainActivity")));

		// Wrap into workspace with a dummy class for that activity.
		FileBundle files = fromFiles(manifest);
		WorkspaceResource primary = new WorkspaceResourceBuilder()
				.withAndroidClassBundles(Map.of("classes.dex", fromClasses(newAndroidClass("foo/MainActivity"))))
				.withFileBundle(files)
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// We should find the mocked entry point.
		List<EntryPointGroup> results = service.findEntryPointGroups(workspace, primary);
		assertEquals(1, results.size());
		EntryPointGroup group = results.getFirst();
		assertEquals(EntryPointSource.ANDROID_ACTIVITY, group.source());
		assertEquals("foo/MainActivity", group.classPath().getValue().getName());
		assertEquals(0, group.memberEntryPoints().size());
	}
}
