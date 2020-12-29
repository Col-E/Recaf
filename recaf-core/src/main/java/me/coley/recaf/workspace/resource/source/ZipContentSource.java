package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Origin location information for zips.
 *
 * @author Matt Coley
 */
public class ZipContentSource extends ArchiveFileContentSource {
	/**
	 * @param path
	 * 		Path to zip file.
	 */
	public ZipContentSource(Path path) {
		super(SourceType.ZIP, path);
	}
}
