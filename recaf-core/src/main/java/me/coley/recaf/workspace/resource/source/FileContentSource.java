package me.coley.recaf.workspace.resource.source;

import java.nio.file.Path;

/**
 * Origin location information of files.
 *
 * @author Matt Coley
 */
public abstract class FileContentSource extends ContentSource {
	private final Path path;

	protected FileContentSource(SourceType type, Path path) {
		super(type);
		this.path = path;
	}

	/**
	 * @return Path to file source.
	 */
	public Path getPath() {
		return path;
	}

	@Override
	public String toString() {
		return path.getFileName().toString();
	}
}
