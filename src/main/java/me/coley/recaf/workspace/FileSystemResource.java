package me.coley.recaf.workspace;

import java.io.File;

/**
 * Importable resource from the file system.
 *
 * @author Matt
 */
public abstract class FileSystemResource extends JavaResource {
	private final File file;

	public FileSystemResource(ResourceKind kind, File file) {
		super(kind);
		this.file = file;
	}

	/**
	 * @return The file imported from.
	 */
	public File getFile() {
		return file;
	}
}
