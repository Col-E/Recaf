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
	 * Create a FileSystemResource from the given file.
	 *
	 * @param file
	 * 		File to load as a resource.
	 *
	 * @return File resource.
	 *
	 * @throws IOException
	 * 		When the file cannot be read.
	 * @throws UnsupportedOperationException
	 * 		When the file extension is not supported.
	 */
	public static FileSystemResource of(File file) throws IOException {
		if (file.isDirectory())
			return new DirectoryResource(file);
		String name = file.getName();
		String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		switch(ext) {
			case "class":
				return new ClassResource(file);
			case "jar":
				return new JarResource(file);
			case "war":
				return new WarResource(file);
			default:
				throw new UnsupportedOperationException("File type '" + ext + "' is not " +
						"allowed for libraries");
		}
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
