package software.coley.recaf.workspace.io;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.ZipCreationUtils;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WorkspaceExporter}
 */
class WorkspaceExporterTest {
	static ResourceImporter importer;

	@BeforeAll
	static void setup() {
		importer = new BasicResourceImporter(
				new BasicInfoImporter(new InfoImporterConfig(), new BasicClassPatcher()),
				new ResourceImporterConfig()
		);
	}

	@Test
	void testFileExportDoesNotTamperResourceModel() throws IOException {
		test(WorkspaceExportOptions.OutputType.FILE);
	}

	@Test
	void testDirectoryExportDoesNotTamperResourceModel() throws IOException {
		test(WorkspaceExportOptions.OutputType.DIRECTORY);
	}

	private static void test(WorkspaceExportOptions.OutputType outputType) throws IOException {
		// Create test ZIP in memory
		byte[] embeddedZipBytes = ZipCreationUtils.createSingleEntryZip("inside.txt", new byte[0]);
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] helloWorldBytes = TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode();
		byte[] targetZipBytes = ZipCreationUtils.builder()
				.add(helloWorldPath + ".class", helloWorldBytes)
				.add(JarFileInfo.MULTI_RELEASE_PREFIX + "9/" + helloWorldPath + ".class", helloWorldBytes)
				.add("hello.txt", "hello world".getBytes(StandardCharsets.UTF_8))
				.add("test.zip", embeddedZipBytes)
				.bytes();

		// Workspace sourced from ZIP
		WorkspaceResource targetResource = importer.importResource(ByteSources.wrap(targetZipBytes));
		Workspace workspace = new BasicWorkspace(targetResource);

		// Export the resource
		Path temp;
		switch (outputType) {
			case DIRECTORY:
				temp = Files.createTempDirectory("recaf");
				temp.toFile().deleteOnExit();
				break;
			case FILE:
			default:
				temp = Files.createTempFile("recaf", "test.zip");
				temp.toFile().deleteOnExit();
				break;
		}

		WorkspaceExportOptions options = new WorkspaceExportOptions(outputType, temp);
		options.setCreateZipDirEntries(true);
		options.setBundleSupporting(false);
		WorkspaceExporter exporter = options.create();
		exporter.export(workspace);

		// Assert it was written
		switch (outputType) {
			case DIRECTORY:
				assertTrue(Files.list(temp).findAny().isPresent(),
						"Temp directory location for exported workspace not written to!");
				break;
			case FILE:
			default:
				assertTrue(Files.size(temp) > 0, "" +
						"Temp zip location for exported workspace not written to!");
				break;
		}

		// Now read it back and ensure it is the same.
		// Because one is a file-backed resource and the other directory-backed, we need to compare the contents rather
		// than the whole resource.
		WorkspaceResource importedResource = importer.importResource(temp);
		assertEquals(targetResource.getJvmClassBundle(), importedResource.getJvmClassBundle());
		assertEquals(targetResource.getFileBundle(), importedResource.getFileBundle());
		assertEquals(targetResource.getEmbeddedResources(), importedResource.getEmbeddedResources());
	}
}