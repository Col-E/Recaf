package me.coley.recaf.workspace;

import java.io.File;
import java.io.IOException;

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
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	public FileSystemResource(ResourceKind kind, File file) throws IOException {
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
	 *
	 * @throws IOException
	 * 		When the file does not exist.
	 */
	protected void verify() throws IOException {
		if (!file.isFile())
			throw new IOException("The file \"" + file.getName() + "\" does not exist!");
	}

	@Override
	public String toString() {
		return file.getName();
	}
}
