package software.coley.recaf.services.analysis.metadata;

import com.google.common.hash.Hashing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.coley.recaf.info.FileInfo;
import software.coley.recaf.info.TextFileInfo;
import software.coley.recaf.path.FilePathNode;
import software.coley.recaf.path.PathNodes;
import software.coley.recaf.services.Service;
import software.coley.recaf.workspace.model.Workspace;
import software.coley.recaf.workspace.model.bundle.FileBundle;
import software.coley.recaf.workspace.model.resource.WorkspaceFileResource;
import software.coley.recaf.workspace.model.resource.WorkspaceResource;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for file metadata analysis.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class FileMetadataAnalysisService implements Service {
	public static final String SERVICE_ID = "file-metadata-analysis";
	private static final Pattern SIGNED_ENTRY_PATTERN = Pattern.compile("(?m)^[\\w-]+-Digest: .+$");
	private static final CertificateFactory CERTIFICATE_FACTORY;
	private final FileMetadataAnalysisConfig config;

	static {
		CertificateFactory factory;
		try {
			factory = CertificateFactory.getInstance("X.509");
		} catch (Throwable t) {
			factory = null;
		}
		CERTIFICATE_FACTORY = factory;
	}

	@Inject
	public FileMetadataAnalysisService(@Nonnull FileMetadataAnalysisConfig config) {
		this.config = config;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect.
	 *
	 * @return Hash results for the primary file resource when present, plus direct embedded file resources.
	 */
	@Nonnull
	public List<FileHashResult> computeHashes(@Nonnull Workspace workspace,
	                                          @Nonnull WorkspaceResource resource) {
		List<FileHashResult> results = new ArrayList<>();
		if (resource instanceof WorkspaceFileResource fileResource)
			results.add(hash(workspace, fileResource));
		for (WorkspaceFileResource embedded : resource.getEmbeddedResources().values())
			results.add(hash(workspace, embedded));
		return results;
	}

	/**
	 * @param workspace
	 * 		Containing workspace.
	 * @param resource
	 * 		Resource to inspect.
	 *
	 * @return Jar signing report, or {@code null} when the resource is not a signed jar file.
	 */
	@Nullable
	public JarSigningReport analyzeJarSigning(@Nonnull Workspace workspace,
	                                          @Nonnull WorkspaceResource resource) {
		FileBundle bundle = resource.getFileBundle();
		FileInfo manifest = bundle.get("META-INF/MANIFEST.MF");

		// Must have a manifest file.
		if (manifest == null || !manifest.isTextFile())
			return null;

		// Must list at least one file signed.
		TextFileInfo manifestText = manifest.asTextFile();
		String manifestContent = manifestText.getText();
		if (!manifestContent.contains("-Digest: "))
			return null;


		// Collect signature files.
		List<FilePathNode> signatureFilePaths = new ArrayList<>();
		List<JarCertificateResult> certificateResults = new ArrayList<>();
		for (FileInfo file : bundle) {
			String name = file.getName();
			if (!name.matches("META-INF/[\\w-]+\\.(?:SF|RSA|DSA)"))
				continue;

			// Collect signature file paths.
			FilePathNode filePath = PathNodes.filePath(workspace, resource, bundle, file);
			signatureFilePaths.add(filePath);
			if (name.endsWith(".SF"))
				continue;

			// Parse certificate file and collect results.
			List<Certificate> certificates = List.of();
			String parseError = null;
			if (CERTIFICATE_FACTORY == null) {
				parseError = "Certificate factory unavailable";
			} else {
				try {
					certificates = List.copyOf(CERTIFICATE_FACTORY.generateCertificates(new ByteArrayInputStream(file.getRawContent())));
				} catch (CertificateException ex) {
					parseError = "Error parsing certificate: " + file.getName();
				}
			}
			certificateResults.add(new JarCertificateResult(filePath, certificates, parseError));
		}

		FilePathNode manifestPath = PathNodes.filePath(workspace, resource, bundle, manifest);
		return new JarSigningReport(manifestPath, List.copyOf(signatureFilePaths), List.copyOf(certificateResults));
	}

	@Nonnull
	@Override
	public String getServiceId() {
		return SERVICE_ID;
	}

	@Nonnull
	@Override
	public FileMetadataAnalysisConfig getServiceConfig() {
		return config;
	}

	@SuppressWarnings("deprecation") // Don't care that MD5/SHA1 are deprecated, people still use them frequently.
	private static FileHashResult hash(@Nonnull Workspace workspace, @Nonnull WorkspaceFileResource fileResource) {
		FileInfo fileInfo = fileResource.getFileInfo();
		byte[] content = fileInfo.getRawContent();
		EnumMap<HashAlgorithm, String> hashes = new EnumMap<>(HashAlgorithm.class);
		hashes.put(HashAlgorithm.MD5, Hashing.md5().hashBytes(content).toString());
		hashes.put(HashAlgorithm.SHA1, Hashing.sha1().hashBytes(content).toString());
		hashes.put(HashAlgorithm.SHA256, Hashing.sha256().hashBytes(content).toString());
		hashes.put(HashAlgorithm.SHA512, Hashing.sha512().hashBytes(content).toString());
		FilePathNode path = PathNodes.filePath(workspace, fileResource, fileResource.getFileBundle(), fileInfo);
		return new FileHashResult(path, new EnumMap<>(hashes));
	}
}
