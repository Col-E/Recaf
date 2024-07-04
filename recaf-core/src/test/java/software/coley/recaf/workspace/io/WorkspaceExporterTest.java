package software.coley.recaf.workspace.io;

import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.services.workspace.io.*;
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
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

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
		test(WorkspaceOutputType.FILE);
	}

	@Test
	void testDirectoryExportDoesNotTamperResourceModel() throws IOException {
		test(WorkspaceOutputType.DIRECTORY);
	}

	private static void test(WorkspaceOutputType outputType) throws IOException {
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

		WorkspaceExportOptions options = new WorkspaceExportOptions(outputType, new PathWorkspaceExportConsumer(temp));
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

	/**
	 * There's a lombok fabric mod which bundles some classes with tampered names. The file contents are normal classes.
	 * When we re-export the workspace we need to ensure the classes are written back to where they originally came from.
	 */
	@Test
	void testLombokClassPrefixSuffixExport() throws IOException {
		byte[] inputZipBytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/name-prefix-suffix.jar"));
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(inputZipBytes));
		assertNotNull(resource.getJvmClassBundle().get("org/objectweb/asm/Constants"), "Missing ASM classes");
		BasicWorkspace workspace = new BasicWorkspace(resource);

		// Export the workspace
		Set<String> paths = new TreeSet<>();
		new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY, new WorkspaceExportConsumer() {
			@Override
			public void write(@Nonnull byte[] bytes) {
				throw new RuntimeException("Should not be invoked in directory output type");
			}

			@Override
			public void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) {
				paths.add(relative);
			}

			@Override
			public void commit() {
				// no-op
			}
		}).create().export(workspace);

		// Verify the paths match the input names
		assertTrue(paths.contains("SCL.lombok/org/objectweb/asm/Constants.SCL.lombok"),
				"Lombok's bundled ASM classes not written to expected path");
	}

	@Test
	void testNameDifferenceExport() throws IOException {
		byte[] inputZipBytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/name-difference.zip"));
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(inputZipBytes));
		assertNotNull(resource.getJvmClassBundle().get("org/objectweb/asm/Constants"), "Missing ASM classes");
		BasicWorkspace workspace = new BasicWorkspace(resource);

		// Export the workspace
		Set<String> paths = new TreeSet<>();
		new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY, new WorkspaceExportConsumer() {
			@Override
			public void write(@Nonnull byte[] bytes) {
				throw new RuntimeException("Should not be invoked in directory output type");
			}

			@Override
			public void writeRelative(@Nonnull String relative, @Nonnull byte[] bytes) {
				paths.add(relative);
			}

			@Override
			public void commit() {
				// no-op
			}
		}).create().export(workspace);

		// Verify the paths match the input names
		assertTrue(paths.contains("SomethingElse.bin"), "Class was not written back to expected name");
	}
}