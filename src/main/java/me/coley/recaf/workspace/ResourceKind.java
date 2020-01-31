package me.coley.recaf.workspace;

/**
 * The kind of a Java resource input.
 *
 * <ul>
 *     <li><b>Class</b> - Resource of a single file</li>
 *     <li><b>Jar</b> - Resource of multiple files in an archive</li>
 *     <li><b>War</b> - Resource of multiple files in an archive</li>
 *     <li><b>Directory</b> - Resource of multiple files in a directory</li>
 *     <li><b>Maven</b> - Resource of multiple files in a maven artifact</li>
 *     <li><b>URL</b> - Resource hosted online, should map to either a {@link #CLASS} or {@link #JAR}</li>
 *     <li><b>Instrumentation</b> - Resource of from the current agent instrumentation</li>
 * </ul>
 *
 * @author Matt
 */
public enum ResourceKind {
	/**
	 * Resource of a single file.
	 */
	CLASS,
	/**
	 * Resource of multiple files in an archive.
	 */
	JAR,
	/**
	 * Resource of multiple files in an archive. For web applications.
	 */
	WAR,
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
	 * Resource for a debugger.
	 */
	DEBUGGER,
	/**
	 * Dummy resource.
	 */
	EMPTY
}
