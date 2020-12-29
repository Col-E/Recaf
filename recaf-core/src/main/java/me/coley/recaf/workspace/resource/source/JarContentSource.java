package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Origin location information for jars.
 *
 * @author Matt Coley
 */
public class JarContentSource extends ArchiveFileContentSource {
	/**
	 * @param path
	 * 		Path to jar file.
	 */
	public JarContentSource(Path path) {
		super(SourceType.JAR, path);
	}
}
