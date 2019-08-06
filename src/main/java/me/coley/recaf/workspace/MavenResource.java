package me.coley.recaf.workspace;

import me.coley.recaf.util.MavenUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Importable maven resource.
 *
 * @author Matt
 */
public class MavenResource extends JavaResource {
	// Artifact coordinate identifiers
	private final String groupId;
	private final String artifactId;
	private final String version;
	/**
	 * Backing url resource pointing to the online maven artifact.
	 */
	private JavaResource backing;

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
		// TODO: Also check local maven directory
		// TODO: Not here, but allow auto-resolving ALL dependencies not just the specified one
		MavenUtil.verifyArtifactOnCentral(groupId, artifactId, version);
		try {
			URL url = MavenUtil.getArtifactUrl(groupId, artifactId, version);
			backing = new UrlResource(url);
		} catch(MalformedURLException ex) {
			// This should NOT ever occur since the url generated should already be pre-verified.
			throw new IllegalArgumentException(ex);
		}
	}

	@Override
	protected Map<String, byte[]> loadClasses() throws IOException {
		return backing.loadClasses();
	}

	@Override
	protected Map<String, byte[]> loadResources() throws IOException {
		return backing.loadResources();
	}

	@Override
	public String toString() {
		return getCoords();
	}
}