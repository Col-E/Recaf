package me.coley.recaf.workspace.resource.source;

import me.coley.recaf.workspace.resource.Resource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.io.File.separatorChar;
import static java.lang.String.format;

/**
 * Origin location information for maven artifacts.
 *
 * @author Matt Coley
 */
public class MavenContentSource extends ContentSource {
	private static final String CENTRAL_REPO = "https://repo1.maven.org/maven2";
	private static final String NO_SUFFIX = "";
	private static final Logger logger = LoggerFactory.getLogger(MavenContentSource.class);
	private static final int CONNECTION_TIMEOUT = 2000;
	private static final int READ_TIMEOUT = 3000;
	private final String groupId;
	private final String artifactId;
	private final String version;

	/**
	 * @param groupId
	 * 		Artifact's group.
	 * @param artifactId
	 * 		Artifact's identifier.
	 * @param version
	 * 		Artifact's version.
	 */
	public MavenContentSource(String groupId, String artifactId, String version) {
		super(SourceType.MAVEN);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	@Override
	public void writeTo(Resource resource, Path path) throws IOException {
		// Redirect to Jar content's write implementation
		new JarContentSource(null).writeTo(resource, path);
	}

	@Override
	protected void onRead(Resource resource) throws IOException {
		Path local = getLocalArtifactPath(NO_SUFFIX);
		// Check if we need to download the artifact
		if (!Files.isReadable(local)) {
			logger.info("Downloading maven artifact \"{}:{}:{}\" to local repo: {}",
					groupId, artifactId, version, getRepositoryPath());
			Files.createDirectories(local.getParent());
			String remoteArtifact = getRemoteArtifactUrl(NO_SUFFIX);
			// Download it
			FileUtils.copyURLToFile(new URL(remoteArtifact), local.toFile(), CONNECTION_TIMEOUT, READ_TIMEOUT);
		}
		// Load from local file
		logger.info("Reading from maven artifact jar: {}", local);
		new JarContentSource(local).readInto(resource);
	}

	/**
	 * @param suffix
	 * 		Artifact name suffix.
	 * 		Used to specify other maven jars such as <i>"-source"</i> and <i>"-javadoc"</i>
	 *
	 * @return Url pointing to the remote artifact.
	 */
	private String getRemoteArtifactUrl(String suffix) {
		String remoteDirectory = format("%s/%s/%s/%s", CENTRAL_REPO, groupId.replace('.', '/'), artifactId, version);
		String artifactName = format("%s-%s%s.jar", artifactId, version, suffix);
		return remoteDirectory + "/" + artifactName;
	}

	/**
	 * @param suffix
	 * 		Artifact name suffix.
	 * 		Used to specify other maven jars such as <i>"-source"</i> and <i>"-javadoc"</i>
	 *
	 * @return Path pointing to the local artifact.
	 */
	private Path getLocalArtifactPath(String suffix) {
		String relativeDirectory = format("%s/%s/%s", groupId.replace('.', separatorChar), artifactId, version);
		String relativeArtifact = format("%s/%s-%s%s.jar", relativeDirectory, artifactId, version, suffix);
		return getRepositoryPath().resolve(relativeArtifact);
	}

	/**
	 * @return Local directory containing downloaded maven artifacts.
	 */
	private static Path getRepositoryPath() {
		return Paths.get(FileUtils.getUserDirectoryPath(), ".m2", "repository");
	}
}
