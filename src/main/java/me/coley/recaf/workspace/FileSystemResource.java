package me.coley.recaf.workspace;

import java.io.File;

/**
 * Importable resource from the file system.
 *
 * @author Matt
 */
public abstract class FileSystemResource extends JavaResource {
	private final File file;

	/**
	 * Constructs a file system resource.
	 *
	 * @param kind
	 * 		The kind of resource implementation.
	 * @param file
	 * 		The reference to the file resource.
	 */
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
		if (!file.isFile())
			throw new IllegalArgumentException("The file \"" + file.getName() + "\" does not exist!");
	}

	@Override
	public String toString() {
		return file.getName();
	}
}
