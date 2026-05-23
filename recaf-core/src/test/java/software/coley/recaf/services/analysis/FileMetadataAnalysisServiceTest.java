package software.coley.recaf.services.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.StubFileInfo;
import software.coley.recaf.services.analysis.metadata.FileHashResult;
import software.coley.recaf.services.analysis.metadata.FileMetadataAnalysisService;
import software.coley.recaf.services.analysis.metadata.HashAlgorithm;
import software.coley.recaf.services.analysis.metadata.JarSigningReport;
import software.coley.recaf.test.TestBase;
import software.coley.recaf.workspace.model.BasicWorkspace;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResourceBuilder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static software.coley.recaf.test.TestClassUtils.createFile;
import static software.coley.recaf.test.TestClassUtils.fromFiles;

/**
 * Tests for {@link FileMetadataAnalysisService}.
 */
class FileMetadataAnalysisServiceTest extends TestBase {
	private static FileMetadataAnalysisService service;

	@BeforeAll
	static void setupService() {
		service = recaf.get(FileMetadataAnalysisService.class);
	}

	@Test
	void hashesPrimaryAndDirectEmbeddedResourcesOnly() {
		// Only the primary and directly embedded resources should be hashed.
		WorkspaceFileResource deepEmbedded = new WorkspaceFileResourceBuilder()
				.withFileInfo(createFile("deep.bin", "deep".getBytes(StandardCharsets.UTF_8)))
				.build();
		WorkspaceFileResource directEmbedded = new WorkspaceFileResourceBuilder()
				.withFileInfo(createFile("nested.bin", "nested".getBytes(StandardCharsets.UTF_8)))
				.withEmbeddedResources(Map.of("deep.bin", deepEmbedded))
				.build();
		WorkspaceFileResource primary = new WorkspaceFileResourceBuilder()
				.withFileInfo(createFile("root.bin", "root".getBytes(StandardCharsets.UTF_8)))
				.withEmbeddedResources(Map.of("nested.bin", directEmbedded))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// We should get hashes for the primary and the directly embedded resource, but not the deeply embedded one.
		List<FileHashResult> results = service.computeHashes(workspace, primary);
		assertEquals(2, results.size());
		Set<String> hashedFiles = results.stream()
				.map(result -> result.filePath().getValue().getName())
				.collect(Collectors.toSet());
		String rootMd5 = results.stream()
				.filter(result -> result.filePath().getValue().getName().equals("root.bin"))
				.findFirst()
				.orElseThrow()
				.hashes()
				.get(HashAlgorithm.MD5);
		assertEquals(Set.of("root.bin", "nested.bin"), hashedFiles);
		assertEquals("63a9f0ea7bb98050796b649e85481845", rootMd5);
	}

	@Test
	void parsesJarSigningCertificates() {
		// Test that we can parse certificates from a signed JAR. We won't verify the signatures, just that we can extract the certificate information.
		FileInfo manifest = new StubFileInfo("META-INF/MANIFEST.MF")
				.withText("""
						Name: A.class
						SHA-256-Digest: abc
						
						Name: B.class
						SHA1-Digest: def
						""");
		FileInfo sf = createFile("META-INF/TEST.SF", "signature".getBytes(StandardCharsets.UTF_8));
		FileInfo rsa = createFile("META-INF/TEST.RSA", AnalysisTestUtils.certificatePemBytes());
		WorkspaceFileResource primary = new WorkspaceFileResourceBuilder()
				.withFileInfo(new StubFileInfo("sample.jar"))
				.withFileBundle(fromFiles(manifest, sf, rsa))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// The report should contain the signature file paths and certificate results, with no parse errors.
		JarSigningReport report = service.analyzeJarSigning(workspace, primary);
		assertNotNull(report);
		assertEquals(2, report.signatureFilePaths().size());
		assertEquals(1, report.certificateResults().size());
		assertNull(report.certificateResults().getFirst().parseError());
		assertFalse(report.certificateResults().getFirst().certificates().isEmpty());
	}

	@Test
	void recordsCertificateParseFailure() {
		// If the certificate file is malformed, we should record a parse error in the report rather than throwing an exception.
		FileInfo manifest = new StubFileInfo("META-INF/MANIFEST.MF")
				.withText("""
						Name: A.class
						SHA-256-Digest: abc
						""");
		FileInfo rsa = createFile("META-INF/TEST.RSA", new byte[]{1, 2, 3, 4});
		WorkspaceFileResource primary = new WorkspaceFileResourceBuilder()
				.withFileInfo(new StubFileInfo("sample.jar"))
				.withFileBundle(fromFiles(manifest, rsa))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// The report should indicate a parse error for the certificate file, but still be generated.
		JarSigningReport report = service.analyzeJarSigning(workspace, primary);
		assertNotNull(report);
		assertEquals("Error parsing certificate: " + rsa.getName(), report.certificateResults().getFirst().parseError());
	}

	@Test
	void skipsUnsignedJar() {
		// If the JAR doesn't contain signature files, there is nothing to report.
		FileInfo manifest = new StubFileInfo("META-INF/MANIFEST.MF").withText("Manifest-Version: 1.0\n");
		WorkspaceFileResource primary = new WorkspaceFileResourceBuilder()
				.withFileInfo(new StubFileInfo("sample.jar"))
				.withFileBundle(fromFiles(manifest))
				.build();
		Workspace workspace = new BasicWorkspace(primary);

		// No info to report on, so no report.
		assertNull(service.analyzeJarSigning(workspace, primary));
	}
}
