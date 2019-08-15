package me.coley.recaf.workspace;

import me.coley.recaf.util.MavenUtil;
import me.coley.recaf.util.NetworkUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Importable maven resource.
 *
 * @author Matt
 */
public class MavenResource extends DeferringResource {
	// Artifact coordinate identifiers
	private final String groupId;
	private final String artifactId;
	private final String version;

	/**
	 * Constructs a maven artifact resource.
	 *
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 */
	public MavenResource(String groupId, String artifactId, String version) {
		super(ResourceKind.MAVEN);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		findArtifact();
	}

	/**
	 * @return Combined group, artifact, version identifiers.
	 */
	public String getCoords() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	/**
	 * @return Maven artifact group.
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return Maven artifact identifier.
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * @return Maven artifact version.
	 */
	public String getVersion() {
		return version;
	}

	private void findArtifact() throws IllegalArgumentException {
		// Check local maven repo
		File localArtifact = MavenUtil.getLocalArtifactUrl(groupId, artifactId, version);
		if (localArtifact.isFile()) {
			setBacking(new JarResource(localArtifact));
			return;
		}
		// Verify artifact is on central
		MavenUtil.verifyArtifactOnCentral(groupId, artifactId, version);
		try {
			// Copy artifact to local maven repo
			URL url = MavenUtil.getArtifactUrl(groupId, artifactId, version);
			FileUtils.copyURLToFile(url, localArtifact);
			setBacking(new JarResource(localArtifact));
		} catch(IOException ex) {
			// Something went wrong fetching the file
			throw new IllegalArgumentException(ex);
		}
		// TODO: Not here, but allow auto-resolving ALL dependencies not just the specified one
	}

	/**
	 * @return {@code true} if sources were found and loaded.
	 *
	 * @throws IOException
	 * 		When the sources could not be downloaded / found.
	 */
	public boolean fetchSources() throws IOException {
		// Check local maven repo
		File localArtifact = MavenUtil.getLocalArtifactUrl(groupId, artifactId, version, "-sources");
		if (localArtifact.isFile())
			return setClassSources(localArtifact);
		try {
			// Find and verify the sources jar url
			URL sourceUrl = MavenUtil.getArtifactUrl(groupId, artifactId, version, "-sources");
			try {
				NetworkUtil.verifyUrlContent(sourceUrl);
			} catch(IllegalArgumentException ex) {
				throw new IOException(ex);
			}
			// Download
			FileUtils.copyURLToFile(sourceUrl, localArtifact);
			return setClassSources(localArtifact);
		} catch(MalformedURLException ex) {
			// This should NOT ever occur since the url generated should already be pre-verified.
			throw new IllegalArgumentException(ex);
		}
	}

	/**
	 * @return {@code true} if javadocs were found and loaded.
	 *
	 * @throws IOException
	 * 		When the javadocs could not be downloaded / found.
	 */
	public boolean fetchJavadoc() throws IOException {
		// Check local maven repo
		File localArtifact = MavenUtil.getLocalArtifactUrl(groupId, artifactId, version, "-javadoc");
		if (localArtifact.isFile())
			return setClassDocs(localArtifact);
		try {
			// Find and verify the javadocs jar url
			URL sourceUrl = MavenUtil.getArtifactUrl(groupId, artifactId, version, "-javadoc");
			try {
				NetworkUtil.verifyUrlContent(sourceUrl);
			} catch(IllegalArgumentException ex) {
				throw new IOException(ex);
			}
			// Download
			FileUtils.copyURLToFile(sourceUrl, localArtifact);
			return setClassDocs(localArtifact);
		} catch(MalformedURLException ex) {
			// This should NOT ever occur since the url generated should already be pre-verified.
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	public String toString() {
		return getCoords();
	}
}