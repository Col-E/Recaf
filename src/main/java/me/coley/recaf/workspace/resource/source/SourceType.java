package me.coley.recaf.workspace.resource.source;

/**
 * The type of content source.
 *
 * @author Matt Coley
 */
public enum SourceType {
	/**
	 * Resource of a single class file.
	 */
	CLASS,
	/**
	 * Resource of multiple files in an Java archive.
	 */
	JAR,
	/**
	 * Resource of multiple files in an archive. For web applications.
	 */
	WAR,
	/**
	 * Resource of multiple files in an archive.
	 */
	ZIP,
	/**
	 * Resource of multiple files in a directory.
	 */
	DIRECTORY,
	/**
	 * Resource of multiple files in a maven artifact.
	 */
	MAVEN,
	/**
	 * Resource hosted online, should map to either a <b><i>CLASS</i></b> or <b><i>JAR</i></b>.
	 */
	URL,
	/**
	 * Resource of from the current agent instrumentation.
	 */
	INSTRUMENTATION,
	/**
	 * Dummy resource.
	 */
	EMPTY
}
