package me.coley.recaf.workspace;

import java.io.File;
import java.io.FileNotFoundException;

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
		verify();
	}

	/**
	 * @return The file imported from.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Verify the file exists.
	 */
	protected void verify() {
		if (!file.exists())
			throw new IllegalArgumentException("The file \"" + file.getName() + "\" does not exist!");
	}

	@Override
	public String toString() {
		return file.getName();
	}
}
