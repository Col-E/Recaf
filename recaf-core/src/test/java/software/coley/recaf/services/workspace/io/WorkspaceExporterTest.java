package software.coley.recaf.services.workspace.io;

import com.google.common.primitives.Bytes;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.coley.recaf.info.JarFileInfo;
import software.coley.recaf.services.text.TextFormatConfig;
import software.coley.recaf.test.TestClassUtils;
import software.coley.recaf.test.dummy.HelloWorld;
import software.coley.recaf.util.io.ByteSources;
import software.coley.recaf.util.io.ZipCreationUtils;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
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
				new BasicInfoImporter(new InfoImporterConfig(), new TextFormatConfig(), new BasicClassPatcher()),
				new ResourceImporterConfig()
		);
	}

	@Test
	void testFileExportDoesNotTamperResourceModel() throws IOException {
		testExportDoesNotTamperResourceModel(WorkspaceOutputType.FILE);
	}

	@Test
	void testDirectoryExportDoesNotTamperResourceModel() throws IOException {
		testExportDoesNotTamperResourceModel(WorkspaceOutputType.DIRECTORY);
	}

	private static void testExportDoesNotTamperResourceModel(WorkspaceOutputType outputType) throws IOException {
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
		byte[] inputZipBytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/samples/name-prefix-suffix.jar"));
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
		byte[] inputZipBytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/samples/name-difference.zip"));
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

	@Test
	void testArbitraryHeaderDataIsKeptAfterExport() throws IOException {
		// Read a regular ZIP file and then pre-pend a lot of junk data to the front of it
		Random random = new Random(2410L);
		byte[] zipBytes = Files.readAllBytes(Paths.get("src/testFixtures/resources/samples/name-difference.zip"));
		byte[] junkBytes = new byte[(int) Math.pow(2, 16)];
		random.nextBytes(junkBytes);
		byte[] concatJunkThenZip = Bytes.concat(junkBytes, zipBytes);

		// Import the data with the junk in the front
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(concatJunkThenZip));
		BasicWorkspace workspace = new BasicWorkspace(resource);

		// Export it, and the junk should still be in the front, and the zip should still be in the back
		ByteArrayWorkspaceExportConsumer bytesExport = new ByteArrayWorkspaceExportConsumer();
		new WorkspaceExportOptions(WorkspaceOutputType.FILE, bytesExport).create().export(workspace);
		byte[] output = bytesExport.getOutput();

		// Verify the junk is still in the front of the output.
		assertNotNull(output, "Failed to export workspace to archive");
		assertEquals(0, Bytes.indexOf(output, junkBytes), "Failed to re-append prefix junk data to archive");

		// Verify the ZIP contents appear after the junk
		byte[] zipBytesHead = Arrays.copyOf(zipBytes, 0x30);
		byte[] outBytesHead = Arrays.copyOfRange(output, junkBytes.length, junkBytes.length + 0x30);
		for (int i = 0; i < 4; i++) {
			assertEquals(zipBytesHead[i], outBytesHead[i], "Mismatch where normal ZIP is supposed to be appended");
		}
		String zipHeadName = new String(zipBytesHead, StandardCharsets.ISO_8859_1).substring(30, 47);
		String outHeadName = new String(outBytesHead, StandardCharsets.ISO_8859_1).substring(30, 47);
		assertEquals("SomethingElse.bin", outHeadName, "Mismatch in outputs expected first ZIP entry name");
		assertEquals(zipHeadName, outHeadName, "Mismatch where normal ZIP entry for 'SomethingElse.bin' is supposed to be");
	}

	@Test
	void testNonArchiveExportedAsIsAndNotBundledInAZipContainer() throws IOException {
		// Make a basic [0, 1, 2, 3... 100]
		byte[] bytes = new byte[100];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte) i;

		// Import the data.
		WorkspaceResource resource = importer.importResource(ByteSources.wrap(bytes));
		BasicWorkspace workspace = new BasicWorkspace(resource);

		// Export it as a file.
		ByteArrayWorkspaceExportConsumer bytesExport = new ByteArrayWorkspaceExportConsumer();
		new WorkspaceExportOptions(WorkspaceOutputType.FILE, bytesExport).create().export(workspace);
		byte[] output = bytesExport.getOutput();

		// Verify the contents are the exact same. Because it's not an archive we should not be
		// exporting it back bundled inside an archive.
		assertArrayEquals(bytes, output, "Expected input and output to be exact match");
	}

	@Test
	void testAutomaticDirectoryExportMirrorsWorkspace(@TempDir Path root) throws IOException {
		Path output = root.resolve("out");

		BasicWorkspace workspace = zipSourcedWorkspace();
		try (PathWorkspaceExportConsumer consumer = new PathWorkspaceExportConsumer(output, PathWorkspaceExportConsumer.Mode.MIRROR_DIRECTORY)) {
			new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY, consumer).create().export(workspace);
		}

		// Current workspace content is present, including generated embedded-resource output.
		assertEquals("hello world", Files.readString(output.resolve("hello.txt")), "Missing file content in mirrored output");
		assertTrue(Files.exists(output.resolve(HelloWorld.class.getName().replace(".", "/") + ".class")), "Missing class file in mirrored output");
		assertTrue(Files.size(output.resolve("test.zip")) > 0, "Missing embedded resource output in mirrored output");

		// Cleanup of the staging directory is done by the exporter, so it should be gone now.
		try (var children = Files.list(root)) {
			List<String> rootDirContents = children.map(path -> path.getFileName().toString()).sorted().toList();
			assertEquals(List.of("out"), rootDirContents, "Staging or backup directory was left beside the mirrored output");
		}
	}

	@Test
	void testManualDirectoryExportPreservesUnrelatedEntries(@TempDir Path root) throws IOException {
		BasicWorkspace workspace = zipSourcedWorkspace();

		// Put some junk in the output directory that should not be deleted by the export process.
		Path output = root.resolve("out");
		Files.createDirectories(output.resolve("nested"));
		Files.writeString(output.resolve("stale.txt"), "stale");
		Files.writeString(output.resolve("nested/old.txt"), "old");

		// Unlike directory mirror mode, manual directory export does not stage the output and instead writes directly to the output.
		// So any existing files aren't moved to a backup location then deleted.
		new WorkspaceExportOptions(WorkspaceOutputType.DIRECTORY,
				new PathWorkspaceExportConsumer(output)).create().export(workspace);

		assertEquals("stale", Files.readString(output.resolve("stale.txt")), "Manual export deleted an unrelated file");
		assertEquals("old", Files.readString(output.resolve("nested/old.txt")), "Manual export deleted an unrelated nested file");
		assertEquals("hello world", Files.readString(output.resolve("hello.txt")), "Missing file content in manual output");
	}

	@Test
	void testAutomaticFileExportHandlesStagedOutput(@TempDir Path root) throws IOException {
		// Make a workspace with some arbitrary bytes to export.
		byte[] bytes = new byte[100];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = (byte) i;
		BasicWorkspace workspace = new BasicWorkspace(importer.importResource(ByteSources.wrap(bytes)));

		// Put some content in the target export path.
		Path output = root.resolve("out.dat");
		Files.writeString(output, "previous");

		// Export the workspace to a staged file, then move it into place.
		try (PathWorkspaceExportConsumer consumer = new PathWorkspaceExportConsumer(output, PathWorkspaceExportConsumer.Mode.ATOMIC_FILE)) {
			new WorkspaceExportOptions(WorkspaceOutputType.FILE, consumer).create().export(workspace);
		}

		// The staged file should have been moved into place, replacing the previous content.
		// Any staging files should have been cleaned up.
		assertArrayEquals(bytes, Files.readAllBytes(output), "Staged file output was not installed");
		try (var children = Files.list(root)) {
			List<String> rootDirContents = children.map(path -> path.getFileName().toString()).sorted().toList();
			assertEquals(List.of("out.dat"), rootDirContents, "Staging file was left beside the output");
		}
	}

	@Nonnull
	private static BasicWorkspace zipSourcedWorkspace() throws IOException {
		String helloWorldPath = HelloWorld.class.getName().replace(".", "/");
		byte[] zipBytes = ZipCreationUtils.builder()
				.add(helloWorldPath + ".class", TestClassUtils.fromRuntimeClass(HelloWorld.class).getBytecode())
				.add("hello.txt", "hello world".getBytes(StandardCharsets.UTF_8))
				.add("test.zip", ZipCreationUtils.createSingleEntryZip("inside.txt", new byte[0]))
				.bytes();
		return new BasicWorkspace(importer.importResource(ByteSources.wrap(zipBytes)));
	}
}