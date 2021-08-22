package me.coley.recaf.util;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.io.File.separator;
import static java.io.File.separatorChar;

/**
 * Utilities for finding maven artifacts. The alternative is a 16 megabyte dependency bloat...
 *
 * @author Matt
 */
public class MavenUtil {
	private static final String CENTRAL_URL = "https://repo1.maven.org/maven2/";

	/**
	 * Verifies that the maven artifact can be located on maven central.
	 *
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 *
	 * @throws IOException
	 * 		Thrown if at any point a faulty URL is generated or if there is no data to read <i>(No
	 * 		such group/artifact/version existing on central)</i>
	 */
	public static void verifyArtifactOnCentral(String groupId, String artifactId, String version)
			throws IOException {
		String groupUrl;
		String artifactUrl;
		String versionUrl;
		String jarUrl;
		// Test connection to maven central
		try {
			NetworkUtil.verifyUrlContent(CENTRAL_URL);
		} catch(MalformedURLException ex) {
			throw new IOException("Central URL is malformed, this should NOT happen", ex);
		} catch(IOException ex) {
			throw new IOException("Maven central is down or has migrated to a new URL: " +
					CENTRAL_URL, ex);
		}
		// Test connection to the group
		try {
			groupUrl = CENTRAL_URL + groupId.replace(".", "/") + "/";
			NetworkUtil.verifyUrlContent(groupUrl);
		} catch(MalformedURLException ex) {
			throw new IOException("Invalid group, generates invalid URL: " + groupId, ex);
		} catch(IOException ex) {
			throw new IOException("Invalid group, does not exist on central: " + groupId, ex);
		}
		// Test connection to the artifact
		try {
			artifactUrl = groupUrl + artifactId + "/";
			NetworkUtil.verifyUrlContent(artifactUrl);
		} catch(MalformedURLException ex) {
			throw new IOException("Invalid artifact, generates invalid URL: " + groupId + ":" +
					artifactId, ex);
		} catch(IOException ex) {
			throw new IOException("Invalid artifact, does not exist on central: " + groupId + ":" +
					artifactId, ex);
		}
		// Test connection to the version
		try {
			versionUrl = artifactUrl + version + "/";
			NetworkUtil.verifyUrlContent(versionUrl);
		} catch(MalformedURLException ex) {
			throw new IOException("Invalid version, generates invalid URL: " + groupId + ":" +
					artifactId + ":" + version, ex);
		} catch(IOException ex) {
			throw new IOException("Invalid version, does not exist on central: " + groupId + ":" +
					artifactId + ":" + version, ex);
		}
		// Test connection to the full url
		try {
			jarUrl = versionUrl + artifactId + "-" + version + ".jar";
			NetworkUtil.verifyUrlContent(jarUrl);
			// TODO: In some cases there are OS-specific jars:
			//  https://repo1.maven.org/maven2/org/openjfx/javafx-controls/13-ea+10/
		} catch(MalformedURLException ex) {
			throw new IOException("Failed to generate maven jar url: " + groupId + ":" + artifactId + ":" +
					version, ex);
		} catch(IOException ex) {
			throw new IOException("Jar does not match expected name on maven central: " + groupId + ":" +
					artifactId + ":" + version, ex);
		}
	}

	/**
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 *
	 * @return URL pointing to the online maven central artifact.
	 *
	 * @throws MalformedURLException
	 * 		Thrown if any of the components result given result in a malformed generated URL.
	 */
	public static URL getArtifactUrl(String groupId, String artifactId, String version) throws MalformedURLException {
		return getArtifactUrl(groupId, artifactId, version, "");
	}

	/**
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 * @param suffix
	 * 		Url suffix.
	 * 		Used to specify other maven jars such as <i>"-source"</i> and <i>"-javadoc"</i>
	 *
	 * @return URL pointing to the online maven central artifact.
	 *
	 * @throws MalformedURLException
	 * 		Thrown if any of the components result given result in a malformed generated URL.
	 */
	public static URL getArtifactUrl(String groupId, String artifactId, String version, String suffix)
			throws MalformedURLException {
		String url = CENTRAL_URL + groupId.replace(".", "/") + "/" + artifactId +
				"/" + version + "/" + artifactId + "-" + version + suffix + ".jar";
		return new URL(url);
	}

	/**
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 *
	 * @return File pointing to the local artifact.
	 */
	public static Path getLocalArtifactUrl(String groupId, String artifactId, String version) {
		return getLocalArtifactUrl(groupId, artifactId, version, "");
	}

	/**
	 * @param groupId
	 * 		Maven artifact group.
	 * @param artifactId
	 * 		Maven artifact identifier.
	 * @param version
	 * 		Maven artifact version.
	 * @param suffix
	 * 		File name suffix.
	 * 		Used to specify other maven jars such as <i>"-source"</i> and <i>"-javadoc"</i>
	 *
	 * @return File pointing to the local artifact.
	 */
	public static Path getLocalArtifactUrl(String groupId, String artifactId, String version, String suffix) {
		String path = groupId.replace('.', separatorChar) + separator + artifactId +
				separator + version + separator + artifactId + "-" + version + suffix + ".jar";
		return getMavenHome().resolve(path);
	}

	/**
	 * @return Local directory containing downloaded maven artifacts.
	 */
	public static Path getMavenHome() {
		return Paths.get(System.getProperty("user.home"), ".m2", "repository");
	}
}
