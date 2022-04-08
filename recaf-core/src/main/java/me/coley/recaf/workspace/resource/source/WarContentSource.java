package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Origin location information for wars.
 *
 * @author Matt Coley
 */
public class WarContentSource extends ArchiveFileContentSource {
	private static final String WAR_CLASS_PREFIX = "WEB-INF/classes/";

	/**
	 * @param path
	 * 		Path to war file.
	 */
	public WarContentSource(Path path) {
		super(SourceType.WAR, path);
	}

	@Override
	protected String filterInputClassName(String className) {
		if (className.startsWith(WAR_CLASS_PREFIX)) {
			return className.substring(WAR_CLASS_PREFIX.length());
		}
		return super.filterInputClassName(className);
	}
}
