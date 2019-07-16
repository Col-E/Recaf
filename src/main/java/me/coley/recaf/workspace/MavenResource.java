package me.coley.recaf.workspace;


import com.jcabi.aether.Aether;
import org.pmw.tinylog.Logger;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Importable maven resource.
 *
 * @author Matt
 */
public class MavenResource extends JavaResource {
	private final static String CENTRAL_URL = "http://repo1.maven.org/maven2/";
	private final static String CENTRAL_ID = "maven-central";
	private final static String CENTRAL_TYPE = "default";
	/**
	 * Maven artifact identifier.
	 */
	private final String groupId, artifactId, version;
	/**
	 * Backing jar resource pointing to the local maven artifact.
	 */
	private JarResource backing;

	public MavenResource(String groupId, String artifactId, String version) {
		super(ResourceKind.MAVEN);
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		readFromCentral();
	}

	/**
	 * @return Combined group, artifact, version identifiers.
	 */
	public String getCoords() {
		return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	private void readFromCentral() {
		// System scope makes dependency resolving only give the actual jar, none of the compile
		// dependencies of our given dependency will be returned below.
		String scope = JavaScopes.SYSTEM;
		// Setup artifact
		String coords = getCoords();
		DefaultArtifact artifact = new DefaultArtifact(coords);
		// Setup repositories
		RemoteRepository central = new RemoteRepository(CENTRAL_ID, CENTRAL_TYPE, CENTRAL_URL);
		Collection<RemoteRepository> remotes = Arrays.asList(central);
		try {
			// Resolve
			Aether aether = new Aether(remotes, getMavenHome());
			Collection<Artifact> dependencies = aether.resolve(artifact, scope);
			if(dependencies.size() > 1) {
				throw new IllegalArgumentException("Resolving on system scope should only return one value.");
			}
			// Maven will downloaded the dependency and we can reference it locally.
			Artifact resolved = dependencies.stream().findFirst().get();
			backing = new JarResource(resolved.getFile());
		} catch(DependencyResolutionException ex) {
			Logger.error(ex, "Failed to resolve maven dependency \"{}\"", coords);
			throw new IllegalArgumentException("Failed to resolve maven dependency \"" + coords +"\"", ex);
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

	private static File getMavenHome() {
		// Check if set by environment variables.
		// https://stackoverflow.com/questions/26609922/maven-home-mvn-home-or-m2-home
		String maven = System.getProperty("maven.home");
		if(maven != null && !maven.isEmpty()) {
			return new File(maven);
		}
		// Should be here
		File m2 =  new File(System.getProperty("user.home"), ".m2");
		return new File(m2, "repository");
	}

	@Override
	public String toString() {
		return getCoords();
	}
}