package me.coley.recaf.util;

import me.coley.recaf.workspace.UrlResource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utilities for finding maven artifacts. The alternative is a 16 megabyte dependency bloat...
 *
 * @author Matt
 */
public class MavenUtil {
	private static final String CENTRAL_URL = "http://repo1.maven.org/maven2/";

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
	 * @throws IllegalArgumentException
	 * 		Thrown if at any point a faulty URL is generated or if there is no data to read <i>(No
	 * 		such group/artifact/version existing on central)</i>
	 */
	public static void verifyArtifactOnCentral(String groupId, String artifactId, String version) {
		String groupUrl;
		String artifactUrl;
		String versionUrl;
		String jarUrl;
		// Test connection to maven central
		try {
			NetworkUtil.verifyUrlContent(CENTRAL_URL);
		} catch(MalformedURLException ex) {
			throw new IllegalArgumentException("Central URL is malformed, this should NOT happen", ex);
		} catch(IllegalArgumentException ex) {
			throw new IllegalArgumentException("Maven central is down or has migrated to a new URL: " +
					CENTRAL_URL, ex);
		}
		// Test connection to the group
		try {
			groupUrl = CENTRAL_URL + groupId.replace(".", "/") + "/";
			NetworkUtil.verifyUrlContent(groupUrl);
		} catch(MalformedURLException ex) {
			throw new IllegalArgumentException("Invalid group, generates invalid URL: " + groupId, ex);
		} catch(IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid group, does not exist on central: " + groupId, ex);
		}
		// Test connection to the artifact
		try {
			artifactUrl = groupUrl + artifactId + "/";
			NetworkUtil.verifyUrlContent(artifactUrl);
		} catch(MalformedURLException ex) {
			throw new IllegalArgumentException("Invalid artifact, generates invalid URL: " +
					groupId + ":" + artifactId, ex);
		} catch(IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid artifact, does not exist on central: " +
					groupId + ":" + artifactId, ex);
		}
		// Test connection to the version
		try {
			versionUrl = artifactUrl + version + "/";
			NetworkUtil.verifyUrlContent(versionUrl);
		} catch(MalformedURLException ex) {
			throw new IllegalArgumentException("Invalid version, generates invalid URL: " +
					groupId + ":" + artifactId + ":" + version, ex);
		} catch(IllegalArgumentException ex) {
			throw new IllegalArgumentException("Invalid version, does not exist on central: " +
					groupId + ":" + artifactId + ":" + version, ex);
		}
		// Test connection to the full url
		try {
			jarUrl = versionUrl + artifactId + "-" + version + ".jar";
			NetworkUtil.verifyUrlContent(jarUrl);
			// TODO: In some cases there are OS-specific jars: https://repo1.maven.org/maven2/org/openjfx/javafx-controls/13-ea+10/
		} catch(MalformedURLException ex) {
			throw new IllegalArgumentException("Failed to generate maven jar url: "  +
					groupId + ":" + artifactId + ":" + version, ex);
		} catch(IllegalArgumentException ex) {
			throw new IllegalArgumentException("Jar does not match expected name on maven central: "  +
					groupId + ":" + artifactId + ":" + version, ex);
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
	public static URL getArtifactUrl(String groupId, String artifactId, String version) throws MalformedURLException{
		String url = CENTRAL_URL + groupId.replace(".", "/") + "/" + artifactId +
				"/" + version + "/" + artifactId + "-" + version + ".jar";
		return new URL(url);
	}

	/**
	 * @return Local directory containing downloaded maven artifacts.
	 */
	public static File getMavenHome() {
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
}
