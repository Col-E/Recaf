package me.coley.recaf.workspace.resource.source;

/**
 * The type of content source.
 *
 * @author Matt Coley
 */
public enum SourceType {
	/**
	 * Single class file.
	 */
	CLASS,
	/**
	 * Multiple files in an Java archive.
	 */
	JAR,
	/**
	 * Multiple files in an archive. For web applications.
	 */
	WAR,
	/**
	 * Multiple files in an archive.
	 */
	ZIP,
	/**
	 * Multiple files in a directory.
	 */
	DIRECTORY,
	/**
	 * Multiple files in a maven artifact.
	 */
	MAVEN,
	/**
	 * Content hosted online or locally, should map to a direct file source type.
	 */
	URL,
	/**
	 * Current agent instrumentation.
	 */
	INSTRUMENTATION,
	/**
	 * Dummy resource.
	 */
	EMPTY
}
